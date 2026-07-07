<script lang="ts" setup>
import type { ContractDeliverable } from '#/api/legal/contract';

import { computed, ref } from 'vue';

import { ElButton, ElDivider, ElMessage, ElTag, ElTooltip } from 'element-plus';

import {
  downloadDeliverable,
  syncWorkingDocument,
} from '#/api/legal/contract';

import { downloadFileFromBlobPart } from '@vben/utils';

import VersionDiffPanel from './VersionDiffPanel.vue';

const props = defineProps<{
  contractId: number;
  auditRound?: number;
  roundLabel: string;
  versions: import('#/api/legal/contract').ContractVersion[];
  mainFileId?: number;
  sourceFormat?: string;
  documentRevision?: string;
  /** 已完成 AI 审核，可生成标注/修订/采纳 */
  auditDeliverablesReady?: boolean;
  /** 下载非源文件前同步 OnlyOffice 编辑内容 */
  beforeAdoptedDownload?: () => Promise<boolean | void>;
  fullPage?: boolean;
}>();

const emit = defineEmits<{
  refreshed: [];
}>();

const loadingKey = ref<string>();

const currentRound = computed(() => props.auditRound ?? 1);

/** 未传 prop 时与旧版一致：仅按格式限制；显式 false 表示 AI 尚未产出可下载衍生件 */
const deliverablesReady = computed(() => props.auditDeliverablesReady !== false);

const isPdfSource = computed(
  () => (props.sourceFormat ?? '').toUpperCase() === 'PDF',
);

interface DownloadRow {
  key: ContractDeliverable;
  label: string;
  hint: string;
}

const rows = computed<DownloadRow[]>(() => [
  {
    key: 'ORIGINAL',
    label: '源文件',
    hint: '上传原件，未修改',
  },
  {
    key: 'ANNOTATED',
    label: '标注版',
    hint: isPdfSource.value
      ? 'PDF 标准 Text 批注，正文流不变'
      : '审阅意见（Word 批注），正文不变',
  },
  {
    key: 'REVISION',
    label: '修订版',
    hint: isPdfSource.value
      ? 'PDF 合同暂不支持，请使用 Word 原件'
      : '已采纳意见之修订痕迹',
  },
  {
    key: 'ADOPTED',
    label: '采纳版',
    hint: isPdfSource.value
      ? 'PDF 合同暂不支持，请使用 Word 原件'
      : '与当前编辑页一致的干净正文',
  },
]);

function isRowDisabled(row: DownloadRow) {
  if (row.key === 'ORIGINAL') {
    return false;
  }
  if (!deliverablesReady.value) {
    return true;
  }
  return isPdfSource.value && row.key !== 'ANNOTATED';
}

function resolveFileName(row: DownloadRow) {
  const roundSuffix = props.roundLabel ? `-${props.roundLabel}` : '';
  if (row.key === 'ORIGINAL' && isPdfSource.value) {
    return `源文件${roundSuffix}.pdf`;
  }
  if (row.key === 'ANNOTATED' && isPdfSource.value) {
    return `标注版${roundSuffix}.pdf`;
  }
  const ext = row.key === 'ORIGINAL' && props.sourceFormat === 'DOC' ? 'doc' : 'docx';
  const nameMap: Record<ContractDeliverable, string> = {
    ORIGINAL: `源文件${roundSuffix}.${ext}`,
    ANNOTATED: `标注版${roundSuffix}.docx`,
    REVISION: `修订版${roundSuffix}.docx`,
    ADOPTED: `采纳版${roundSuffix}.docx`,
  };
  return nameMap[row.key];
}

async function syncBeforeDownload() {
  if (props.beforeAdoptedDownload) {
    await props.beforeAdoptedDownload();
  }
  await syncWorkingDocument(props.contractId);
  emit('refreshed');
}

