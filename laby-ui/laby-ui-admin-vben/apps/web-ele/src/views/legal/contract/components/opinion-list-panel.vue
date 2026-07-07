<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, ref } from 'vue';

import {
  ElButton,
  ElCheckbox,
  ElCollapse,
  ElCollapseItem,
  ElEmpty,
  ElInput,
  ElOption,
  ElSelect,
} from 'element-plus';

import OpinionCard from './opinion-card.vue';
import { FEEDBACK_SUMMARY_MIN_LENGTH } from '../constants';

const props = defineProps<{
  opinions: LegalContractApi.Opinion[];
  filteredOpinions: LegalContractApi.Opinion[];
  displayOpinions: LegalContractApi.Opinion[];
  pendingCount: number;
  riskLevelOptions: { label: string; value: string }[];
  canApplyAnnotations?: boolean;
  annotateLoading?: boolean;
  riskFilter: string;
  statusFilter: string;
  sourceFilter: string;
  statusFilterOptions: { label: string; value: string }[];
  sourceFilterOptions: { label: string; value: string }[];
  opinionEditable: boolean;
  readonlyMode: boolean;
  embeddedInBpm: boolean;
  actionLoading: boolean;
  loading: boolean;
  needSecondRound: boolean;
  secondRoundApplicable: boolean;
  opinionCompletable?: boolean;
  feedbackSummary: string;
  /** 二轮/首轮 AI 审核进行中，隐藏提交区并展示进度 */
  aiAuditing?: boolean;
  auditRound?: number;
  /** 嵌入审阅工作台侧栏时使用更紧凑布局 */
  embedded?: boolean;
}>();

const submitLabel = computed(() => {
  if (props.aiAuditing) {
    return (props.auditRound ?? 1) >= 2 ? '二轮 AI 审核中' : 'AI 审核中';
  }
  return props.secondRoundApplicable ? '保存处置结果' : '完成二轮复核';
});

const emit = defineEmits<{
  'update:riskFilter': [value: string];
  'update:statusFilter': [value: string];
  'update:sourceFilter': [value: string];
  'update:needSecondRound': [value: boolean];
  'update:feedbackSummary': [value: string];
  adopt: [id: number];
  ignore: [id: number];
  revoke: [id: number];
  locate: [paragraphId: string, locateText?: string];
  batchAdopt: [];
  openManual: [];
  applyAnnotations: [];
  submit: [];
  goBpmTodo: [];
}>();

const total = computed(() => props.filteredOpinions.length);
const secondRoundExpanded = ref<string[]>([]);
</script>

