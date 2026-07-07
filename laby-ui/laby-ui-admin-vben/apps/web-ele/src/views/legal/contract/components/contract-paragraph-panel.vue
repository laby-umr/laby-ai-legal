<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, nextTick, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElCheckbox, ElInput, ElScrollbar, ElTag } from 'element-plus';

const props = defineProps<{
  paragraphs: LegalContractApi.Paragraph[];
  activeParagraphId?: string;
  /** 是否可标记「不需审核」 */
  skipEditable?: boolean;
}>();

const emit = defineEmits<{
  toggleSkip: [paragraphId: string, skipAudit: boolean];
}>();

const keyword = ref('');
const listRef = ref<InstanceType<typeof ElScrollbar>>();
const expandedIds = ref<Set<string>>(new Set());

const PREVIEW_LEN = 280;

const sortedParagraphs = computed(() =>
  [...props.paragraphs].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
);

const filteredParagraphs = computed(() => {
  const k = keyword.value.trim().toLowerCase();
  if (!k) {
    return sortedParagraphs.value;
  }
  return sortedParagraphs.value.filter(
    (item) =>
      item.text.toLowerCase().includes(k) ||
      item.paragraphId.toLowerCase().includes(k) ||
      (item.path?.toLowerCase().includes(k) ?? false),
  );
});

function isActive(paragraphId: string) {
  return props.activeParagraphId === paragraphId;
}

function isExpanded(paragraphId: string) {
  return expandedIds.value.has(paragraphId) || isActive(paragraphId);
}

function needsCollapse(text: string) {
  return text.length > PREVIEW_LEN;
}

function displayText(item: LegalContractApi.Paragraph) {
  if (!needsCollapse(item.text) || isExpanded(item.paragraphId)) {
    return item.text;
  }
  return `${item.text.slice(0, PREVIEW_LEN)}…`;
}

function toggleExpand(paragraphId: string) {
  const next = new Set(expandedIds.value);
  if (next.has(paragraphId)) {
    next.delete(paragraphId);
  } else {
    next.add(paragraphId);
  }
  expandedIds.value = next;
}

function getScrollWrap(): HTMLElement | null {
  const scrollbar = listRef.value;
  if (!scrollbar) {
    return null;
  }
  return (
    (scrollbar.wrapRef as HTMLElement | undefined) ??
    (scrollbar.$el?.querySelector?.('.el-scrollbar__wrap') as HTMLElement | null)
  );
}

watch(
  () => props.activeParagraphId,
  async (id) => {
    if (!id) {
      return;
    }
    expandedIds.value = new Set([...expandedIds.value, id]);
    await nextTick();
    const wrap = getScrollWrap();
    const el = wrap?.querySelector<HTMLElement>(`[data-paragraph-id="${id}"]`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  },
);
</script>

<template>
  <div class="contract-paragraph-panel flex flex-col gap-2">
    <ElInput
      v-model="keyword"
      clearable
      size="small"
      placeholder="搜索段落内容或编号"
      class="shrink-0"
    >
      <template #prefix>
        <IconifyIcon icon="lucide:search" class="size-4 text-muted-foreground" />
      </template>
    </ElInput>

    <ElScrollbar ref="listRef" max-height="420px" class="min-h-[200px]">
      <div
        v-if="filteredParagraphs.length === 0"
        class="flex flex-col items-center justify-center gap-2 py-10 text-muted-foreground"
      >
        <IconifyIcon icon="lucide:file-text" class="size-10 opacity-40" />
        <p class="m-0 text-sm">
          {{
            paragraphs.length === 0
              ? '暂无段落（请等待合同解析完成）'
              : '无匹配段落，请调整搜索词'
          }}
        </p>
      </div>

      <article
        v-for="(item, index) in filteredParagraphs"
        :key="item.paragraphId"
        :data-paragraph-id="item.paragraphId"
        class="contract-paragraph-panel__item group relative mb-3 rounded-lg border px-3 py-2.5 transition-all last:mb-0"
        :class="
          isActive(item.paragraphId)
            ? 'border-primary/50 bg-primary/5 shadow-sm ring-1 ring-primary/20'
            : 'border-border bg-muted/20 hover:border-border/80 hover:bg-muted/35'
        "
      >
        <span
          class="absolute left-0 top-3 bottom-3 w-0.5 rounded-full transition-colors"
          :class="isActive(item.paragraphId) ? 'bg-primary' : 'bg-transparent group-hover:bg-border'"
        />

        <header class="mb-2 flex flex-wrap items-center gap-2 pl-1">
          <span
            class="inline-flex size-6 shrink-0 items-center justify-center rounded-md bg-background text-xs font-semibold text-muted-foreground ring-1 ring-border"
          >
            {{ item.sort ?? index + 1 }}
          </span>
          <ElTag size="small" type="info" effect="plain" class="!font-mono !text-xs">
            {{ item.paragraphId }}
          </ElTag>
          <ElTag v-if="item.skipAudit" size="small" type="warning" effect="light">
            不需审核
          </ElTag>
          <span
            v-if="item.path"
            class="truncate text-xs text-muted-foreground"
            :title="item.path"
          >
            {{ item.path }}
          </span>
        </header>

        <p
          class="m-0 whitespace-pre-wrap pl-1 text-[0.8125rem] leading-7 text-foreground/90"
        >
          {{ displayText(item) }}
        </p>

        <div class="mt-2 flex flex-wrap items-center gap-3 pl-1">
          <button
            v-if="needsCollapse(item.text)"
            type="button"
            class="text-xs text-primary hover:underline"
            @click="toggleExpand(item.paragraphId)"
          >
            {{ isExpanded(item.paragraphId) ? '收起' : '展开全文' }}
          </button>
          <ElCheckbox
            v-if="skipEditable"
            :model-value="!!item.skipAudit"
            @update:model-value="emit('toggleSkip', item.paragraphId, !!$event)"
          >
            <span class="text-xs">标记为不需 AI 审核</span>
          </ElCheckbox>
        </div>
      </article>
    </ElScrollbar>
  </div>
</template>

<style scoped>
.contract-paragraph-panel :deep(.el-input__wrapper) {
  border-radius: 0.5rem;
}
</style>
