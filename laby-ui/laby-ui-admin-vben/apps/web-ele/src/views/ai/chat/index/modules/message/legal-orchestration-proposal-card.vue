<script lang="ts" setup>
import type { LegalOrchestrationApi } from '#/api/legal/orchestration';

import { computed, onMounted, ref, watch } from 'vue';

import { useRouter } from 'vue-router';

import { DICT_TYPE } from '@vben/constants';
import { getDictLabel } from '@vben/hooks';

import { ElButton, ElCard, ElMessage, ElTag } from 'element-plus';

import {
  cancelOrchestrationProposal,
  executeOrchestrationProposal,
  getOrchestrationSession,
  listPendingOrchestrationProposals,
  resumeOrchestrationFromCheckpoint,
} from '#/api/legal/orchestration';

defineOptions({ name: 'LegalOrchestrationProposalCard' });

const props = defineProps<{
  conversationId?: number | null;
  refreshKey?: number;
}>();

const emit = defineEmits<{
  proposalExecuted: [payload: { action: string; proposalNo: string }];
}>();

const router = useRouter();
const loading = ref(false);
const actionLoading = ref<string | null>(null);
const proposals = ref<LegalOrchestrationApi.Proposal[]>([]);
const session = ref<LegalOrchestrationApi.Session | null>(null);
const resumeLoading = ref(false);
const autoResumedConversationId = ref<number | null>(null);

const ORCH_PHASE_ORDER = [
  'INIT',
  'FILE_REGISTERED',
  'CLASSIFY_PENDING',
  'CLASSIFY_CONFIRMED',
  'TYPE_PACKAGE_DRAFTING',
  'TYPE_PACKAGE_PENDING',
  'CREATE_PENDING',
  'CONTRACTS_CREATED',
  'TRACKING',
  'CLOSED',
] as const;

function orchPhaseIndex(phase?: string) {
  if (!phase) {
    return -1;
  }
  return ORCH_PHASE_ORDER.indexOf(phase as (typeof ORCH_PHASE_ORDER)[number]);
}

function parseCreatePolicy(
  item: LegalOrchestrationApi.Proposal,
): LegalOrchestrationApi.CreateContractsPolicyPayload | null {
  if (item.action !== 'CREATE_CONTRACTS_BATCH' || !item.payloadJson) {
    return null;
  }
  try {
    return JSON.parse(item.payloadJson) as LegalOrchestrationApi.CreateContractsPolicyPayload;
  } catch {
    return null;
  }
}

function partyRoleLabel(code?: string) {
  return code ? getDictLabel(DICT_TYPE.LEGAL_PARTY_ROLE, code) || code : '-';
}

function auditLevelLabel(code?: string) {
  return code ? getDictLabel(DICT_TYPE.LEGAL_AUDIT_LEVEL, code) || code : '-';
}

function modelLabel(policy: LegalOrchestrationApi.CreateContractsPolicyPayload | null) {
  if (session.value?.modelName) {
    return session.value.modelName;
  }
  if (policy?.modelId) {
    return `#${policy.modelId}`;
  }
  if (session.value?.modelId) {
    return `#${session.value.modelId}`;
  }
  return '-';
}

async function loadSession() {
  if (!props.conversationId) {
    session.value = null;
    return;
  }
  const data = await getOrchestrationSession(props.conversationId);
  session.value = data ?? null;
  await tryAutoResumeCheckpoint();
}

async function tryAutoResumeCheckpoint() {
  if (
    !props.conversationId
    || resumeLoading.value
    || autoResumedConversationId.value === props.conversationId
    || !shouldAutoResumeCheckpoint.value
  ) {
    return;
  }
  autoResumedConversationId.value = props.conversationId;
  resumeLoading.value = true;
  try {
    const checkpoint = await resumeOrchestrationFromCheckpoint(props.conversationId);
    if (!checkpoint) {
      return;
    }
    ElMessage.success(`检测到编排中断，已自动恢复至 ${checkpoint.phase}`);
    const data = await getOrchestrationSession(props.conversationId);
    session.value = data ?? null;
    await loadProposals();
  } catch {
    autoResumedConversationId.value = null;
  } finally {
    resumeLoading.value = false;
  }
}

const createdContracts = computed(() =>
  (session.value?.fileItems || []).filter((item) => item.contractId),
);

