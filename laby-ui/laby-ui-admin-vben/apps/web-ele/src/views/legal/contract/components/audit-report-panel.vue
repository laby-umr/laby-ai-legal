<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  ElButton,
  ElDialog,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElMessage,
  ElScrollbar,
} from 'element-plus';

import {
  downloadContractFile,
  exportReportDocx,
} from '#/api/legal/contract';
import { MarkdownView } from '#/components/markdown-view';

export interface ReportTocItem {
  id: string;
  level: number;
  text: string;
}

const props = defineProps<{
  content?: string;
  emptyHint?: string;
  contractId?: number;
  contractTitle?: string;
  auditRound?: number;
  /** 嵌入审阅工作台侧栏 */
  embedded?: boolean;
}>();

const scrollRef = ref<InstanceType<typeof ElScrollbar>>();
const activeSectionId = ref<string>();
const previewVisible = ref(false);
const downloadLoading = ref(false);

const hasContent = computed(() => !!props.content?.trim());

const reportBaseName = computed(() => {
  const title = props.contractTitle?.trim() || '合同';
  const round = props.auditRound ?? 1;
  return `${title}-审核报告-第${round}轮`;
});

const toc = computed<ReportTocItem[]>(() => {
  if (!hasContent.value) {
    return [];
  }
  const items: ReportTocItem[] = [];
  let index = 0;
  for (const line of props.content!.split('\n')) {
    const matched = line.match(/^(#{2,3})\s+(.+)$/);
    if (!matched?.[1] || !matched[2]) {
      continue;
    }
    items.push({
      id: `legal-report-${index}`,
      level: matched[1].length,
      text: matched[2].trim(),
    });
    index += 1;
  }
  return items;
});

function getScrollWrap(el?: InstanceType<typeof ElScrollbar> | null) {
  const scrollbar = el ?? scrollRef.value;
  if (!scrollbar) {
    return null;
  }
  return (
    (scrollbar.wrapRef as HTMLElement | undefined) ??
    (scrollbar.$el?.querySelector?.('.el-scrollbar__wrap') as HTMLElement | null)
  );
}

function bindHeadingAnchors(root?: HTMLElement | null) {
  const scope =
    root ??
    getScrollWrap()?.querySelector<HTMLElement>('.legal-audit-report__body') ??
    getScrollWrap();
  if (!scope) {
    return;
  }
  const headings = scope.querySelectorAll<HTMLElement>('h2, h3');
  headings.forEach((el, i) => {
    const item = toc.value[i];
    if (item) {
      el.id = item.id;
    }
  });
}

async function scrollToSection(id: string) {
  activeSectionId.value = id;
  await nextTick();
  const wrap = getScrollWrap();
  const target = wrap?.querySelector<HTMLElement>(`#${CSS.escape(id)}`);
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function openPreview() {
  if (!hasContent.value) {
    ElMessage.warning('暂无报告内容');
    return;
  }
  previewVisible.value = true;
}

function downloadMarkdown() {
  if (!hasContent.value) {
    return;
  }
  const blob = new Blob([props.content!], {
    type: 'text/markdown;charset=utf-8',
  });
  downloadFileFromBlobPart({
    fileName: `${reportBaseName.value}.md`,
    source: blob,
  });
  ElMessage.success('Markdown 已下载');
}

async function downloadWord() {
  if (!props.contractId) {
    ElMessage.warning('缺少合同编号，无法导出 Word');
    return;
  }
  downloadLoading.value = true;
  try {
    const fileId = await exportReportDocx(props.contractId);
    const data = await downloadContractFile(fileId);
    downloadFileFromBlobPart({
      fileName: `${reportBaseName.value}.docx`,
      source: data,
    });
    ElMessage.success('Word 报告已下载');
  } finally {
    downloadLoading.value = false;
  }
}

function handleReportAction(command: string) {
  if (command === 'preview') {
    openPreview();
    return;
  }
  if (command === 'md') {
    downloadMarkdown();
    return;
  }
  if (command === 'docx') {
    downloadWord();
  }
}

watch(
  () => props.content,
  async () => {
    activeSectionId.value = undefined;
    await nextTick();
    await nextTick();
    bindHeadingAnchors();
  },
  { immediate: true },
);

watch(previewVisible, async (visible) => {
  if (!visible) {
    return;
  }
  await nextTick();
  await nextTick();
  const article = document.querySelector<HTMLElement>(
    '.legal-audit-report-preview .legal-audit-report__body',
  );
  bindHeadingAnchors(article);
});
</script>

<template>
  <div v-if="!hasContent" class="py-8" :class="{ 'px-2': embedded }">
    <ElEmpty :description="emptyHint || '暂无 AI 审核报告'" />
  </div>
  <div
    v-else
    class="legal-audit-report flex flex-col"
    :class="embedded ? 'h-full min-h-0 px-2 pb-2 pt-1' : 'h-full min-h-0 flex-1'"
  >
    <div class="mb-3 flex flex-wrap items-center justify-end gap-2">
      <ElDropdown trigger="click" @command="handleReportAction">
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs text-primary transition-colors hover:bg-primary/10"
        >
          <IconifyIcon icon="lucide:file-text" class="size-3.5" />
          Markdown 结构化报告
          <IconifyIcon icon="lucide:chevron-down" class="size-3.5 opacity-70" />
        </button>
        <template #dropdown>
          <ElDropdownMenu>
            <ElDropdownItem command="preview">
              <IconifyIcon icon="lucide:eye" class="mr-1.5 inline size-4" />
              预览
            </ElDropdownItem>
            <ElDropdownItem command="md">
              <IconifyIcon icon="lucide:download" class="mr-1.5 inline size-4" />
              下载 Markdown
            </ElDropdownItem>
            <ElDropdownItem command="docx" :disabled="!contractId">
              <IconifyIcon
                icon="lucide:file-down"
                class="mr-1.5 inline size-4"
              />
              下载 Word
            </ElDropdownItem>
          </ElDropdownMenu>
        </template>
      </ElDropdown>
      <ElButton link type="primary" size="small" @click="openPreview">
        预览
      </ElButton>
      <ElButton
        link
        type="primary"
        size="small"
        :loading="downloadLoading"
        @click="downloadWord"
      >
        下载 Word
      </ElButton>
    </div>

    <nav
      v-if="toc.length > 0"
      class="mb-3 shrink-0 rounded-lg border border-border bg-muted/30 p-2"
    >
      <div
        class="mb-1.5 flex items-center gap-1 text-xs font-medium text-muted-foreground"
      >
        <IconifyIcon icon="lucide:list-tree" class="size-3.5" />
        章节目录
      </div>
      <ElScrollbar max-height="120px">
        <ul class="m-0 list-none space-y-0.5 p-0">
          <li v-for="item in toc" :key="item.id">
            <button
              type="button"
              class="w-full rounded-md px-2 py-1 text-left text-sm transition-colors hover:bg-accent"
              :class="[
                item.level === 3 ? 'pl-5' : '',
                activeSectionId === item.id
                  ? 'bg-primary/10 font-medium text-primary'
                  : 'text-foreground/85',
              ]"
              @click="scrollToSection(item.id)"
            >
              {{ item.text }}
            </button>
          </li>
        </ul>
      </ElScrollbar>
    </nav>

    <ElScrollbar ref="scrollRef" class="min-h-0 flex-1">
      <article
        class="legal-audit-report__body rounded-lg border border-border bg-card px-4 py-3 shadow-sm"
      >
        <MarkdownView :content="content!" />
      </article>
    </ElScrollbar>

    <ElDialog
      v-model="previewVisible"
      class="legal-audit-report-preview"
      :title="`${reportBaseName}（预览）`"
      width="88%"
      top="4vh"
      destroy-on-close
    >
      <ElScrollbar max-height="72vh">
        <article
          class="legal-audit-report__body rounded-lg border border-border bg-card px-5 py-4"
        >
          <MarkdownView :content="content!" />
        </article>
      </ElScrollbar>
      <template #footer>
        <ElButton @click="previewVisible = false">关闭</ElButton>
        <ElButton type="primary" plain @click="downloadMarkdown">
          下载 Markdown
        </ElButton>
        <ElButton
          type="primary"
          :loading="downloadLoading"
          :disabled="!contractId"
          @click="downloadWord"
        >
          下载 Word
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.legal-audit-report__body :deep(.markdown-view) {
  font-size: 0.875rem;
  line-height: 1.75rem;
  color: hsl(var(--foreground) / 0.9);
}

.legal-audit-report__body :deep(.markdown-view h2) {
  margin-top: 1.25rem;
  padding-bottom: 0.35rem;
  font-size: 1.05rem;
  border-bottom: 1px solid hsl(var(--border));
}

.legal-audit-report__body :deep(.markdown-view h3) {
  margin-top: 0.85rem;
  font-size: 0.95rem;
  color: hsl(var(--foreground) / 0.85);
}

.legal-audit-report__body :deep(.markdown-view table) {
  display: block;
  width: 100%;
  margin: 0.75rem 0;
  overflow-x: auto;
  font-size: 0.8125rem;
  border-collapse: collapse;
}

.legal-audit-report__body :deep(.markdown-view th),
.legal-audit-report__body :deep(.markdown-view td) {
  padding: 0.4rem 0.6rem;
  border: 1px solid hsl(var(--border));
}

.legal-audit-report__body :deep(.markdown-view th) {
  background: hsl(var(--muted) / 0.5);
}

.legal-audit-report__body :deep(.markdown-view blockquote) {
  margin: 0.75rem 0;
  padding: 0.5rem 0.75rem;
  border-left: 3px solid hsl(var(--primary));
  background: hsl(var(--muted) / 0.35);
  color: hsl(var(--foreground) / 0.8);
}

.legal-audit-report__body :deep(.markdown-view ul),
.legal-audit-report__body :deep(.markdown-view ol) {
  padding-left: 1.25rem;
}

.legal-audit-report__body :deep(.markdown-view li) {
  margin-bottom: 0.35rem;
}
</style>