<template>
  <div
    class="opinion-list-panel flex h-full min-h-0 flex-col overflow-hidden"
    :class="embedded ? 'gap-2' : 'gap-3'"
  >
    <div
      class="flex shrink-0 flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-muted/20"
      :class="embedded ? 'px-2 py-2' : 'gap-3 px-4 py-3'"
    >
      <div class="flex flex-wrap items-center gap-2 text-sm">
        <span class="text-muted-foreground">待处置</span>
        <span class="font-semibold text-primary">{{ pendingCount }}</span>
        <span class="text-muted-foreground">/ 共 {{ opinions.length }} 条</span>
        <span v-if="total !== opinions.length" class="text-muted-foreground">
          （筛选后 {{ total }} 条）
        </span>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ElSelect
          :model-value="riskFilter"
          clearable
          placeholder="风险等级"
          class="!w-28"
          size="small"
          @update:model-value="emit('update:riskFilter', $event ?? '')"
        >
          <ElOption label="全部风险" value="" />
          <ElOption
            v-for="item in riskLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
        <ElSelect
          :model-value="statusFilter"
          clearable
          placeholder="处置状态"
          class="!w-28"
          size="small"
          @update:model-value="emit('update:statusFilter', $event ?? '')"
        >
          <ElOption
            v-for="item in statusFilterOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
        <ElSelect
          :model-value="sourceFilter"
          clearable
          placeholder="意见来源"
          class="!w-28"
          size="small"
          @update:model-value="emit('update:sourceFilter', $event ?? '')"
        >
          <ElOption
            v-for="item in sourceFilterOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </ElSelect>
        <ElButton
          v-if="opinionEditable"
          type="success"
          plain
          size="small"
          :loading="actionLoading"
          :disabled="pendingCount === 0"
          @click="emit('batchAdopt')"
        >
          {{ embedded ? '批量采纳' : '批量采纳（筛选结果）' }}
        </ElButton>
        <ElButton
          v-if="opinionEditable"
          type="primary"
          plain
          size="small"
          :loading="actionLoading"
          @click="emit('openManual')"
        >
          补充意见
        </ElButton>
        <ElButton
          v-if="canApplyAnnotations"
          type="primary"
          plain
          size="small"
          :loading="annotateLoading"
          @click="emit('applyAnnotations')"
        >
          添加标注
        </ElButton>
      </div>
    </div>

    <ElEmpty v-if="total === 0" description="暂无意见" />

    <div
      v-else
      class="opinion-list-scroll flex-1 basis-0 overflow-y-auto"
      :class="embedded ? 'min-h-[160px]' : 'min-h-[220px]'"
    >
      <div class="pr-1" :class="embedded ? 'space-y-2' : 'space-y-3'">
        <OpinionCard
          v-for="item in displayOpinions"
          :key="item.id"
          :opinion="item"
          :loading="actionLoading"
          :readonly="!opinionEditable"
          @adopt="emit('adopt', $event)"
          @ignore="emit('ignore', $event)"
          @revoke="emit('revoke', $event)"
          @locate="(paragraphId, locateText) => emit('locate', paragraphId, locateText)"
        />
      </div>
    </div>

    <div
      v-if="aiAuditing"
      class="shrink-0 rounded-lg border border-border bg-muted/15"
      :class="embedded ? 'p-2' : 'p-4'"
    >
      <slot name="audit-progress" />
      <ElButton type="primary" disabled loading class="mt-2 w-full">
        {{ submitLabel }}
      </ElButton>
    </div>

    <div
      v-else-if="opinionCompletable"
      class="shrink-0 rounded-lg border border-border bg-muted/15"
      :class="embedded ? 'p-2' : 'p-4'"
    >
      <ElCollapse
        v-if="secondRoundApplicable"
        v-model="secondRoundExpanded"
        class="opinion-second-round-collapse"
      >
        <ElCollapseItem name="second-round" title="申请二轮 AI 审核">
          <ElCheckbox
            :model-value="needSecondRound"
            @update:model-value="emit('update:needSecondRound', !!$event)"
          >
            申请二轮 AI 审核
          </ElCheckbox>
          <ElInput
            v-if="needSecondRound"
            :model-value="feedbackSummary"
            class="mt-3"
            type="textarea"
            :rows="embedded ? 3 : 4"
            :placeholder="`请填写反馈说明（至少 ${FEEDBACK_SUMMARY_MIN_LENGTH} 字），说明需二轮重点复核的条款或问题`"
            @update:model-value="emit('update:feedbackSummary', $event)"
          />
        </ElCollapseItem>
      </ElCollapse>
      <div
        class="flex flex-wrap gap-2"
        :class="secondRoundApplicable ? 'mt-3' : ''"
      >
        <ElButton type="primary" :loading="loading" @click="emit('submit')">
          {{ submitLabel }}
        </ElButton>
        <ElButton v-if="!embeddedInBpm" @click="emit('goBpmTodo')">
          前往我的待办
        </ElButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.opinion-list-scroll {
  flex: 1 1 0;
}

.opinion-second-round-collapse {
  border: none;
}

.opinion-second-round-collapse :deep(.el-collapse-item__header) {
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  border-bottom: none;
}

.opinion-second-round-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.opinion-second-round-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}
</style>
