import type { LegalContractApi } from '#/api/legal/contract';

import { getAuditReport } from '#/api/legal/contract';

import { CONTRACT_STATUS } from '../constants';

/**
 * 加载 AI 审核报告 Markdown；按轮次查询，无则回退最近一轮。
 */
export async function loadAuditReportMarkdown(
  contractId: number,
  contract?: Pick<
    LegalContractApi.Contract,
    'auditRound' | 'status' | 'auditOpinionCount'
  >,
): Promise<{ content: string; emptyHint: string }> {
  const round = contract?.auditRound ?? 1;
  try {
    const report = await getAuditReport(contractId, round);
    const content = report?.content?.trim() ?? '';
    if (content) {
      return { content, emptyHint: '' };
    }
  } catch {
    /* 接口异常时仍展示说明 */
  }
  return {
    content: '',
    emptyHint: resolveEmptyReportHint(contract?.status, contract?.auditOpinionCount),
  };
}

function resolveEmptyReportHint(status?: number, opinionCount?: number): string {
  if (opinionCount != null && opinionCount > 0) {
    return '已有 AI 审核意见但报告未加载，请点击刷新；若仍无内容，请让管理员执行 legal 数据修复 SQL 后重试。';
  }

  if (status === CONTRACT_STATUS.FAILED) {
    return '处理失败，未生成 AI 审核报告。请查看失败原因后重试。';
  }
  if (
    status === CONTRACT_STATUS.PARSING ||
    status === CONTRACT_STATUS.AI_AUDITING ||
    status === CONTRACT_STATUS.AI_REAUDITING
  ) {
    return '后台解析或 AI 审核进行中，完成后将自动生成报告。';
  }
  if (status === CONTRACT_STATUS.DRAFT) {
    return '合同尚未进入 AI 审核。';
  }
  return '暂无 AI 审核报告（可能审核未完成、未识别到意见，或请刷新后重试）。';
}
