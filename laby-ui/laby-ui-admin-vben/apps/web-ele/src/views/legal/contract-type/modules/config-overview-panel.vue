<script lang="ts" setup>
import type { LegalContractTypeApi } from '#/api/legal/contract-type';

import { ref, watch } from 'vue';

import { ElMessage } from 'element-plus';

import {
  getContractTypeConfigOverview,
  resolveContractTypeConfig,
} from '#/api/legal/contract-type';

const props = defineProps<{
  contractTypeId?: number;
}>();

const loading = ref(false);
const loadError = ref(false);
const overview = ref<LegalContractTypeApi.ConfigOverview>();
const resolvePreview = ref<LegalContractTypeApi.ConfigResolve>();
const showResolve = ref(false);

async function load() {
  if (!props.contractTypeId) {
    overview.value = undefined;
    resolvePreview.value = undefined;
    loadError.value = false;
    return;
  }
  loading.value = true;
  loadError.value = false;
  try {
    overview.value = await getContractTypeConfigOverview(props.contractTypeId);
    if (showResolve.value) {
      resolvePreview.value = await resolveContractTypeConfig(props.contractTypeId);
    }
  } catch {
    loadError.value = true;
    overview.value = undefined;
    resolvePreview.value = undefined;
    ElMessage.error('加载配置中枢失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

async function toggleResolve() {
  showResolve.value = !showResolve.value;
  if (showResolve.value && props.contractTypeId && !resolvePreview.value) {
    try {
      resolvePreview.value = await resolveContractTypeConfig(props.contractTypeId);
    } catch {
      ElMessage.error('加载运行时解析预览失败');
      showResolve.value = false;
    }
  }
}

watch(
  () => props.contractTypeId,
  () => {
    showResolve.value = false;
    resolvePreview.value = undefined;
    load();
  },
  { immediate: true },
);

defineExpose({ reload: load });
</script>

<template>
  <div
    v-if="contractTypeId"
    v-loading="loading"
    class="config-overview rounded-lg border border-border bg-muted/30 p-3 text-sm"
  >
    <div class="mb-2 font-medium text-foreground">配置中枢</div>
    <p v-if="loadError" class="text-xs text-destructive">配置加载失败，请关闭弹窗后重试。</p>
    <template v-else-if="overview">
      <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-muted-foreground">
        <div>
          知识库：
          <span class="text-foreground">{{
            overview.knowledgeName || '未绑定'
          }}</span>
        </div>
        <div>
          审核规则：
          <span class="text-foreground">{{ overview.enabledAuditRuleCount ?? 0 }} 条生效</span>
        </div>
        <div class="col-span-2">
          审核技能包：
          <span class="text-foreground">{{
            overview.auditSkillPack?.configured
              ? `${overview.auditSkillPack.name} → ${overview.auditSkillPack.chatRoleName || '未绑角色'}`
              : '未配置'
          }}</span>
        </div>
        <div class="col-span-2">
          对话技能包：
          <span class="text-foreground">{{
            overview.chatSkillPack?.configured
              ? `${overview.chatSkillPack.name}（${overview.chatSkillPack.toolNames?.length ?? 0} 工具）`
              : '未配置'
          }}</span>
        </div>
      </div>
      <ul v-if="overview.checklist?.length" class="mt-3 space-y-1">
        <li
          v-for="item in overview.checklist"
          :key="item.key"
          class="flex items-start gap-2"
        >
          <span :class="item.ok ? 'text-green-600' : 'text-amber-600'">{{
            item.ok ? '✓' : '○'
          }}</span>
          <span>
            <span class="text-foreground">{{ item.label }}</span>
            <span v-if="item.hint" class="ml-1 text-xs text-muted-foreground">{{
              item.hint
            }}</span>
          </span>
        </li>
      </ul>
      <button
        type="button"
        class="mt-2 text-xs text-primary hover:underline"
        @click="toggleResolve"
      >
        {{ showResolve ? '收起' : '查看' }}运行时解析预览
      </button>
      <div
        v-if="showResolve && resolvePreview"
        class="mt-2 rounded border border-dashed border-border bg-card p-2 text-xs"
      >
        <div>提示词来源：{{ resolvePreview.auditPromptSource }}</div>
        <div v-if="resolvePreview.auditChatRoleName">
          角色：{{ resolvePreview.auditChatRoleName }}
        </div>
        <div v-if="resolvePreview.auditToolNames?.length">
          工具：{{ resolvePreview.auditToolNames.join(', ') }}
        </div>
        <div
          v-if="resolvePreview.auditSystemMessagePreview"
          class="mt-1 whitespace-pre-wrap text-muted-foreground"
        >
          {{ resolvePreview.auditSystemMessagePreview }}
        </div>
      </div>
    </template>
  </div>
</template>
