/** 生成 OnlyOffice 内搜索用的多段候选文本（长短、标点归一） */
export function buildOnlyOfficeSearchVariants(text?: string): string[] {
  if (!text?.trim()) {
    return [];
  }
  const normalized = text
    .replace(/[\s\u00a0\u200b\u3000]+/g, ' ')
    .replace(/[“”""''‘’]/g, '"')
    .trim();
  const variants = new Set<string>();
  if (normalized) {
    variants.add(normalized.slice(0, 200));
  }
  const compact = normalized.replace(/\s+/g, '');
  if (compact.length >= 8) {
    variants.add(compact.slice(0, 120));
  }
  const short = normalized.slice(0, 80);
  if (short.length >= 8) {
    variants.add(short);
  }
  const sentence = normalized.split(/[。；;.\n]/)[0]?.trim();
  if (sentence && sentence.length >= 6) {
    variants.add(sentence.slice(0, 120));
  }
  return [...variants].filter((item) => item.length >= 4);
}

export const ONLYOFFICE_LOCATE = {
  PLUGIN_GUID: 'asc.legal-locate-v3',
  BOOKMARK_PREFIX: 'laby_p_',
  MSG_LOCATE: 'legal:locate',
  MSG_READY: 'legal:locate-ready',
  MSG_RESULT: 'legal:locate-result',
} as const;

export function buildParagraphBookmarkName(paragraphId?: string): string | undefined {
  if (!paragraphId?.trim()) {
    return undefined;
  }
  return `${ONLYOFFICE_LOCATE.BOOKMARK_PREFIX}${paragraphId.trim()}`;
}