const enrichedProposals = computed(() =>
  proposals.value.map((item) => ({
    ...item,
    createPolicy: parseCreatePolicy(item),
  })),
);

const hasPanelContent = computed(
  () => proposals.value.length > 0 || createdContracts.value.length > 0,
);

const canResumeCheckpoint = computed(() => {
  if (!session.value?.checkpointSavedAt) {
    return false;
  }
  if (!session.value.checkpointPhase) {
    return true;
  }
  return session.value.checkpointPhase !== session.value.phase;
});

/** Checkpoint 阶段领先于当前阶段时，视为中断后可自动续跑 */
const shouldAutoResumeCheckpoint = computed(() => {
  if (!canResumeCheckpoint.value || !session.value?.checkpointPhase) {
    return false;
  }
  const checkpointIdx = orchPhaseIndex(session.value.checkpointPhase);
  const currentIdx = orchPhaseIndex(session.value.phase);
  if (checkpointIdx < 0 || currentIdx < 0) {
    return false;
  }
  return checkpointIdx > currentIdx;
});

function formatCheckpointTime(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

async function handleResumeCheckpoint() {
  if (!props.conversationId) {
    return;
  }
  resumeLoading.value = true;
  try {
    const checkpoint = await resumeOrchestrationFromCheckpoint(props.conversationId);
    if (!checkpoint) {
      ElMessage.warning('暂无可恢复的编排进度');
      return;
    }
    ElMessage.success(`已恢复至阶段 ${checkpoint.phase}`);
    await refreshAll();
  } finally {
    resumeLoading.value = false;
  }
}

async function loadProposals() {
  if (!props.conversationId) {
    proposals.value = [];
    return;
  }
  loading.value = true;
  try {
    proposals.value = await listPendingOrchestrationProposals(props.conversationId);
  } finally {
    loading.value = false;
  }
}

async function handleExecute(proposalNo: string) {
  const proposal = proposals.value.find((item) => item.proposalNo === proposalNo);
  actionLoading.value = proposalNo;
  try {
    await executeOrchestrationProposal(proposalNo);
    ElMessage.success('提案已执行');
    await Promise.all([loadProposals(), loadSession()]);
    if (proposal) {
      emit('proposalExecuted', {
        action: proposal.action,
        proposalNo: proposal.proposalNo,
      });
    }
  } finally {
    actionLoading.value = null;
  }
}

async function handleCancel(proposalNo: string) {
  actionLoading.value = proposalNo;
  try {
    await cancelOrchestrationProposal(proposalNo);
    ElMessage.info('提案已取消');
    await loadProposals();
  } finally {
    actionLoading.value = null;
  }
}

function actionLabel(action: string) {
  switch (action) {
    case 'CLASSIFY_CONFIRM':
      return '分类确认';
    case 'CREATE_CONTRACTS_BATCH':
      return '创建合同';
    case 'CREATE_TYPE_PACKAGE':
      return '发布类型';
    default:
      return action;
  }
}

function openContract(contractId: number) {
  router.push({ path: '/legal/contract/review', query: { id: String(contractId) } });
}

async function refreshAll() {
  await Promise.all([loadProposals(), loadSession()]);
}

watch(
  () => [props.conversationId, props.refreshKey],
  () => {
    if (autoResumedConversationId.value !== props.conversationId) {
      autoResumedConversationId.value = null;
    }
    refreshAll();
  },
);

onMounted(() => {
  refreshAll();
});
</script>

<template>
  <div v-if="conversationId" class="legal-orch-proposals">
    <div
      v-if="!hasPanelContent && !loading"
      class="legal-orch-proposals__compact"
    >
      <span class="legal-orch-proposals__title">法务编排待确认</span>
      <ElTag v-if="session?.phase" size="small" type="info">{{ session.phase }}</ElTag>
      <ElButton
        v-if="canResumeCheckpoint"
        size="small"
        link
        type="primary"
        :loading="resumeLoading"
        @click="handleResumeCheckpoint"
      >
        恢复进度
      </ElButton>
      <span class="legal-orch-proposals__hint">暂无待确认提案</span>
    </div>
    <ElCard
      v-else
      v-loading="loading"
      shadow="never"
      class="legal-orch-proposals__card"
    >
      <template #header>
        <div class="legal-orch-proposals__header">
          <div class="legal-orch-proposals__header-main">
            <span class="legal-orch-proposals__title">法务编排待确认</span>
            <ElTag v-if="session?.phase" size="small" type="info">{{ session.phase }}</ElTag>
          </div>
          <div v-if="session?.checkpointSavedAt" class="legal-orch-proposals__checkpoint">
            <span class="legal-orch-proposals__checkpoint-text">
              Checkpoint {{ formatCheckpointTime(session.checkpointSavedAt) }}
              <template v-if="session.checkpointPhase">
                · {{ session.checkpointPhase }}
              </template>
            </span>
            <ElButton
              v-if="canResumeCheckpoint"
              size="small"
              link
              type="primary"
              :loading="resumeLoading"
              @click="handleResumeCheckpoint"
            >
              恢复进度
            </ElButton>
          </div>
        </div>
      </template>
      <div class="legal-orch-proposals__body">
        <div
          v-for="item in enrichedProposals"
          :key="item.proposalNo"
          class="legal-orch-proposals__item"
        >
          <div class="legal-orch-proposals__row">
            <div class="legal-orch-proposals__meta">
              <ElTag size="small" type="warning">{{ actionLabel(item.action) }}</ElTag>
              <span class="legal-orch-proposals__item-title">{{ item.title }}</span>
            </div>
            <div class="legal-orch-proposals__actions">
              <ElButton
                type="primary"
                size="small"
                :loading="actionLoading === item.proposalNo"
                @click="handleExecute(item.proposalNo)"
              >
                确认执行
              </ElButton>
              <ElButton
                size="small"
                :disabled="actionLoading === item.proposalNo"
                @click="handleCancel(item.proposalNo)"
              >
                取消
              </ElButton>
            </div>
          </div>
          <p
            v-if="item.action === 'CLASSIFY_CONFIRM'"
            class="legal-orch-proposals__step-hint"
          >
            确认后将自动继续：Agent 将生成「创建合同」提案（创建合同仍需再点一次确认执行；落库后系统自动解析并首轮 AI 审核）。
          </p>
          <div v-if="item.createPolicy" class="legal-orch-proposals__policy">
            <span>模型 {{ modelLabel(item.createPolicy) }}</span>
            <span>立场 {{ partyRoleLabel(item.createPolicy.partyRole) }}</span>
            <span>强度 {{ auditLevelLabel(item.createPolicy.auditLevel) }}</span>
            <span v-if="session?.previewOpinionCount">
              预览 {{ session.previewOpinionCount }} 条
              <template v-if="session.previewHighRiskCount">
                （高风险 {{ session.previewHighRiskCount }}）
              </template>
            </span>
          </div>
        </div>
        <div v-if="createdContracts.length" class="legal-orch-proposals__created">
          <div class="legal-orch-proposals__created-title">已创建合同</div>
          <div
            v-for="item in createdContracts"
            :key="item.id"
            class="legal-orch-proposals__created-item"
          >
            <span>{{ item.fileName }}</span>
            <ElButton link type="primary" @click="openContract(item.contractId!)">
              #{{ item.contractId }} 查看审核
            </ElButton>
          </div>
        </div>
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.legal-orch-proposals {
  flex-shrink: 0;
  margin: 8px 0 0;
}

.legal-orch-proposals__compact {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
}

.legal-orch-proposals__hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.legal-orch-proposals__card {
  border: 1px dashed var(--el-border-color);
}

.legal-orch-proposals__card :deep(.el-card__header) {
  padding: 8px 12px;
}

.legal-orch-proposals__card :deep(.el-card__body) {
  padding: 0 12px 8px;
}

.legal-orch-proposals__body {
  max-height: 140px;
  overflow-y: auto;
}

.legal-orch-proposals__header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
}

.legal-orch-proposals__header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.legal-orch-proposals__checkpoint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.legal-orch-proposals__checkpoint-text {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.legal-orch-proposals__title {
  font-size: 13px;
  font-weight: 600;
}

.legal-orch-proposals__created {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.legal-orch-proposals__created-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.legal-orch-proposals__created-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
}

.legal-orch-proposals__item {
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.legal-orch-proposals__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.legal-orch-proposals__step-hint {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.legal-orch-proposals__policy {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.legal-orch-proposals__item:last-child {
  border-bottom: none;
}

.legal-orch-proposals__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.legal-orch-proposals__item-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legal-orch-proposals__actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
</style>
