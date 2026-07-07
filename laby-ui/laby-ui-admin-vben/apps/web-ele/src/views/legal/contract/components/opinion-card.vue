<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { IconifyIcon } from '@vben/icons';

import { ElButton, ElLink, ElTag, ElTooltip } from 'element-plus';

import { OPINION_STATUS } from '#/api/legal/contract';

import { LEGAL_RISK_LEVEL } from '../constants';
import {
  isInsertClauseOpinion,
  isOpinionDocumentWritable,
  opinionChangeTypeLabel,
} from '../utils/opinion';

export interface EvidenceRef {
  sourceType?: string;
  sourceId?: string;
  documentId?: string;
  excerpt?: string;
}

const router = useRouter();

const props = defineProps<{
  opinion: LegalContractApi.Opinion;
  loading?: boolean;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  adopt: [id: number];
  ignore: [id: number];
  revoke: [id: number];
  locate: [paragraphId: string, locateText?: string];
}>();

function handleLocateClick() {
  const paragraphId = props.opinion.paragraphId;
  if (!paragraphId) {
    return;
  }
  const locateText =
    props.opinion.oldText?.trim() || props.opinion.newText?.trim();
  emit('locate', paragraphId, locateText);
}

const SOURCE_TYPE_LABEL: Record<string, string> = {
  AI: 'AI',
  RULE: '审核规则',
  STANDARD_CLAUSE: '标准条款',
  KNOWLEDGE: '知识库',
  MANUAL: '人工',
};

const canRevoke = computed(
  () =>
    !props.readonly &&
    (props.opinion.status === OPINION_STATUS.ADOPTED ||
      props.opinion.status === OPINION_STATUS.IGNORED),
);

const riskTagType = computed(() => {
  const level = props.opinion.riskLevel?.toUpperCase();
  if (level === LEGAL_RISK_LEVEL.HIGH) {
    return 'danger';
  }
  if (level === LEGAL_RISK_LEVEL.MEDIUM) {
    return 'warning';
  }
  return 'info';
});

const statusLabel = computed(() => {
  switch (props.opinion.status) {
    case OPINION_STATUS.ADOPTED:
      return { text: '已采纳', type: 'success' as const };
    case OPINION_STATUS.IGNORED:
      return { text: '已忽略', type: 'info' as const };
    default:
      return { text: '待处置', type: 'warning' as const };
  }
});

const isPending = computed(
  () => props.opinion.status === OPINION_STATUS.PENDING,
);

const documentWritable = computed(() => isOpinionDocumentWritable(props.opinion));

const changeTypeLabel = computed(() =>
  opinionChangeTypeLabel(props.opinion.changeType),
);

const isInsertClause = computed(() => isInsertClauseOpinion(props.opinion));

const adoptTooltip = computed(() =>
  documentWritable.value
    ? '采纳后将把「修改后正文」写入工作版合同'
    : '该意见仅有风险提示/修改说明，采纳后不会自动改正文，请在编辑器中手工修改',
);

