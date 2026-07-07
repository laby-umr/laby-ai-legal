import type { LegalContractApi } from '#/api/legal/contract';

import { sortParagraphs } from './contract-document-style';

const BLOCK_SELECTOR =
  'p, li, td, th, h1, h2, h3, h4, h5, h6, .docx p';

/** 去掉空白与零宽字符，便于跨视图比对 */
export function normalizeLocateText(text: string): string {
  return text
    .replace(/[\s\u00a0\u200b\u3000]+/g, '')
    .replace(/[“”""''‘’]/g, '"')
    .trim();
}

export function collectDocxRenderableBlocks(
  root: ParentNode | null,
): HTMLElement[] {
  if (!root) {
    return [];
  }
  const blocks = Array.from(
    root.querySelectorAll<HTMLElement>(BLOCK_SELECTOR),
  );
  return blocks.filter((el) => (el.textContent?.trim().length ?? 0) > 0);
}

function scoreTextMatch(needle: string, haystack: string): number {
  if (!needle || !haystack) {
    return 0;
  }
  if (haystack === needle) {
    return 1;
  }
  if (haystack.includes(needle)) {
    return 0.85 + (needle.length / Math.max(haystack.length, 1)) * 0.15;
  }
  if (needle.includes(haystack) && haystack.length >= 12) {
    return 0.7 + (haystack.length / needle.length) * 0.2;
  }
  const minLen = Math.min(needle.length, haystack.length, 48);
  if (minLen >= 10) {
    const prefix = needle.slice(0, minLen);
    if (haystack.includes(prefix)) {
      return 0.55 + (prefix.length / needle.length) * 0.25;
    }
  }
  return 0;
}

function pickBestUnusedBlock(
  needle: string,
  blocks: HTMLElement[],
  used: Set<HTMLElement>,
): { block: HTMLElement; score: number } | null {
  let best: { block: HTMLElement; score: number } | null = null;
  for (const block of blocks) {
    if (used.has(block)) {
      continue;
    }
    const haystack = normalizeLocateText(block.textContent ?? '');
    const score = scoreTextMatch(needle, haystack);
    if (!best || score > best.score) {
      best = { block, score };
    }
  }
  return best && best.score >= 0.5 ? best : null;
}

/**
 * 将后端段落 id 绑定到 docx 预览 DOM（按正文顺序 + 文本相似度，避免纯序号错位）
 */
export function buildParagraphBlockMap(
  paragraphs: LegalContractApi.Paragraph[],
  blocks: HTMLElement[],
): Map<string, HTMLElement> {
  const map = new Map<string, HTMLElement>();
  const used = new Set<HTMLElement>();
  const sorted = sortParagraphs(paragraphs);

  for (const paragraph of sorted) {
    const needle = normalizeLocateText(paragraph.text ?? '');
    if (!needle) {
      continue;
    }
    const match = pickBestUnusedBlock(needle, blocks, used);
    if (match) {
      map.set(paragraph.paragraphId, match.block);
      used.add(match.block);
      match.block.dataset.paragraphId = paragraph.paragraphId;
    }
  }
  return map;
}

export function findBlockForParagraph(
  map: Map<string, HTMLElement>,
  blocks: HTMLElement[],
  paragraphId?: string,
  locateText?: string,
): HTMLElement | null {
  if (paragraphId) {
    const mapped = map.get(paragraphId);
    if (mapped) {
      return mapped;
    }
    const byAttr = blocks.find(
      (el) => el.dataset.paragraphId === paragraphId,
    );
    if (byAttr) {
      return byAttr;
    }
  }
  const needle = normalizeLocateText(locateText ?? '');
  if (!needle) {
    return null;
  }
  let best: HTMLElement | null = null;
  let bestScore = 0;
  for (const block of blocks) {
    const score = scoreTextMatch(
      needle,
      normalizeLocateText(block.textContent ?? ''),
    );
    if (score > bestScore) {
      bestScore = score;
      best = block;
    }
  }
  return bestScore >= 0.5 ? best : null;
}
