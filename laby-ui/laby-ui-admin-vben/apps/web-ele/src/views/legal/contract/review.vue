<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { ContentWrap, Page } from '@vben/common-ui';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  ElAlert,
  ElButton,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElLink,
  ElMessage,
  ElOption,
  ElSelect,
  ElTabPane,
  ElTabs,
  ElTag,
} from 'element-plus';

import { ACTION_ICON, TableAction } from '#/adapter/vxe-table';

import { getApprovalDetail } from '#/api/bpm/processInstance';
import { approveTask } from '#/api/bpm/task';
import {
  adoptOpinion,
  applyRiskAnnotations,
  batchAdoptOpinions,
  completeOpinionReview,
  createManualOpinion,
  downloadContractFile,
  exportAdoptedContractDocx,
  exportAnnotatedContractDocx,
  exportArchiveZip,
  exportDeliveryBundle,
  exportReportDocx,
  getContract,
  getContractVersionList,
  getOpinionList,
  getParagraphList,
  getWorkbench,
  ignoreOpinion,
  OPINION_STATUS,
  precheckAdoptedExport,
  repairWorkingVersion,
  revokeOpinion,
  startContractFirstAudit,
} from '#/api/legal/contract';
import AuditReportPanel from './components/audit-report-panel.vue';
import ContractAuditProgressPanel from './components/contract-audit-progress-panel.vue';
import ContractChatPanel from './components/contract-chat-panel.vue';
import ContractStatusTimeline from './components/contract-status-timeline.vue';
import OpinionListPanel from './components/opinion-list-panel.vue';
import ConfigOverviewPanel from '../contract-type/modules/config-overview-panel.vue';
import ContractWorkbench from './workbench/ContractWorkbench.vue';
import ContractWorkbenchDownloads from './workbench/ContractWorkbenchDownloads.vue';
import {
  CONTRACT_STATUS,
  FEEDBACK_SUMMARY_MIN_LENGTH,
  formatContractTaskKeyLabel,
  LEGAL_RISK_LEVEL,
} from './constants';
import { useContractDeliverables } from './composables/use-contract-deliverables';
import { loadAuditReportMarkdown } from './utils/load-audit-report';

const riskLevelOptions = getDictOptions(DICT_TYPE.LEGAL_RISK_LEVEL, 'string');

defineOptions({ name: 'LegalContractReview' });

const props = defineProps<{
  /** BPM 业务表单嵌入时传入，值为合同 id */
  id?: string;
  /** 列表「详情」等只读入口 */
  readonly?: boolean;
}>();

const route = useRoute();
const router = useRouter();
const contractId = computed(() => {
  const raw = props.id ?? route.query.id;
  if (raw === undefined || raw === null || raw === '') {
    return 0;
  }
  const id = Number(raw);
  return Number.isFinite(id) ? id : 0;
});
const embeddedInBpm = computed(() => !!props.id);
/** 仅在显式 readonly 参数下只读；详情页默认也允许交互 */
const readonlyMode = computed(() => {
  if (props.readonly) {
    return true;
  }
  const q = route.query.readonly;
  return q === '1' || q === 'true';
});
const contract = ref<LegalContractApi.Contract>();
const opinions = ref<LegalContractApi.Opinion[]>([]);
const {
  auditDeliverablesReady,
  canDownloadOriginal,
  canExportArchive,
  canExportDeliveryBundle,
  canExportReport,
} = useContractDeliverables(contract, opinions);
const riskFilter = ref<string>('');
const statusFilter = ref<string>('');
const sourceFilter = ref<string>('');
const needSecondRound = ref(false);
const feedbackSummary = ref('');
const loading = ref(false);
const actionLoading = ref(false);
const startAuditLoading = ref(false);
/** 已提交二轮审核、等待进度条出现前的过渡态 */
const secondRoundAuditingPending = ref(false);
const annotateLoading = ref(false);
const auditReportMarkdown = ref('');
const reportEmptyHint = ref('');
const paragraphs = ref<LegalContractApi.Paragraph[]>([]);
const navigationMode = ref<'CLAUSE' | 'PARAGRAPH'>('PARAGRAPH');
const navigationNodes = ref<LegalContractApi.WorkbenchNavigationNode[]>([]);
const activeParagraphId = ref<string>();
const activeNavigationId = ref<string>();
const highlightParagraphIds = ref<string[]>([]);
/** docx 定位时用于文本匹配的原文（意见 oldText 或段落全文） */
const activeLocateText = ref<string>();
/** 重复点击定位时递增，避免同一段落 watch 不触发 */
const locateNonce = ref(0);
/** WORKING 文档 revision，采纳后刷新 OnlyOffice */
const documentRevision = ref<string>();
/** 采纳后递增，强制 OnlyOffice 重载 WORKING 正文 */
const documentReloadKey = ref(0);

function applyDocumentRevision(result?: {
  documentRevision?: string;
  documentUpdated?: boolean;
}) {
  if (result?.documentRevision) {
    documentRevision.value = result.documentRevision;
    documentReloadKey.value += 1;
  } else if (result?.documentUpdated) {
    documentReloadKey.value += 1;
  }
  return Boolean(result?.documentUpdated);
}

async function syncDocumentRevision(revision?: string) {
  if (!revision || revision === documentRevision.value) {
    return;
  }
  documentRevision.value = revision;
  await refreshVersions();
}

async function refreshVersions() {
  if (!contractId.value) {
    return;
  }
  contractVersions.value = await getContractVersionList(contractId.value);
}

async function syncWorkingBeforeDownload() {
  const saved = await workbenchRef.value?.forceSaveWorking?.();
  await refreshVersions();
  return saved !== false;
}

const workbenchRef = ref<InstanceType<typeof ContractWorkbench>>();

