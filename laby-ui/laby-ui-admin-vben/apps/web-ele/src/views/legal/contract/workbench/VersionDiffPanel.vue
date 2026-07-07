<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { ref, watch } from 'vue';

import { ElButton, ElEmpty, ElOption, ElSelect, ElTable, ElTableColumn, ElTag } from 'element-plus';

import { getVersionDiff } from '#/api/legal/contract';

const props = defineProps<{
  contractId: number;
  versions: Array<{
    id: number;
    versionNo: number;
    type: string;
    visibility: string;
  }>;
  /** 嵌入审阅工作台侧栏 */
  embedded?: boolean;
  /** 全页 Tab 下表格撑满宽度 */
  fullWidth?: boolean;
}>();

const fromVersionId = ref<number>();
const toVersionId = ref<number>();
const loading = ref(false);
const diffResult = ref<LegalContractApi.VersionDiffResp>();

const changeTypeMap: Record<string, { label: string; type: string }> = {
  MODIFIED: { label: '已修改', type: 'warning' },
  ADDED: { label: '新增', type: 'success' },
  REMOVED: { label: '删除', type: 'danger' },
};

async function loadDiff() {
  if (!props.contractId || !fromVersionId.value || !toVersionId.value) {
    return;
  }
  loading.value = true;
  try {
    diffResult.value = await getVersionDiff(
      props.contractId,
      fromVersionId.value,
      toVersionId.value,
    );
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.versions,
  (list) => {
    if (!list?.length) {
      return;
    }
    if (list.length >= 2) {
      fromVersionId.value = list[1]?.id;
      toVersionId.value = list[0]?.id;
    } else {
      fromVersionId.value = list[0]?.id;
      toVersionId.value = list[0]?.id;
    }
  },
  { immediate: true },
);

const VERSION_TYPE_LABEL: Record<string, string> = {
  ORIGINAL: '原始版',
  WORKING: '工作版',
  AI_ANNOTATED: '标注版',
  ADOPTED_TRACKED: '修订版',
  ADOPTED_CLEAN: '采纳-干净',
};

function versionLabel(v: { versionNo: number; type: string }) {
  const typeLabel = VERSION_TYPE_LABEL[v.type] ?? v.type;
  return `v${v.versionNo} · ${typeLabel}`;
}
</script>

<template>
  <div
    class="version-diff-panel w-full"
    :class="embedded ? 'space-y-2' : 'space-y-3'"
  >
    <div class="flex flex-wrap items-center gap-2">
      <ElSelect
        v-model="fromVersionId"
        placeholder="对比版本（旧）"
        :class="
          embedded ? '!w-full' : fullWidth ? '!w-64' : '!w-52'
        "
        clearable
        :size="embedded ? 'small' : 'default'"
      >
        <ElOption
          v-for="v in versions"
          :key="'from-' + v.id"
          :label="versionLabel(v)"
          :value="v.id"
        />
      </ElSelect>
      <span class="text-muted-foreground">→</span>
      <ElSelect
        v-model="toVersionId"
        placeholder="对比版本（新）"
        :class="
          embedded ? '!w-full' : fullWidth ? '!w-64' : '!w-52'
        "
        clearable
        :size="embedded ? 'small' : 'default'"
      >
        <ElOption
          v-for="v in versions"
          :key="'to-' + v.id"
          :label="versionLabel(v)"
          :value="v.id"
        />
      </ElSelect>
      <ElButton
        type="primary"
        :size="embedded ? 'small' : 'default'"
        :loading="loading"
        @click="loadDiff"
      >
        对比
      </ElButton>
    </div>

    <ElTable
      v-if="diffResult?.diffs?.length"
      v-loading="loading"
      class="w-full"
      :data="diffResult.diffs"
      :size="embedded ? 'small' : 'default'"
      border
      :max-height="embedded ? 280 : fullWidth ? undefined : 360"
      :style="fullWidth ? { width: '100%' } : undefined"
    >
      <ElTableColumn prop="clauseTitle" label="条款" :min-width="fullWidth ? 200 : 140" />
      <ElTableColumn label="变更" width="90">
        <template #default="{ row }">
          <ElTag
            size="small"
            :type="(changeTypeMap[row.changeType]?.type as any) || 'info'"
          >
            {{ changeTypeMap[row.changeType]?.label || row.changeType }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn
        prop="beforeText"
        label="变更前"
        :min-width="fullWidth ? 280 : 180"
        show-overflow-tooltip
      />
      <ElTableColumn
        prop="afterText"
        label="变更后"
        :min-width="fullWidth ? 280 : 180"
        show-overflow-tooltip
      />
      <ElTableColumn label="关联意见" width="90">
        <template #default="{ row }">
          {{ row.relatedOpinionIds?.length || 0 }}
        </template>
      </ElTableColumn>
    </ElTable>

    <ElEmpty
      v-else-if="diffResult && !loading"
      description="两版本无条款级差异（或解析为空）"
    />
    <ElEmpty v-else-if="!loading" description="选择两个版本后点击对比" />
  </div>
</template>
