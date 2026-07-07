import type { LegalContractApi } from '#/api/legal/contract';

/** 段落展示样式（模拟合同排版，用于纯文本回退视图） */
export type ContractParagraphDisplayStyle =
  | 'body'
  | 'title'
  | 'heading1'
  | 'heading2'
  | 'heading3'
  | 'signature';

const CN_ARTICLE = /^第[一二三四五六七八九十百千]+条/;
const NUM_OUTLINE = /^(\d+(\.\d+)+|\d+[、.．])/;
const NUM_SINGLE = /^(\d+[、.．]|[（(]\d+[）)])/;
const CONTRACT_TITLE = /^(.*合同|.*协议)(书)?$/;

export function resolveParagraphDisplayStyle(
  text: string,
  index: number,
): ContractParagraphDisplayStyle {
  const trimmed = text.trim();
  if (!trimmed) {
    return 'body';
  }
  if (index === 0 && trimmed.length <= 40 && CONTRACT_TITLE.test(trimmed)) {
    return 'title';
  }
  if (CN_ARTICLE.test(trimmed)) {
    return 'heading2';
  }
  if (NUM_OUTLINE.test(trimmed) && trimmed.length <= 80) {
    return 'heading3';
  }
  if (trimmed.length <= 40 && NUM_SINGLE.test(trimmed)) {
    return 'heading3';
  }
  if (
    trimmed.length <= 24 &&
    (trimmed.endsWith('：') || trimmed.endsWith(':'))
  ) {
    return 'heading3';
  }
  if (/^(甲方|乙方|丙方|丁方)[:：]/.test(trimmed) && trimmed.length < 120) {
    return 'signature';
  }
  return 'body';
}

export function sortParagraphs(
  paragraphs: LegalContractApi.Paragraph[],
): LegalContractApi.Paragraph[] {
  return [...paragraphs].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
}

export function findParagraphIndex(
  paragraphs: LegalContractApi.Paragraph[],
  paragraphId?: string,
): number {
  if (!paragraphId) {
    return -1;
  }
  return sortParagraphs(paragraphs).findIndex(
    (item) => item.paragraphId === paragraphId,
  );
}