const mainContractFileId = computed(() => {
  const files = contract.value?.files;
  if (!files?.length) {
    return undefined;
  }
  const main = files.find((f) => f.mainFlag);
  return main?.fileId ?? files[0]?.fileId;
});
const parsePartialHint = computed(() => {
  if (contract.value?.parseStatus !== 4) {
    return '';
  }
  if (contract.value.sourceFormat === 'DOC') {
    return 'DOC 格式需 LibreOffice 转换后解析；请启用 laby.legal.format-convert 或上传 docx。';
  }
  return '合同结构化解析不完整，请优先使用段落视图核对。';
});
const exportLoading = ref(false);
const repairAnchorLoading = ref(false);
const exportVisibility = ref<'EXTERNAL' | 'INTERNAL'>('INTERNAL');
const precheckDialogVisible = ref(false);
const precheckLoading = ref(false);
const precheckResult = ref<{
  adoptedCount: number;
  autoWritableCount: number;
  conflictCount: number;
  manualConfirmCount: number;
  anchorMissingCount?: number;
  anchorOrphanCount?: number;
  missingParagraphIds?: string[];
  orphanBookmarkNames?: string[];
}>();
const pendingExportMode = ref<'CLEAN' | 'TRACKED'>('CLEAN');
const contractVersions = ref<
  Array<{
    id: number;
    versionNo: number;
    type: string;
    visibility: 'EXTERNAL' | 'INTERNAL';
    fileId: number;
    createTime?: string;
  }>
>([]);
const activeTab = ref('workbench');
const workbenchSideTab = ref<'chat' | 'download' | 'opinions' | 'report'>(
  'chat',
);

const OPINION_STATUS_FILTER_OPTIONS = [
  { label: '全部状态', value: '' },
  { label: '待处置', value: String(OPINION_STATUS.PENDING) },
  { label: '已采纳', value: String(OPINION_STATUS.ADOPTED) },
  { label: '已忽略', value: String(OPINION_STATUS.IGNORED) },
];

const OPINION_SOURCE_FILTER_OPTIONS = [
  { label: '全部来源', value: '' },
  { label: 'AI 审核', value: 'AI' },
  { label: '审核规则', value: 'RULE' },
  { label: '标准条款', value: 'STANDARD_CLAUSE' },
  { label: '知识库', value: 'KNOWLEDGE' },
  { label: '人工', value: 'MANUAL' },
];

const contractMainFile = computed(() => {
  const files = contract.value?.files;
  if (!files?.length) {
    return undefined;
  }
  return files.find((item) => item.mainFlag) ?? files[0];
});

const supplementaryUploadFiles = computed(() =>
  (contract.value?.files ?? []).filter((item) => !item.mainFlag),
);

const manualDialogVisible = ref(false);
const manualForm = ref({
  title: '',
  content: '',
  riskLevel: LEGAL_RISK_LEVEL.MEDIUM,
  suggestion: '',
});

const filteredOpinions = computed(() => {
  let list = opinions.value;
  if (riskFilter.value) {
    list = list.filter(
      (item) =>
        item.riskLevel?.toUpperCase() === riskFilter.value.toUpperCase(),
    );
  }
  if (statusFilter.value !== '') {
    const status = Number(statusFilter.value);
    list = list.filter((item) => item.status === status);
  }
  if (sourceFilter.value) {
    list = list.filter(
      (item) =>
        (item.sourceType ?? 'AI').toUpperCase() ===
        sourceFilter.value.toUpperCase(),
    );
  }
  return list;
});

const highRiskPendingCount = computed(
  () =>
    opinions.value.filter(
      (item) =>
        item.riskLevel?.toUpperCase() === LEGAL_RISK_LEVEL.HIGH &&
        item.status === OPINION_STATUS.PENDING,
    ).length,
);

const suggestSecondRound = computed(() => highRiskPendingCount.value > 0);

const pendingCount = computed(
  () =>
    opinions.value.filter((item) => item.status === OPINION_STATUS.PENDING)
      .length,
);

const pageTitle = computed(() =>
  route.name === 'LegalContractDetail' ? '合同详情' : '审核工作台',
);

/** 是否可处置意见（与后端 opinionEditable 一致；详情页强制只读） */
const opinionEditable = computed(
  () => !readonlyMode.value && contract.value?.opinionEditable === true,
);

/** 是否可申请二轮 AI */
const secondRoundApplicable = computed(
  () => contract.value?.secondRoundApplicable === true,
);

/** 是否可保存处置 / 完成复核 */
const opinionCompletable = computed(
  () => !readonlyMode.value && contract.value?.opinionCompletable === true,
);

const aiAuditing = computed(() => {
  const s = contract.value?.status;
  return (
    s === CONTRACT_STATUS.AI_AUDITING || s === CONTRACT_STATUS.AI_REAUDITING
  );
});

const showAuditProgress = computed(
  () => aiAuditing.value || secondRoundAuditingPending.value,
);

const auditProgressRound = computed(
  () =>
    contract.value?.auditRound ??
    (secondRoundAuditingPending.value ? 2 : 1),
);

const opinionActionBlocked = computed(() => showAuditProgress.value);

const opinionEditableEffective = computed(
  () => opinionEditable.value && !opinionActionBlocked.value,
);

const opinionCompletableEffective = computed(
  () => opinionCompletable.value && !opinionActionBlocked.value,
);

const startAuditVisible = computed(
  () => contract.value?.startAuditVisible === true,
);

const isArchived = computed(
  () => contract.value?.status === CONTRACT_STATUS.ARCHIVED,
);

const isSecondAuditRound = computed(() => (contract.value?.auditRound ?? 1) >= 2);

const exportRoundLabel = computed(() =>
  isSecondAuditRound.value ? '二审' : '首轮',
);

const auditVersionLabel = computed(() =>
  isSecondAuditRound.value ? '二审' : '一审',
);

/** 审阅中：页面内 OnlyOffice 编辑 + 采纳，头部不堆导出/下载 */
const reviewFocusMode = computed(
  () => opinionEditable.value && !readonlyMode.value && !isArchived.value,
);

const pageBind = computed(() =>
  embeddedInBpm.value
    ? { class: 'm-2' }
    : {
        autoContentHeight: true,
        contentClass: reviewFocusMode.value ? 'legal-review-page-focus' : '',
      },
);

const cardBodyStyle = computed(() => ({
  flex: 1,
  overflow: 'hidden',
  paddingTop: reviewFocusMode.value ? '8px' : '12px',
  paddingBottom: reviewFocusMode.value ? '8px' : '12px',
  paddingLeft: '12px',
  paddingRight: '12px',
  display: 'flex',
  flexDirection: 'column',
  minHeight: 0,
}));

function notifyWorkbenchResize() {
  void nextTick(() => {
    window.setTimeout(() => window.dispatchEvent(new Event('resize')), 80);
    window.setTimeout(() => window.dispatchEvent(new Event('resize')), 320);
  });
}

watch(reviewFocusMode, () => {
  notifyWorkbenchResize();
});

