<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, nextTick, ref, watch } from 'vue';

import {
  resolveParagraphDisplayStyle,
  sortParagraphs,
} from './utils/contract-document-style';

const props = defineProps<{
  paragraphs: LegalContractApi.Paragraph[];
  activeParagraphId?: string;
  highlightParagraphIds?: string[];
  showParagraphIds?: boolean;
}>();

const emit = defineEmits<{
  selectParagraph: [paragraphId: string];
}>();

const containerRef = ref<HTMLElement | null>(null);

const sortedParagraphs = computed(() => sortParagraphs(props.paragraphs));

watch(
  () => props.activeParagraphId,
  async (id) => {
    if (!id) {
      return;
    }
    await nextTick();
    const el = containerRef.value?.querySelector(`[data-paragraph-id="${id}"]`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  },
);

function isHighlighted(paragraphId: string) {
  if (props.activeParagraphId === paragraphId) {
    return true;
  }
  return props.highlightParagraphIds?.includes(paragraphId);
}

function paragraphClass(text: string, index: number) {
  return `legal-reader--${resolveParagraphDisplayStyle(text, index)}`;
}
</script>

<template>
  <div
    ref="containerRef"
    class="legal-contract-reader min-h-0 min-w-0 flex-1 overflow-y-auto rounded-lg border border-border bg-muted/25 px-4 py-5"
  >
    <article class="legal-contract-paper mx-auto">
      <div
        v-for="(p, index) in sortedParagraphs"
        :key="p.paragraphId"
        :data-paragraph-id="p.paragraphId"
        class="legal-reader-paragraph cursor-pointer transition-colors"
        :class="[
          paragraphClass(p.text, index),
          isHighlighted(p.paragraphId)
            ? 'legal-reader-paragraph--active'
            : 'legal-reader-paragraph--idle',
        ]"
        @click="emit('selectParagraph', p.paragraphId)"
      >
        <div
          v-if="showParagraphIds"
          class="legal-reader-meta mb-1 flex items-center gap-2 text-xs text-muted-foreground"
        >
          <span>{{ p.paragraphId }}</span>
          <span v-if="p.skipAudit" class="text-amber-600">跳过 AI</span>
        </div>
        <div class="legal-reader-text">{{ p.text }}</div>
      </div>
      <div
        v-if="sortedParagraphs.length === 0"
        class="py-16 text-center text-sm text-muted-foreground"
      >
        暂无合同正文
      </div>
    </article>
  </div>
</template>

<style scoped>
.legal-contract-reader {
  max-height: calc(100vh - 280px);
}

.legal-contract-paper {
  max-width: 210mm;
  min-height: 280mm;
  padding: 48px 56px 64px;
  background: #fff;
  color: #1a1a1a;
  font-family: 'SimSun', 'Songti SC', 'STSong', serif;
  font-size: 14px;
  line-height: 1.85;
  box-shadow:
    0 1px 3px rgb(0 0 0 / 8%),
    0 8px 24px rgb(0 0 0 / 6%);
}

.legal-reader-paragraph--idle:hover {
  background: rgb(0 0 0 / 2%);
}

.legal-reader-paragraph--active {
  background: rgb(59 130 246 / 8%);
  box-shadow: inset 3px 0 0 rgb(59 130 246 / 55%);
}

.legal-reader--title .legal-reader-text {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  line-height: 1.5;
  margin: 12px 0 24px;
}

.legal-reader--heading1 .legal-reader-text {
  font-size: 16px;
  font-weight: 700;
  margin: 18px 0 10px;
}

.legal-reader--heading2 .legal-reader-text {
  font-size: 15px;
  font-weight: 700;
  margin: 16px 0 8px;
}

.legal-reader--heading3 .legal-reader-text {
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 6px;
}

.legal-reader--body .legal-reader-text {
  text-align: justify;
  text-indent: 2em;
  margin: 0 0 4px;
}

.legal-reader--signature .legal-reader-text {
  margin: 8px 0;
}
</style>