async function handleDownload(row: DownloadRow) {
  if (isRowDisabled(row)) {
    if (!deliverablesReady.value && row.key !== 'ORIGINAL') {
      ElMessage.warning('请等待 AI 审核完成后再下载该版本');
    } else {
      ElMessage.warning('PDF 合同不支持下载修订版/采纳版');
    }
    return;
  }
  loadingKey.value = row.key;
  try {
    const skipSync = row.key === 'ORIGINAL'
      || (isPdfSource.value && row.key === 'ANNOTATED');
    if (!skipSync) {
      loadingKey.value = `${row.key}-sync`;
      await syncBeforeDownload();
      loadingKey.value = row.key;
    }
    const blob = await downloadDeliverable(
      props.contractId,
      row.key,
      currentRound.value,
    );
    downloadFileFromBlobPart({ fileName: resolveFileName(row), source: blob });
  } catch (error: unknown) {
    console.error('[ContractWorkbenchDownloads]', error);
    ElMessage.error('下载失败，请稍后重试');
  } finally {
    loadingKey.value = undefined;
  }
}

function rowStatus(row: DownloadRow) {
  if (isRowDisabled(row)) {
    if (row.key === 'ORIGINAL') {
      return { text: '按需生成', type: 'success' as const };
    }
    if (!deliverablesReady.value) {
      return { text: '待审核', type: 'warning' as const };
    }
    return { text: '不可用', type: 'info' as const };
  }
  if (row.key === 'ADOPTED' && props.documentRevision) {
    return { text: `已同步 ${props.documentRevision}`, type: 'success' as const };
  }
  return { text: '按需生成', type: 'success' as const };
}

const internalVersions = computed(() =>
  props.versions.filter((item) => item.visibility === 'INTERNAL'),
);
</script>

<template>
  <div
    class="contract-workbench-downloads flex h-full min-h-0 flex-col gap-3 overflow-y-auto pb-2 pt-1"
    :class="fullPage ? 'w-full gap-4 pb-2' : 'gap-2 px-2'"
  >
    <p
      class="m-0 leading-relaxed text-muted-foreground"
      :class="fullPage ? 'text-sm' : 'text-xs'"
    >
      下载当前轮次四件套。标注/修订/采纳会先同步 OnlyOffice 编辑内容，再按需生成（不读历史缓存）。
      <span v-if="isPdfSource" class="text-warning">
        PDF 原件可下载源文件与标注版；修订版/采纳版需 Word 原件。
      </span>
    </p>
    <div
      class="download-card-grid w-full gap-3"
      :class="
        fullPage
          ? 'grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4'
          : 'flex flex-col gap-2'
      "
    >
      <div
        v-for="row in rows"
        :key="row.key"
        class="rounded-lg border border-border bg-card px-3 py-2.5"
        :class="[fullPage ? 'px-4 py-3' : '', isRowDisabled(row) ? 'opacity-60' : '']"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0 flex-1">
            <div
              class="font-medium text-foreground"
              :class="fullPage ? 'text-base' : 'text-sm'"
            >
              {{ row.label }}
            </div>
            <div
              class="mt-0.5 text-muted-foreground"
              :class="fullPage ? 'text-sm' : 'text-xs'"
            >
              {{ row.hint }}
            </div>
          </div>
          <ElTag size="small" :type="rowStatus(row).type" effect="plain">
            {{ rowStatus(row).text }}
          </ElTag>
        </div>
        <div class="mt-2 flex justify-end">
          <ElTooltip
            :content="row.hint"
            :disabled="!isRowDisabled(row)"
            placement="top"
          >
            <ElButton
              :size="fullPage ? 'default' : 'small'"
              type="primary"
              :link="!fullPage"
              :plain="fullPage"
              :disabled="isRowDisabled(row)"
              :loading="loadingKey === row.key || loadingKey === `${row.key}-sync`"
              @click="handleDownload(row)"
            >
              下载
            </ElButton>
          </ElTooltip>
        </div>
      </div>
    </div>

    <template v-if="internalVersions.length >= 2">
      <ElDivider class="!my-1" />
      <div
        class="font-medium text-foreground"
        :class="fullPage ? 'text-base' : 'text-sm'"
      >
        版本对比
      </div>
      <VersionDiffPanel
        :embedded="!fullPage"
        :full-width="fullPage"
        :contract-id="contractId"
        :versions="internalVersions"
      />
    </template>
  </div>
</template>