watch(suggestSecondRound, (suggest) => {
  if (suggest && !needSecondRound.value) {
    needSecondRound.value = true;
    return;
  }
  // 高风险已清零时，自动回收二轮勾选，避免提交时误触发「反馈说明必填」
  if (!suggest && needSecondRound.value) {
    needSecondRound.value = false;
    feedbackSummary.value = '';
  }
});

async function load(options?: { silent?: boolean }) {
  if (!contractId.value) {
    return;
  }
  if (!options?.silent) {
    loading.value = true;
  }
  const requestConfig = showAuditProgress.value
    ? { timeout: 30_000 }
    : undefined;
  try {
    try {
      const workbench = await getWorkbench(contractId.value, requestConfig);
      contract.value = workbench.contract;
      opinions.value = workbench.opinions ?? [];
      paragraphs.value = workbench.paragraphs ?? [];
      navigationMode.value = workbench.navigationMode ?? 'PARAGRAPH';
      navigationNodes.value = workbench.navigationNodes ?? [];
    } catch {
      contract.value = await getContract(contractId.value, requestConfig);
      opinions.value = await getOpinionList(contractId.value);
      paragraphs.value = await getParagraphList(contractId.value);
      navigationMode.value = 'PARAGRAPH';
      navigationNodes.value = paragraphs.value.map((p) => ({
        id: p.paragraphId,
        label: p.text.slice(0, 32) || p.paragraphId,
        level: 0,
        paragraphIds: [p.paragraphId],
      }));
    }
    if (
      contract.value?.status === CONTRACT_STATUS.AI_REAUDITING ||
      contract.value?.status === CONTRACT_STATUS.AI_AUDITING
    ) {
      secondRoundAuditingPending.value = false;
    }
    contractVersions.value = await getContractVersionList(contractId.value);
    const report = await loadAuditReportMarkdown(contractId.value, contract.value);
    auditReportMarkdown.value = report.content;
    reportEmptyHint.value = report.emptyHint;
    if (suggestSecondRound.value && opinionCompletableEffective.value) {
      needSecondRound.value = true;
    }
  } finally {
    if (!options?.silent) {
      loading.value = false;
    }
  }
}

function applyOptimisticSecondRoundState() {
  if (!contract.value) {
    return;
  }
  contract.value = {
    ...contract.value,
    status: CONTRACT_STATUS.AI_REAUDITING,
    statusName: 'AI 二轮审核中',
    currentTaskKey: 'aiRound2',
    auditRound: 2,
    opinionEditable: false,
    secondRoundApplicable: false,
    opinionCompletable: false,
  };
  secondRoundAuditingPending.value = true;
  needSecondRound.value = false;
  feedbackSummary.value = '';
}

async function handleAuditProgressCompleted() {
  secondRoundAuditingPending.value = false;
  statusFilter.value = String(OPINION_STATUS.PENDING);
  ElMessage.success('二轮 AI 审核已完成，请先采纳或忽略新意见，再点击「完成二轮复核」');
  await load();
}

const lateStageOpinionRecovery = computed(
  () =>
    opinionEditable.value &&
    !opinionCompletable.value &&
    pendingCount.value > 0 &&
    (contract.value?.currentTaskKey === 'finalize' ||
      contract.value?.currentTaskKey === 'directorReview' ||
      contract.value?.status === CONTRACT_STATUS.FINALIZING),
);

function handleLocateParagraph(paragraphId: string, locateText?: string) {
  if (!paragraphId?.trim()) {
    ElMessage.warning('该意见未关联段落，无法在正文中定位');
    return;
  }
  activeTab.value = 'workbench';
  activeParagraphId.value = paragraphId;
  activeLocateText.value =
    locateText?.trim() ||
    paragraphs.value.find((p) => p.paragraphId === paragraphId)?.text;
  activeNavigationId.value = resolveNavigationId(paragraphId);
  highlightParagraphIds.value = [paragraphId];
  locateNonce.value += 1;
}

function resolveNavigationId(paragraphId: string) {
  const clauseNode = navigationNodes.value.find((node) =>
    node.paragraphIds?.includes(paragraphId),
  );
  return clauseNode?.id ?? paragraphId;
}

function handleWorkbenchNavigationSelect(
  node: LegalContractApi.WorkbenchNavigationNode,
) {
  activeNavigationId.value = node.id;
  const firstParagraphId = node.paragraphIds?.[0];
  if (firstParagraphId) {
    activeParagraphId.value = firstParagraphId;
    highlightParagraphIds.value = [...(node.paragraphIds ?? [])];
  }
}

function handleWorkbenchParagraphSelect(paragraphId: string) {
  activeParagraphId.value = paragraphId;
  activeLocateText.value = paragraphs.value.find(
    (p) => p.paragraphId === paragraphId,
  )?.text;
  activeNavigationId.value = resolveNavigationId(paragraphId);
  highlightParagraphIds.value = [paragraphId];
  locateNonce.value += 1;
}

async function handleExportReport() {
  if (!contractId.value) {
    return;
  }
  exportLoading.value = true;
  try {
    const fileId = await exportReportDocx(contractId.value);
    await load();
    const exported = contract.value?.files?.find((f) => f.fileId === fileId);
    if (exported) {
      await handleDownloadFile(exported.fileId, exported.fileName);
      ElMessage.success('Word 报告已下载');
    } else {
      ElMessage.success('Word 报告已生成，可在「基本信息」附件中下载');
    }
  } finally {
    exportLoading.value = false;
  }
}

async function handleExportAnnotated() {
  if (!contractId.value) {
    return;
  }
  exportLoading.value = true;
  try {
    const fileId = await exportAnnotatedContractDocx(
      contractId.value,
      exportVisibility.value,
    );
    await load();
    const exported = contract.value?.files?.find((f) => f.fileId === fileId);
    if (exported) {
      await handleDownloadFile(exported.fileId, exported.fileName);
    }
    ElMessage.success('标注版合同已导出');
  } finally {
    exportLoading.value = false;
  }
}

async function handleDownloadOriginal() {
  const fileId = mainContractFileId.value;
  if (!fileId) {
    ElMessage.warning('暂无合同主文件');
    return;
  }
  const name =
    contract.value?.originalFileName ||
    contract.value?.files?.find((f) => f.fileId === fileId)?.fileName ||
    'contract';
  await handleDownloadFile(fileId, name);
}