const evidenceList = computed<EvidenceRef[]>(() => {
  if (props.opinion.evidenceRefs) {
    try {
      const parsed = JSON.parse(props.opinion.evidenceRefs) as EvidenceRef[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  if (props.opinion.sourceId && props.opinion.sourceType && props.opinion.sourceType !== 'AI') {
    return [
      {
        sourceType: props.opinion.sourceType,
        sourceId: props.opinion.sourceId,
        excerpt: props.opinion.referenceClause,
      },
    ];
  }
  return [];
});

function sourceTypeLabel(type?: string) {
  if (!type) {
    return '来源';
  }
  return SOURCE_TYPE_LABEL[type] ?? type;
}

function isNumericSegmentId(id?: string) {
  return !!id?.trim() && /^\d+$/.test(id.trim());
}

function evidenceRoute(evidence: EvidenceRef) {
  const id = evidence.sourceId?.trim();
  if (!id) {
    return null;
  }
  const type = evidence.sourceType?.toUpperCase();
  if (type === 'RULE') {
    return { path: '/legal/rule/audit-rule', query: { id } };
  }
  if (type === 'STANDARD_CLAUSE') {
    return { path: '/legal/rule/standard-clause', query: { id } };
  }
  if (type === 'KNOWLEDGE') {
    const query: Record<string, string> = { id };
    if (evidence.documentId) {
      query.documentId = evidence.documentId;
    }
    return { path: '/ai/knowledge/segment', query };
  }
  return null;
}

function openEvidence(evidence: EvidenceRef) {
  const route = evidenceRoute(evidence);
  if (route) {
    router.push(route);
  }
}
</script>

<template>
  <div
    class="opinion-card-item rounded-lg border border-border bg-card p-4 transition-shadow hover:shadow-sm"
    :class="{ 'ring-1 ring-primary/25': isPending && !readonly }"
  >
    <div class="mb-2 flex flex-wrap items-center gap-2">
      <ElTag :type="riskTagType" size="small" effect="light">
        {{ opinion.riskLevel }}
      </ElTag>
      <ElTag size="small" type="warning" effect="plain">
        {{ changeTypeLabel }}
      </ElTag>
      <ElTag v-if="opinion.clauseType" size="small" type="info" effect="plain">
        {{ opinion.clauseType }}
      </ElTag>
      <ElTag
        v-if="isPending && !documentWritable"
        size="small"
        type="info"
        effect="plain"
      >
        需人工改正文
      </ElTag>
      <ElTag :type="statusLabel.type" size="small" effect="light">
        {{ statusLabel.text }}
      </ElTag>
      <span v-if="opinion.auditRound" class="text-xs text-muted-foreground">
        第 {{ opinion.auditRound }} 轮
      </span>
    </div>
    <h4 class="mb-1.5 text-base font-medium leading-snug text-foreground">
      {{ opinion.title }}
    </h4>
    <p class="m-0 text-sm leading-relaxed text-muted-foreground">
      {{ opinion.content }}
    </p>
    <p
      v-if="opinion.suggestion"
      class="mt-2 rounded-md bg-primary/5 px-2.5 py-2 text-sm text-primary"
    >
      修改说明：{{ opinion.suggestion }}
    </p>
    <div
      v-if="opinion.oldText || opinion.newText"
      class="mt-2 space-y-2 rounded-md border border-emerald-500/30 bg-emerald-500/5 px-2.5 py-2 text-sm"
    >
      <div class="text-xs font-medium text-emerald-700 dark:text-emerald-300">
        {{
          isInsertClause
            ? `采纳后将在段落 ${opinion.paragraphId || '-'} 后新增条款`
            : '采纳后将写入合同的改写内容'
        }}
      </div>
      <p v-if="opinion.oldText" class="m-0 whitespace-pre-wrap leading-relaxed">
        <span class="text-muted-foreground">原文：</span>
        <span class="line-through opacity-80">{{ opinion.oldText }}</span>
      </p>
      <p v-if="opinion.newText" class="m-0 whitespace-pre-wrap leading-relaxed text-foreground">
        <span class="font-medium text-emerald-700 dark:text-emerald-300">
          {{ isInsertClause ? '新增条款：' : '改后正文：' }}
        </span>
        {{ opinion.newText }}
      </p>
    </div>
    <p
      v-if="opinion.referenceClause"
      class="mt-2 rounded-md bg-amber-500/10 px-2.5 py-2 text-sm text-amber-800 dark:text-amber-200"
    >
      对照标准条款：{{ opinion.referenceClause }}
    </p>
    <div
      v-if="evidenceList.length"
      class="mt-2 space-y-1.5 rounded-md border border-border/80 bg-muted/20 px-2.5 py-2"
    >
      <div class="text-xs font-medium text-muted-foreground">审核依据</div>
      <div
        v-for="(evidence, index) in evidenceList"
        :key="`${evidence.sourceType}-${evidence.sourceId}-${index}`"
        class="text-xs leading-relaxed text-foreground/90"
      >
        <ElTag size="small" type="info" effect="plain" class="mr-1">
          {{ sourceTypeLabel(evidence.sourceType) }}
        </ElTag>
        <ElLink
          v-if="evidence.sourceId && evidenceRoute(evidence)"
          type="primary"
          class="font-mono text-xs"
          @click="openEvidence(evidence)"
        >
          #{{ evidence.sourceId }}
        </ElLink>
        <span
          v-else-if="evidence.sourceId"
          class="font-mono text-muted-foreground"
        >
          #{{ evidence.sourceId }}
        </span>
        <p
          v-if="evidence.excerpt"
          class="m-0 mt-1 whitespace-pre-wrap text-muted-foreground"
        >
          {{ evidence.excerpt }}
        </p>
      </div>
    </div>
    <div v-if="opinion.paragraphId" class="mt-2">
      <ElButton
        link
        type="primary"
        size="small"
        @click="handleLocateClick"
      >
        <IconifyIcon icon="lucide:map-pin" class="mr-0.5 size-3.5" />
        定位段落 {{ opinion.paragraphId }}
      </ElButton>
    </div>
    <div
      v-if="(isPending || canRevoke) && !readonly"
      class="mt-3 flex flex-wrap gap-2 border-t border-border pt-3"
    >
      <template v-if="isPending">
        <ElTooltip :content="adoptTooltip" placement="top">
          <ElButton
            size="small"
            type="success"
            :loading="loading"
            @click="emit('adopt', opinion.id)"
          >
            采纳
          </ElButton>
        </ElTooltip>
        <ElButton size="small" :loading="loading" @click="emit('ignore', opinion.id)">
          忽略
        </ElButton>
      </template>
      <ElButton
        v-else-if="canRevoke"
        size="small"
        plain
        :loading="loading"
        @click="emit('revoke', opinion.id)"
      >
        撤销处置
      </ElButton>
    </div>
  </div>
</template>
