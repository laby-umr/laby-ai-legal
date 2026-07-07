import type { LegalContractApi } from '#/api/legal/contract';

/** 意见是否可在采纳时自动写入 WORKING 正文 */
export function isOpinionDocumentWritable(opinion: LegalContractApi.Opinion): boolean {
  const changeType = (opinion.changeType || 'NO_CHANGE').toUpperCase();
  const oldText = opinion.oldText?.trim();
  const newText = opinion.newText?.trim();
  if (changeType === 'DELETE') {
    return !!oldText;
  }
  if (changeType === 'INSERT_BEFORE' || changeType === 'INSERT_AFTER' || changeType === 'REPLACE') {
    return !!newText;
  }
  return !!newText;
}

export function isInsertClauseOpinion(opinion: LegalContractApi.Opinion): boolean {
  const changeType = (opinion.changeType || 'NO_CHANGE').toUpperCase();
  if (changeType === 'INSERT_BEFORE' || changeType === 'INSERT_AFTER') {
    return !!opinion.newText?.trim();
  }
  return !opinion.oldText?.trim() && !!opinion.newText?.trim();
}

export function opinionChangeTypeLabel(changeType?: string): string {
  switch ((changeType || 'NO_CHANGE').toUpperCase()) {
    case 'REPLACE':
      return '替换';
    case 'INSERT_BEFORE':
      return '前插入';
    case 'INSERT_AFTER':
      return '后插入';
    case 'DELETE':
      return '删除';
    default:
      return '仅提示';
  }
}