async function openAdoptedExportPrecheck(mode: 'CLEAN' | 'TRACKED') {
  if (!contractId.value) {
    return;
  }
  pendingExportMode.value = mode;
  precheckLoading.value = true;
  precheckDialogVisible.value = true;
  try {
    precheckResult.value = await precheckAdoptedExport(contractId.value);
  } finally {
    precheckLoading.value = false;
  }
}

async function confirmExportAdopted() {
  if (!contractId.value) {
    return;
  }
  exportLoading.value = true;
  try {
    const fileId = await exportAdoptedContractDocx(
      contractId.value,
      pendingExportMode.value,
      exportVisibility.value,
    );
    precheckDialogVisible.value = false;
    await load();
    const exported = contract.value?.files?.find((f) => f.fileId === fileId);
    if (exported) {
      await handleDownloadFile(exported.fileId, exported.fileName);
    }
    ElMessage.success(
      pendingExportMode.value === 'TRACKED'
        ? '采纳版（带修订）合同已导出'
        : '采纳版（干净）合同已导出',
    );
  } finally {
    exportLoading.value = false;
  }
}

async function handleRepairWorkingAnchors() {
  if (!contractId.value) {
    return;
  }
  repairAnchorLoading.value = true;
  try {
    await repairWorkingVersion(contractId.value);
    precheckResult.value = await precheckAdoptedExport(contractId.value);
    await load();
    ElMessage.success('WORKING 版段落锚点已修复，请重新预检后导出');
  } finally {
    repairAnchorLoading.value = false;
  }
}

async function handleExportArchiveZip() {
  if (!contractId.value) {
    return;
  }
  exportLoading.value = true;
  try {
    const fileId = await exportArchiveZip(contractId.value);
    await load();
    const exported = contract.value?.files?.find((f) => f.fileId === fileId);
    if (exported) {
      await handleDownloadFile(exported.fileId, exported.fileName);
    }
    ElMessage.success('归档包已生成');
  } finally {
    exportLoading.value = false;
  }
}

async function handleExportDeliveryBundle() {
  if (!contractId.value) {
    return;
  }
  exportLoading.value = true;
  try {
    const fileId = await exportDeliveryBundle(contractId.value);
    await load();
    const exported = contract.value?.files?.find((f) => f.fileId === fileId);
    if (exported) {
      await handleDownloadFile(exported.fileId, exported.fileName);
      ElMessage.success('交付包已下载');
    } else {
      ElMessage.success('交付包已生成，请在附件列表下载');
    }
  } finally {
    exportLoading.value = false;
  }
}

async function handleDownloadFile(fileId: number, fileName: string) {
  const data = await downloadContractFile(fileId);
  downloadFileFromBlobPart({ fileName, source: data });
}

function isRequestTimeoutError(error: unknown): boolean {
  if (!error || typeof error !== 'object') {
    return false;
  }
  const err = error as { code?: string; message?: string };
  const message = String(err.message ?? '');
  return (
    err.code === 'ECONNABORTED' ||
    message.includes('timeout') ||
    message.includes('超时')
  );
}

type BpmApproveResult = false | 'timeout' | true;

async function tryApproveBpmTask(): Promise<BpmApproveResult> {
  const c = contract.value;
  if (!c?.processInstanceId) {
    return false;
  }
  let taskId = route.query.taskId as string | undefined;
  if (!taskId) {
    const detail = await getApprovalDetail({
      processInstanceId: c.processInstanceId,
    });
    taskId = detail.todoTask?.id;
  }
  if (!taskId) {
    return false;
  }
  const pendingHigh = opinions.value.filter(
    (item) =>
      item.riskLevel?.toUpperCase() === LEGAL_RISK_LEVEL.HIGH &&
      item.status === OPINION_STATUS.PENDING,
  ).length;
  try {
    await approveTask(
      {
        id: taskId,
        reason: '意见处置完成',
        variables: {
          needSecondRound: needSecondRound.value,
          riskHighCount: pendingHigh,
        },
      },
      { timeout: needSecondRound.value ? 60_000 : 30_000 },
    );
    return true;
  } catch (error) {
    if (isRequestTimeoutError(error)) {
      return 'timeout';
    }
    throw error;
  }
}

async function handleAdopt(id: number) {
  actionLoading.value = true;
  try {
    const result = await adoptOpinion(id);
    const docUpdated = applyDocumentRevision(result);
    ElMessage.success(
      docUpdated
        ? '已采纳，修改后正文已写入工作版合同'
        : '已采纳（无改后正文，未自动修改合同，请在编辑器中手工处理）',
    );
    await load();
    await refreshVersions();
  } finally {
    actionLoading.value = false;
  }
}

async function handleIgnore(id: number) {
  actionLoading.value = true;
  try {
    await ignoreOpinion(id);
    ElMessage.success('已忽略');
    await load();
    await refreshVersions();
  } finally {
    actionLoading.value = false;
  }
}

async function handleRevoke(id: number) {
  actionLoading.value = true;
  try {
    const result = await revokeOpinion(id);
    const docUpdated = applyDocumentRevision(result);
    ElMessage.success(
      docUpdated
        ? '已撤销，意见恢复为待处置，合同正文已同步回滚'
        : '已撤销，意见恢复为待处置',
    );
    await load();
    await refreshVersions();
  } finally {
    actionLoading.value = false;
  }
}

function goBpmProcess() {
  const pid = contract.value?.processInstanceId;
  if (!pid) {
    return;
  }
  router.push({
    name: 'BpmProcessInstanceDetail',
    query: { id: pid },
  });
}

async function batchAdoptPending() {
  const ids = filteredOpinions.value
    .filter((item) => item.status === OPINION_STATUS.PENDING)
    .map((item) => item.id);
  if (ids.length === 0) {
    ElMessage.warning('当前筛选下没有待处置意见');
    return;
  }
  actionLoading.value = true;
  try {
    const result = await batchAdoptOpinions(ids);
    const docUpdated = applyDocumentRevision(result);
    ElMessage.success(
      docUpdated
        ? `已批量采纳 ${ids.length} 条意见，改后正文已写入工作版合同，请继续点击「保存处置结果」完成本轮审核提交`
        : `已批量采纳 ${ids.length} 条意见（部分无改后正文，未自动改正文），请继续点击「保存处置结果」并在编辑器中手工处理`,
    );
    await load();
    await refreshVersions();
  } finally {
    actionLoading.value = false;
  }
}

const canApplyRiskAnnotations = computed(
  () => opinionEditable.value && opinions.value.length > 0,
);

