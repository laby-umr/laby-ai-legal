import type { LegalContractApi } from '#/api/legal/contract';

import type { ComputedRef, Ref } from 'vue';

import { computed, unref } from 'vue';

import { CONTRACT_STATUS } from '../constants';

/**
 * 合同四件套 / 导出按钮可见性（与 DELIV-001、下载页签一致）
 */
export function useContractDeliverables(
  contract: Ref<LegalContractApi.Contract | undefined>,
  opinions?: Ref<LegalContractApi.Opinion[] | undefined>,
) {
  const isPdfSource = computed(
    () => (contract.value?.sourceFormat ?? '').toUpperCase() === 'PDF',
  );

  const isWordSource = computed(() => !isPdfSource.value);

  /** 解析未完成或失败时不提供衍生件 */
  const parseUsable = computed(() => {
    const c = contract.value;
    if (!c) {
      return false;
    }
    if (c.status === CONTRACT_STATUS.PARSING || c.status === CONTRACT_STATUS.FAILED) {
      return false;
    }
    return true;
  });

  /** 已完成至少一轮 AI 审核，可生成标注/修订/采纳 */
  const auditDeliverablesReady = computed(() => {
    const c = contract.value;
    if (!c || !parseUsable.value) {
      return false;
    }
    if (
      c.status === CONTRACT_STATUS.AI_AUDITING ||
      c.status === CONTRACT_STATUS.AI_REAUDITING
    ) {
      return false;
    }
    if (c.startAuditVisible) {
      return false;
    }
    const opinionCount = (c.auditOpinionCount ?? 0) > 0
      || (unref(opinions)?.length ?? 0) > 0;
    return (
      Boolean(c.hasAuditReport) ||
      opinionCount ||
      (c.status ?? 0) >= CONTRACT_STATUS.OPINION_REVIEW
    );
  });

  const canDownloadOriginal = computed(() => parseUsable.value && Boolean(contract.value));

  const canDownloadAnnotated = computed(() => auditDeliverablesReady.value);

  const canDownloadRevision = computed(
    () => auditDeliverablesReady.value && isWordSource.value,
  );

  const canDownloadAdopted = computed(
    () => auditDeliverablesReady.value && isWordSource.value,
  );

  const canExportReport = computed(() => Boolean(contract.value?.hasAuditReport));

  const canExportDeliveryBundle = computed(
    () =>
      auditDeliverablesReady.value &&
      contract.value?.status !== CONTRACT_STATUS.ARCHIVED,
  );

  const canExportArchive = computed(
    () => contract.value?.status === CONTRACT_STATUS.ARCHIVED,
  );

  return {
    isPdfSource,
    isWordSource,
    parseUsable,
    auditDeliverablesReady,
    canDownloadOriginal,
    canDownloadAnnotated,
    canDownloadRevision,
    canDownloadAdopted,
    canExportReport,
    canExportDeliveryBundle,
    canExportArchive,
  } satisfies Record<string, ComputedRef<boolean>>;
}