async function handleApplyRiskAnnotations() {
  if (!contractId.value) {
    return;
  }
  if (opinions.value.length === 0) {
    ElMessage.warning('暂无审核意见，无法添加标注');
    return;
  }
  annotateLoading.value = true;
  try {
    const result = await applyRiskAnnotations(contractId.value);
    const docUpdated = applyDocumentRevision(result);
    if (docUpdated) {
      activeTab.value = 'workbench';
      ElMessage.success(
        `已为 ${opinions.value.length} 条风险意见添加文档标注，请在左侧 OnlyOffice 中查看批注`,
      );
    } else {
      ElMessage.info('标注已是最新，无需重复写入');
    }
    await load();
    await refreshVersions();
  } finally {
    annotateLoading.value = false;
  }
}

async function submitManualOpinion() {
  if (!manualForm.value.title?.trim() || !manualForm.value.content?.trim()) {
    ElMessage.warning('请填写意见标题与内容');
    return;
  }
  actionLoading.value = true;
  try {
    await createManualOpinion({
      contractId: contractId.value,
      title: manualForm.value.title.trim(),
      content: manualForm.value.content.trim(),
      riskLevel: manualForm.value.riskLevel,
      suggestion: manualForm.value.suggestion?.trim() || undefined,
    });
    ElMessage.success('已添加人工意见');
    manualDialogVisible.value = false;
    manualForm.value = {
      title: '',
      content: '',
      riskLevel: LEGAL_RISK_LEVEL.MEDIUM,
      suggestion: '',
    };
    await load();
  } finally {
    actionLoading.value = false;
  }
}

async function submitOpinion() {
  if (secondRoundApplicable.value && needSecondRound.value) {
    const summary = feedbackSummary.value?.trim() || '';
    if (summary.length < FEEDBACK_SUMMARY_MIN_LENGTH) {
      ElMessage.warning(
        `申请二轮 AI 审核时，反馈说明至少 ${FEEDBACK_SUMMARY_MIN_LENGTH} 字`,
      );
      return;
    }
  }
  const goingSecondRound =
    secondRoundApplicable.value && needSecondRound.value;
  loading.value = true;
  try {
    await completeOpinionReview(
      {
        contractId: contractId.value,
        needSecondRound: needSecondRound.value,
        feedbackSummary: feedbackSummary.value?.trim() || undefined,
      },
      goingSecondRound ? { timeout: 120_000 } : undefined,
    );

    if (goingSecondRound) {
      applyOptimisticSecondRoundState();
      const approveResult = embeddedInBpm.value
        ? false
        : await tryApproveBpmTask();
      if (approveResult === true || approveResult === 'timeout') {
        ElMessage.success('已提交二轮 AI 审核，请等待进度完成后继续复核');
      } else if (embeddedInBpm.value) {
        ElMessage.success(
          '处置已保存，二轮 AI 审核已启动；请使用页面底部流程按钮提交审批',
        );
      } else {
        ElMessage.warning(
          '二轮 AI 审核已启动；请在「工作流 → 我的待办」中审批以推进流程',
        );
      }
      return;
    }

    await load();
    const approveResult = embeddedInBpm.value
      ? false
      : await tryApproveBpmTask();
    if (approveResult === true) {
      ElMessage.success('意见处置已保存，流程任务已自动通过');
      await load();
    } else if (approveResult === 'timeout') {
      ElMessage.warning(
        '意见处置已保存；流程审批处理中，请稍后刷新页面或到「我的待办」查看',
      );
    } else if (embeddedInBpm.value) {
      ElMessage.success('意见处置已保存，请使用页面底部流程按钮提交审批');
    } else {
      ElMessage.success(
        '意见处置已保存；请在「工作流 → 我的待办」中审批以推进流程',
      );
    }
  } finally {
    loading.value = false;
  }
}

function goBpmTodo() {
  router.push({ name: 'BpmTodoTask' });
}

async function handleStartFirstAudit() {
  if (!contractId.value) {
    return;
  }
  startAuditLoading.value = true;
  try {
    await startContractFirstAudit(contractId.value);
    ElMessage.success('已提交首轮 AI 审核，请稍候');
    await load();
  } finally {
    startAuditLoading.value = false;
  }
}

const orchestrationChatLinked = computed(
  () =>
    contract.value?.createSource === 'AI_CHAT' &&
    !!contract.value?.createConversationId,
);

function goOrchestrationChat() {
  const conversationId = contract.value?.createConversationId;
  if (!conversationId) {
    return;
  }
  router.push({
    path: '/ai/chat',
    query: { conversationId: String(conversationId) },
  });
}

function goBackList() {
  router.push({ name: 'LegalContractList' });
}

function goHandleContract() {
  const c = contract.value;
  if (!c) {
    return;
  }
  if (c.processInstanceId) {
    router.push({
      name: 'BpmProcessInstanceDetail',
      query: { id: c.processInstanceId },
    });
    return;
  }
  router.push({
    name: 'LegalContractReview',
    query: { id: String(c.id) },
  });
}

watch(
  () => [props.id, route.query.id] as const,
  () => {
    load();
  },
);

onMounted(async () => {
  await load();
  notifyWorkbenchResize();
});

watch(activeTab, (tab) => {
  if (tab === 'workbench') {
    notifyWorkbenchResize();
  }
  if (tab === 'download' || tab === 'report') {
    void refreshVersions();
  }
});
</script>

<template>
  <component :is="embeddedInBpm ? ContentWrap : Page" v-bind="pageBind">
    <ElEmpty
      v-if="!contractId"
      class="py-16"
      description="缺少合同编号，请从「我的合同」列表点击「详情」进入"
    />

    <ElCard
      v-else
      v-loading="loading"
      class="legal-contract-review-card legal-contract-review-card--flex flex h-full min-h-0 flex-col"
      :class="{ 'legal-contract-review-card--focus': reviewFocusMode }"
      :body-style="cardBodyStyle"
    >
      <ContractAuditProgressPanel
        v-if="contractId && !reviewFocusMode && showAuditProgress"
        :contract-id="contractId"
        :active="showAuditProgress"
        :audit-round="auditProgressRound"
        @completed="handleAuditProgressCompleted"
      />

      <template v-if="!embeddedInBpm && !reviewFocusMode" #header>
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div class="flex min-w-0 flex-col gap-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="truncate text-lg font-semibold">
                {{ contract?.title || pageTitle }}
              </span>
              <ElTag v-if="contract?.statusName" size="small" effect="light">
                {{ contract.statusName }}
              </ElTag>
              <ElTag v-if="readonlyMode" size="small" type="info" effect="plain">
                只读
              </ElTag>
            </div>
            <div class="text-xs text-muted-foreground">
              {{ pageTitle }} · 合同 #{{ contractId }}
              <template v-if="contract?.currentTaskKey">
                · 节点
                {{
                  formatContractTaskKeyLabel(
                    contract.currentTaskKey,
                    contract.status,
                  )
                }}
              </template>
            </div>
          </div>
          <TableAction
            :actions="[
              {
                label: '返回列表',
                icon: ACTION_ICON.CLOSE,
                onClick: goBackList,
              },
              {
                label: '我的待办',
                icon: ACTION_ICON.BOOK,
                ifShow: !readonlyMode && opinionEditable,
                onClick: goBpmTodo,
              },
              {
                label: '导出 Word 报告',
                icon: ACTION_ICON.DOWNLOAD,
                ifShow: !reviewFocusMode && canExportReport,
                loading: exportLoading,
                onClick: handleExportReport,
              },
              {
                label: '下载合同原件',
                icon: ACTION_ICON.DOWNLOAD,
                ifShow: !reviewFocusMode && canDownloadOriginal && !!mainContractFileId,
                onClick: handleDownloadOriginal,
              },
              {
                label: '四件套下载',
                icon: ACTION_ICON.DOWNLOAD,
                ifShow: !reviewFocusMode && auditDeliverablesReady,
                onClick: () => {
                  activeTab.value = 'download';
                },
              },
              {
                label: '下载归档包',
                icon: ACTION_ICON.DOWNLOAD,
                ifShow: canExportArchive,
                loading: exportLoading,
                onClick: handleExportArchiveZip,
              },
              {
                label: '导出交付包',
                icon: ACTION_ICON.DOWNLOAD,
                ifShow: !reviewFocusMode && canExportDeliveryBundle,
                loading: exportLoading,
                onClick: handleExportDeliveryBundle,
              },
              {
                label: '开始 AI 审核',
                type: 'primary',
                icon: ACTION_ICON.EDIT,
                ifShow: !readonlyMode && startAuditVisible,
                loading: startAuditLoading,
                onClick: handleStartFirstAudit,
              },
              {
                label: contract?.processInstanceId ? '办理流程' : '进入审核',
                type: 'primary',
                icon: ACTION_ICON.EDIT,
                ifShow: readonlyMode && contract?.reviewActionVisible,
                onClick: goHandleContract,
              },
            ]"
          />
        </div>
      </template>

      <div v-if="!reviewFocusMode" class="mb-3 flex shrink-0 flex-col gap-2">
        <ElAlert
          v-if="startAuditVisible"
          type="warning"
          show-icon
          :closable="false"
        >
          <template #title>
            合同已解析但未完成首轮 AI 审核（可能为异常中断）。请点击
            <strong>「开始 AI 审核」</strong>
            补发。
          </template>
        </ElAlert>
        <ElAlert
          v-if="orchestrationChatLinked"
          type="success"
          show-icon
          :closable="false"
        >
          <template #title>
            本合同来自 AI 编排对话（模型/立场/审核强度与对话一致）。
            <ElLink type="primary" @click="goOrchestrationChat">返回编排对话</ElLink>
          </template>
        </ElAlert>
        <ElAlert
          v-if="parsePartialHint"
          type="warning"
          show-icon
          :closable="false"
          title="结构化解析未完整"
          :description="parsePartialHint"
        />
        <ElAlert
          v-if="readonlyMode && !embeddedInBpm"
          type="info"
          show-icon
          :closable="false"
          title="合同详情（只读）"
          description="仅供查看；如需处置意见或办理流程，请从列表进入「审核」或「办理」。"
        />
        <ElAlert
          v-if="!embeddedInBpm && !readonlyMode && opinionEditable"
          type="info"
          show-icon
          :closable="false"
        >
          <template #title>
            业务处置完成后，还需在
            <ElLink type="primary" @click="goBpmTodo">工作流 → 我的待办</ElLink>
            中点击「通过」办理任务，流程才会继续推进。
          </template>
        </ElAlert>
        <ElAlert
          v-if="lateStageOpinionRecovery"
          type="warning"
          show-icon
          :closable="false"
          title="仍有待处置意见，可在此补采纳"
          description="流程已进入后续节点，但检测到未处置意见。请使用右侧「批量采纳」或逐条采纳/忽略；处理完成后请前往「我的待办」继续办理流程。"
        />
      </div>

      <ElTabs v-model="activeTab" class="legal-review-tabs min-h-0 flex-1">
        <ElTabPane name="workbench" class="legal-review-tab-pane">
          <template #label>
            <span class="legal-review-tab-label">
              <IconifyIcon icon="lucide:layout-panel-left" class="size-4 shrink-0" />
              <span>审阅工作台</span>
              <ElTag
                v-if="opinions.length > 0"
                size="small"
                round
                type="primary"
                effect="plain"
              >
                {{ opinions.length }}
              </ElTag>
            </span>
          </template>
          <div class="legal-workbench-tab flex min-h-0 flex-1 flex-col">
            <ContractWorkbench
              ref="workbenchRef"
              v-model:side-tab="workbenchSideTab"
              :contract-id="contractId"
              :source-format="contract?.sourceFormat"
              :main-file-id="mainContractFileId"
              :active-locate-text="activeLocateText"
              :active-locate-paragraph-id="activeParagraphId"
              :locate-nonce="locateNonce"
              :document-revision="documentRevision"
              :pending-opinion-count="pendingCount"
              :total-opinion-count="opinions.length"
              :has-audit-report="!!contract?.hasAuditReport"
              :key="`${contractId}-${documentReloadKey}`"
              @document-revision="syncDocumentRevision"
            >
              <template #opinions>
                <ElAlert
                  v-if="lateStageOpinionRecovery"
                  class="mx-2 mb-2 shrink-0"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="仍有待处置意见，可在此补采纳"
                  description="处理完成后请前往「我的待办」继续办理流程。"
                />
                <OpinionListPanel
                  class="min-h-0 flex-1 px-2 pb-2"
                  embedded
                  v-model:risk-filter="riskFilter"
                  v-model:status-filter="statusFilter"
                  v-model:source-filter="sourceFilter"
                  v-model:need-second-round="needSecondRound"
                  v-model:feedback-summary="feedbackSummary"
                  :status-filter-options="OPINION_STATUS_FILTER_OPTIONS"
                  :source-filter-options="OPINION_SOURCE_FILTER_OPTIONS"
                  :opinions="opinions"
                  :filtered-opinions="filteredOpinions"
                  :display-opinions="filteredOpinions"
                  :pending-count="pendingCount"
                  :can-apply-annotations="canApplyRiskAnnotations"
                  :annotate-loading="annotateLoading"
                  :risk-level-options="riskLevelOptions"
                  :opinion-editable="opinionEditableEffective"
                  :readonly-mode="readonlyMode"
                  :embedded-in-bpm="embeddedInBpm"
                  :action-loading="actionLoading"
                  :loading="loading"
                  :second-round-applicable="secondRoundApplicable"
                  :opinion-completable="opinionCompletableEffective"
                  :ai-auditing="showAuditProgress"
                  :audit-round="auditProgressRound"
                  @adopt="handleAdopt"
                  @ignore="handleIgnore"
                  @revoke="handleRevoke"
                  @locate="handleLocateParagraph"
                  @batch-adopt="batchAdoptPending"
                  @open-manual="manualDialogVisible = true"
                  @apply-annotations="handleApplyRiskAnnotations"
                  @submit="submitOpinion"
                  @go-bpm-todo="goBpmTodo"
                >
                  <template #audit-progress>
                    <ContractAuditProgressPanel
                      v-if="contractId"
                      :contract-id="contractId"
                      :active="showAuditProgress"
                      :audit-round="auditProgressRound"
                      class="!mb-0"
                      @completed="handleAuditProgressCompleted"
                    />
                  </template>
                </OpinionListPanel>
              </template>
              <template #chat>
                <ContractChatPanel
                  class="min-h-0 flex-1"
                  :contract-id="contractId"
                  :disabled="!contract"
                  embedded
                  @contract-changed="load"
                />
              </template>
              <template #report>
                <AuditReportPanel
                  embedded
                  :content="auditReportMarkdown"
                  :empty-hint="reportEmptyHint"
                  :contract-id="contractId || undefined"
                  :contract-title="contract?.title"
                  :audit-round="contract?.auditRound"
                />
              </template>
              <template #download>
                <ContractWorkbenchDownloads
                  :contract-id="contractId"
                  :audit-round="contract?.auditRound"
                  :round-label="auditVersionLabel"
                  :versions="contractVersions"
                  :main-file-id="mainContractFileId"
                  :source-format="contract?.sourceFormat"
                  :document-revision="documentRevision"
                  :audit-deliverables-ready="auditDeliverablesReady"
                  :before-adopted-download="syncWorkingBeforeDownload"
                  @refreshed="refreshVersions"
                />
              </template>
            </ContractWorkbench>
          </div>
        </ElTabPane>

        <ElTabPane name="report" class="legal-review-tab-pane">
          <template #label>
            <span class="legal-review-tab-label">
              <IconifyIcon icon="lucide:file-text" class="size-4 shrink-0" />
              <span>审核报告</span>
              <ElTag
                v-if="contract?.hasAuditReport"
                size="small"
                round
                type="success"
                effect="plain"
              >
                已生成
              </ElTag>
            </span>
          </template>
          <div class="legal-review-tab-body legal-review-tab-body--report">
            <AuditReportPanel
              class="min-h-0 flex-1"
              :content="auditReportMarkdown"
              :empty-hint="reportEmptyHint"
              :contract-id="contractId || undefined"
              :contract-title="contract?.title"
              :audit-round="contract?.auditRound"
            />
          </div>
        </ElTabPane>

        <ElTabPane name="download" class="legal-review-tab-pane">
          <template #label>
            <span class="legal-review-tab-label">
              <IconifyIcon icon="lucide:download" class="size-4 shrink-0" />
              <span>下载</span>
            </span>
          </template>
          <div class="legal-review-tab-body legal-review-tab-body--download">
            <ContractWorkbenchDownloads
              full-page
              :contract-id="contractId"
              :audit-round="contract?.auditRound"
              :round-label="auditVersionLabel"
              :versions="contractVersions"
              :main-file-id="mainContractFileId"
              :source-format="contract?.sourceFormat"
              :document-revision="documentRevision"
              :audit-deliverables-ready="auditDeliverablesReady"
              :before-adopted-download="syncWorkingBeforeDownload"
              @refreshed="refreshVersions"
            />
          </div>
        </ElTabPane>

        <ElTabPane name="chat" class="legal-review-tab-pane">
          <template #label>
            <span class="legal-review-tab-label">
              <IconifyIcon icon="lucide:message-square" class="size-4 shrink-0" />
              <span>合同问答</span>
            </span>
          </template>
          <div class="legal-review-tab-body legal-review-tab-body--chat">
            <ContractChatPanel
              class="min-h-0 flex-1"
              :contract-id="contractId"
              :disabled="!contract"
              @contract-changed="load"
            />
          </div>
        </ElTabPane>

        <ElTabPane name="info" class="legal-review-tab-pane">
          <template #label>
            <span class="legal-review-tab-label">
              <IconifyIcon icon="lucide:info" class="size-4 shrink-0" />
              <span>基本信息</span>
            </span>
          </template>
          <div class="legal-review-tab-body space-y-4">
            <ContractStatusTimeline :contract="contract" />
            <ConfigOverviewPanel
              v-if="contract?.contractTypeId"
              :contract-type-id="contract.contractTypeId"
            />
            <div
              v-if="contract?.processInstanceId"
              class="flex justify-end"
            >
              <ElButton type="primary" link @click="goBpmProcess">
                查看 BPM 流程进度与流程图
              </ElButton>
            </div>
            <ElDescriptions
              v-if="contract"
              :column="2"
              border
              class="legal-contract-desc"
            >
              <ElDescriptionsItem label="合同标题" :span="2">
                {{ contract.title }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="业务状态">
                {{ contract.statusName || contract.status }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="当前节点">
                {{
                  formatContractTaskKeyLabel(
                    contract.currentTaskKey,
                    contract.status,
                  )
                }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="AI 轮次">
                第 {{ contract.auditRound ?? 1 }} 轮
              </ElDescriptionsItem>
              <ElDescriptionsItem label="未处置高风险">
                {{ highRiskPendingCount }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="流程实例" :span="2">
                {{ contract.processInstanceId || '未绑定' }}
              </ElDescriptionsItem>
              <ElDescriptionsItem label="导出可见性">
                <ElSelect v-model="exportVisibility" class="!w-36">
                  <ElOption label="内部版" value="INTERNAL" />
                  <ElOption label="外发版" value="EXTERNAL" />
                </ElSelect>
              </ElDescriptionsItem>
              <ElDescriptionsItem
                v-if="exportVisibility === 'EXTERNAL'"
                label="外发说明"
                :span="2"
              >
                <span class="text-xs text-amber-700 dark:text-amber-200">
                  外发版强制干净文本、去除批注与内部依据，不可导出带修订版。
                </span>
              </ElDescriptionsItem>
              <ElDescriptionsItem label="合同原件">
                <template v-if="contractMainFile">
                  <ElLink
                    type="primary"
                    @click="
                      handleDownloadFile(
                        contractMainFile.fileId,
                        contractMainFile.fileName,
                      )
                    "
                  >
                    {{ contractMainFile.fileName }}
                  </ElLink>
                </template>
                <span v-else class="text-muted-foreground">—</span>
              </ElDescriptionsItem>
              <ElDescriptionsItem label="系统版本">
                <span class="text-sm text-muted-foreground">
                  共 {{ contractVersions.length }} 个（标注/修订/采纳等请前往
                  <ElButton
                    type="primary"
                    link
                    class="!px-0 align-baseline"
                    @click="activeTab = 'download'"
                  >
                    下载
                  </ElButton>
                  Tab）
                </span>
              </ElDescriptionsItem>
              <ElDescriptionsItem
                v-if="supplementaryUploadFiles.length"
                label="补充附件"
                :span="2"
              >
                <div class="flex flex-col gap-1">
                  <ElLink
                    v-for="f in supplementaryUploadFiles"
                    :key="f.fileId"
                    type="primary"
                    @click="handleDownloadFile(f.fileId, f.fileName)"
                  >
                    {{ f.fileName }}
                  </ElLink>
                </div>
              </ElDescriptionsItem>
            </ElDescriptions>
          </div>
        </ElTabPane>
      </ElTabs>
    </ElCard>

    <ElDialog
        v-model="manualDialogVisible"
        title="补充人工意见"
        width="520px"
        destroy-on-close
      >
        <ElForm label-width="90px">
          <ElFormItem label="标题" required>
            <ElInput v-model="manualForm.title" placeholder="意见标题" />
          </ElFormItem>
          <ElFormItem label="风险等级">
            <ElSelect v-model="manualForm.riskLevel" class="w-full">
              <ElOption
                v-for="item in riskLevelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="内容" required>
            <ElInput
              v-model="manualForm.content"
              type="textarea"
              :rows="4"
              placeholder="意见说明"
            />
          </ElFormItem>
          <ElFormItem label="修改建议">
            <ElInput
              v-model="manualForm.suggestion"
              type="textarea"
              :rows="2"
              placeholder="可选"
            />
          </ElFormItem>
        </ElForm>
        <template #footer>
          <ElButton @click="manualDialogVisible = false">取消</ElButton>
          <ElButton
            type="primary"
            :loading="actionLoading"
            @click="submitManualOpinion"
          >
            添加
          </ElButton>
        </template>
    </ElDialog>

    <ElDialog
      v-model="precheckDialogVisible"
      title="导出前预检"
      width="520px"
      destroy-on-close
    >
      <div v-loading="precheckLoading" class="space-y-3">
        <ElAlert
          type="info"
          :closable="false"
          show-icon
          title="将基于采纳意见生成合同版本"
          :description="
            pendingExportMode === 'TRACKED'
              ? '当前将导出带修订留痕版本'
              : '当前将导出干净文本版本'
          "
        />
        <ElDescriptions :column="1" border>
          <ElDescriptionsItem label="采纳意见总数">
            {{ precheckResult?.adoptedCount ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="可自动写回">
            {{ precheckResult?.autoWritableCount ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="冲突条数">
            {{ precheckResult?.conflictCount ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="需人工确认">
            {{ precheckResult?.manualConfirmCount ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="缺失 Bookmark">
            {{ precheckResult?.anchorMissingCount ?? 0 }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="孤立 Bookmark">
            {{ precheckResult?.anchorOrphanCount ?? 0 }}
          </ElDescriptionsItem>
        </ElDescriptions>
        <ElAlert
          v-if="(precheckResult?.anchorMissingCount ?? 0) > 0"
          type="warning"
          show-icon
          :closable="false"
          title="段落锚点不一致"
          :description="`缺失段落：${(precheckResult?.missingParagraphIds ?? []).join('、') || '—'}`"
        />
      </div>
      <template #footer>
        <ElButton @click="precheckDialogVisible = false">取消</ElButton>
        <ElButton
          v-if="(precheckResult?.anchorMissingCount ?? 0) > 0"
          type="warning"
          :loading="repairAnchorLoading"
          @click="handleRepairWorkingAnchors"
        >
          修复锚点
        </ElButton>
        <ElButton
          type="primary"
          :loading="exportLoading"
          @click="confirmExportAdopted"
        >
          确认导出
        </ElButton>
      </template>
    </ElDialog>
  </component>
</template>

<style scoped>
.legal-contract-review-card :deep(.el-card__header) {
  padding-bottom: 12px;
}

.legal-contract-review-card--flex {
  height: 100%;
  overflow: hidden;
}

.legal-contract-review-card--flex :deep(.el-card__body) {
  min-height: 0;
}

.legal-contract-review-card--focus :deep(.el-card__body) {
  padding-inline: 12px;
}

.legal-contract-review-card--focus .legal-review-tabs :deep(.el-tabs__header) {
  margin-bottom: 4px;
}

.legal-review-tabs {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.legal-review-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin-bottom: 8px;
  padding: 0 4px;
}

.legal-review-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legal-review-tab-body {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 8px 12px 16px;
}

.legal-review-tab-body--chat {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding-bottom: 12px;
}

.legal-review-tab-body--report,
.legal-review-tab-body--download {
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  min-height: min(720px, calc(100vh - 200px));
  padding: 12px 16px 16px;
  overflow: auto;
}

.legal-review-tabs :deep(.el-tabs__content) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.legal-review-tabs :deep(.el-tab-pane) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.legal-workbench-tab {
  display: flex;
  flex: 1;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 0 4px 4px;
  box-sizing: border-box;
}
</style>

<style>
.legal-review-page-focus {
  padding: 8px 12px !important;
}
</style>
