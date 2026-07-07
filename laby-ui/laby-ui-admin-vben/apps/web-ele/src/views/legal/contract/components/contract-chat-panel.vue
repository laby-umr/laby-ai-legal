<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';

import { AiModelTypeEnum } from '@vben/constants';
import { IconifyIcon } from '@vben/icons';
import { useClipboard } from '@vueuse/core';
import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElScrollbar,
  ElSelect,
  ElSwitch,
  ElTag,
} from 'element-plus';

import { getModelSimpleList } from '#/api/ai/model/model';
import {
  cancelAgentProposal,
  clearContractChatMessages,
  confirmAgent,
  deleteContractChatMessage,
  deleteContractChatMessagesFrom,
  executeAgentProposal,
  getContract,
  createContractMemory,
  deleteContractMemory,
  getContractChatMessageList,
  getContractMemoryList,
  sendContractChatStream,
  updateContractMemory,
} from '#/api/legal/contract';

import AiChatAssistantMessage from '#/views/ai/chat/index/modules/message/assistant-message.vue';
import CompactionSummaryMessage from '#/views/ai/chat/index/modules/message/compaction-summary-message.vue';
import AiChatUserMessage from '#/views/ai/chat/index/modules/message/user-message.vue';

import '#/styles/ai-markdown.scss';

import { getContractChatBucket } from '../composables/use-contract-chat-shared';

const props = defineProps<{
  contractId: number;
  disabled?: boolean;
  /** 嵌入审阅工作台侧栏时占满高度 */
  embedded?: boolean;
}>();

const emit = defineEmits<{
  /** 提案执行等写操作成功后，通知父页刷新意见/合同状态 */
  'contract-changed': [];
}>();

interface ToolTrace {
  id: string;
  toolName: string;
  status: 'running' | 'done';
  summary?: string;
}

interface AgentProposal {
  proposalNo: string;
  proposalAction: string;
  proposalTitle: string;
  proposalPayload?: string;
  status: 'pending' | 'executed' | 'cancelled';
}

interface AgentConfirm {
  confirmId: string;
  toolName: string;
  summary: string;
  status: 'pending' | 'approved' | 'denied';
}

interface UiMessage {
  id: string;
  dbId?: number;
  role: 'assistant' | 'user' | 'summary';
  content: string;
  reasoningContent?: string;
  pending?: boolean;
  toolTraces?: ToolTrace[];
  proposals?: AgentProposal[];
  pendingConfirm?: AgentConfirm;
}

const input = ref('');
const messages = computed({
  get() {
    return props.contractId ? getContractChatBucket(props.contractId).messages : [];
  },
  set(value: UiMessage[]) {
    if (props.contractId) {
      getContractChatBucket(props.contractId).messages = value;
    }
  },
});
const sending = computed({
  get() {
    return props.contractId ? getContractChatBucket(props.contractId).sending : false;
  },
  set(value: boolean) {
    if (props.contractId) {
      getContractChatBucket(props.contractId).sending = value;
    }
  },
});
const chatSessionId = computed({
  get() {
    return props.contractId ? getContractChatBucket(props.contractId).chatSessionId : '';
  },
  set(value: string) {
    if (props.contractId) {
      getContractChatBucket(props.contractId).chatSessionId = value;
    }
  },
});
const selectedModelId = ref<number>();
const modelOptions = ref<{ label: string; value: number }[]>([]);
const hoverMessageId = ref('');
const scrollRef = ref<InstanceType<typeof ElScrollbar>>();
const memoryExpanded = ref(true);
const memoryShowAllSessions = ref(false);
const memoryList = ref<LegalContractApi.ContractMemory[]>([]);
const memoryLoading = ref(false);
const memoryDialogVisible = ref(false);
const memorySaving = ref(false);
const memoryEditingId = ref<number | null>(null);
const memoryForm = ref({
  memoryType: 'fact',
  content: '',
});
const memoryTypeOptions = [
  { label: '事实', value: 'fact' },
  { label: '里程碑', value: 'milestone' },
  { label: '风险', value: 'risk' },
  { label: '决策', value: 'decision' },
];
const abortController = ref<AbortController>();
const streamWatchdog = ref<number>();
const confirmWatchdog = ref<number>();
const streamConnected = ref(false);
const lastChunkAt = ref(0);
const receiveMessageFullText = ref('');
const receiveMessageDisplayedText = ref('');
/** 流式推理文案（与 AI 对话一致：SSE 直写，不走正文打字机） */
const streamingReasoningText = ref('');
const textSpeed = ref(50);
const textRoleRunning = ref(false);
const { copy } = useClipboard({ legacy: true });

const agentMode = ref(true);
const allowProposal = ref(true);
/** 等待用户 Confirm 时锁定 Agent/提案开关，并保持 SSE 会话 */
const awaitingAgentConfirm = ref(false);
const activeToolTraces = ref<ToolTrace[]>([]);
const proposalActionLoading = ref<string | null>(null);
/** 用户手动上滑后为 true，暂停自动滚底（与 AI 对话 list 一致） */
const userScrolledUp = ref(false);
let activeStreamTask: Promise<void> | undefined;

const THINKING_PLACEHOLDER = '思考中...';

function genSessionId() {
  return globalThis.crypto?.randomUUID?.()
    ?? `sess-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function ensureChatSessionId() {
  if (!chatSessionId.value) {
    chatSessionId.value = genSessionId();
  }
  return chatSessionId.value;
}

function mapDbMessage(item: LegalContractApi.ChatMessage): UiMessage {
  return {
    id: String(item.id),
    dbId: item.id,
    role: item.type,
    content: item.content,
    reasoningContent: item.reasoningContent,
  };
}

async function loadContractMemories() {
  if (!props.contractId) {
    memoryList.value = [];
    return;
  }
  memoryLoading.value = true;
  try {
    const sessionId = memoryShowAllSessions.value ? undefined : ensureChatSessionId();
    memoryList.value = await getContractMemoryList(props.contractId, sessionId);
  } catch {
    memoryList.value = [];
  } finally {
    memoryLoading.value = false;
  }
}

async function handleDeleteMemory(id: number) {
  if (!props.contractId) {
    return;
  }
  await deleteContractMemory(id, props.contractId);
  ElMessage.success('已删除记忆');
  await loadContractMemories();
}

function openMemoryDialog(item?: LegalContractApi.ContractMemory) {
  memoryEditingId.value = item?.id ?? null;
  memoryForm.value = {
    memoryType: item?.memoryType || 'fact',
    content: item?.content || '',
  };
  memoryDialogVisible.value = true;
}

async function handleSaveMemory() {
  if (!props.contractId || !memoryForm.value.content.trim()) {
    ElMessage.warning('请填写记忆内容');
    return;
  }
  memorySaving.value = true;
  try {
    const sessionId = ensureChatSessionId();
    if (memoryEditingId.value) {
      await updateContractMemory({
        id: memoryEditingId.value,
        contractId: props.contractId,
        memoryType: memoryForm.value.memoryType,
        content: memoryForm.value.content.trim(),
      });
      ElMessage.success('记忆已更新');
    } else {
      await createContractMemory({
        contractId: props.contractId,
        sessionId,
        memoryType: memoryForm.value.memoryType,
        content: memoryForm.value.content.trim(),
      });
      ElMessage.success('记忆已添加');
    }
    memoryDialogVisible.value = false;
    memoryExpanded.value = true;
    await loadContractMemories();
  } finally {
    memorySaving.value = false;
  }
}

function memoryTypeLabel(type: string) {
  switch (type) {
    case 'fact':
      return '事实';
    case 'milestone':
      return '里程碑';
    case 'risk':
      return '风险';
    case 'decision':
      return '决策';
    default:
      return type;
  }
}

async function loadMessagesFromServer() {
  if (!props.contractId || sending.value) {
    return;
  }
  try {
    const sessionId = ensureChatSessionId();
    const list = await getContractChatMessageList(props.contractId, sessionId);
    messages.value = (list || [])
      .filter((item) => item.content?.trim())
      .map(mapDbMessage);
    void loadContractMemories();
  } catch {
    if (!messages.value.length) {
      messages.value = [];
    }
  }
}

async function notifyContractChanged() {
  emit('contract-changed');
  await loadMessagesFromServer();
}

function handleToolEvent(data: LegalContractApi.ChatResp) {
  if (!data.eventType) {
    return;
  }
  if (data.eventType === 'error') {
    const assistant = getLastAssistantMessage();
    if (assistant) {
      const hasAnswer =
        receiveMessageFullText.value.trim().length > 0
        || (
          !!assistant.content?.trim()
          && assistant.content !== THINKING_PLACEHOLDER
          && !assistant.content.startsWith('Agent 问答')
        );
      if (!hasAnswer) {
        assistant.content = data.content || '问答失败，请重试';
      }
      if (assistant.pending) {
        finalizeAssistant(assistant);
      }
    }
    awaitingAgentConfirm.value = false;
    stopConfirmWatchdog();
    abortController.value?.abort();
    return;
  }
  if (data.eventType === 'proposal' && data.proposalNo) {
    const assistant = getLastAssistantMessage();
    const proposal: AgentProposal = {
      proposalNo: data.proposalNo,
      proposalAction: data.proposalAction || '',
      proposalTitle: data.proposalTitle || data.proposalNo,
      proposalPayload: data.proposalPayload,
      status: 'pending',
    };
    if (assistant) {
      assistant.proposals = [...(assistant.proposals || []), proposal];
    }
    void autoExecuteProposal(proposal);
    return;
  }
  if (data.eventType === 'confirm_required' && data.confirmId) {
    void silentApproveToolConfirm(data.confirmId);
    return;
  }
  if (!data.toolName) {
    return;
  }
  if (data.eventType === 'tool_start') {
    activeToolTraces.value.push({
      id: genMessageId('tool'),
      toolName: data.toolName,
      status: 'running',
    });
    const assistant = getLastAssistantMessage();
    if (assistant) {
      assistant.toolTraces = [...activeToolTraces.value];
    }
    return;
  }
  if (data.eventType === 'tool_end') {
    const trace = [...activeToolTraces.value].reverse().find(
      (t) => t.toolName === data.toolName && t.status === 'running',
    );
    if (trace) {
      trace.status = 'done';
      trace.summary = data.toolSummary;
    }
    const assistant = getLastAssistantMessage();
    if (assistant) {
      assistant.toolTraces = [...activeToolTraces.value];
    }
    if (
      data.toolName &&
      /adopt/i.test(data.toolName) &&
      data.toolSummary &&
      !data.toolSummary.includes('失败')
    ) {
      void notifyContractChanged();
    }
  }
}

async function silentApproveToolConfirm(confirmId: string) {
  if (!chatSessionId.value) {
    return;
  }
  try {
    await confirmAgent({
      sessionId: chatSessionId.value,
      confirmId,
      approved: true,
    });
  } catch {
    // 静默失败，不打断主对话流
  }
}

async function autoExecuteProposal(proposal: AgentProposal) {
  if (proposal.status !== 'pending') {
    return;
  }
  try {
    await executeAgentProposal({ proposalNo: proposal.proposalNo });
    proposal.status = 'executed';
    void notifyContractChanged();
  } catch {
    proposal.status = 'pending';
  }
}

function resetToolTraces() {
  activeToolTraces.value = [];
}

function proposalActionLabel(action?: string) {
  if (action === 'ADOPT_OPINION') {
    return '采纳审核意见';
  }
  if (action === 'SKIP_PARAGRAPH') {
    return '更新段落审核跳过状态';
  }
  return action || '写操作';
}

async function handleConfirmProposal(proposal: AgentProposal) {
  if (proposalActionLoading.value || proposal.status !== 'pending') {
    return;
  }
  proposalActionLoading.value = proposal.proposalNo;
  try {
    await executeAgentProposal({ proposalNo: proposal.proposalNo });
    proposal.status = 'executed';
    ElMessage.success('操作已执行，工作台意见已更新');
    void notifyContractChanged();
  } catch {
    ElMessage.error('执行提案失败');
  } finally {
    proposalActionLoading.value = null;
  }
}

async function handleCancelProposal(proposal: AgentProposal) {
  if (proposalActionLoading.value || proposal.status !== 'pending') {
    return;
  }
  proposalActionLoading.value = proposal.proposalNo;
  try {
    await cancelAgentProposal({ proposalNo: proposal.proposalNo });
    proposal.status = 'cancelled';
    ElMessage.info('已取消提案');
  } catch {
    ElMessage.error('取消提案失败');
  } finally {
    proposalActionLoading.value = null;
  }
}

function applyStreamPayload(
  data: LegalContractApi.ChatResp | undefined,
  assistant: UiMessage,
  user: UiMessage | undefined,
  state: { gotRealPayload: boolean; isFirstChunk: boolean },
) {
  if (!data) {
    return;
  }
  if (data.eventType) {
    handleToolEvent(data);
  }
  if (user && data.userMessageId) {
    user.dbId = data.userMessageId;
    user.id = String(data.userMessageId);
  }
  if (data.assistantMessageId) {
    assistant.dbId = data.assistantMessageId;
    assistant.id = String(data.assistantMessageId);
  }
  if (!data.content && !data.reasoningContent) {
    return;
  }
  if (state.isFirstChunk) {
    state.isFirstChunk = false;
    if (assistant.content === THINKING_PLACEHOLDER) {
      assistant.content = '';
    }
  }
  if (data.reasoningContent) {
    appendStreamingReasoning(data.reasoningContent);
  }
  if (data.content) {
    receiveMessageFullText.value += data.content;
    state.gotRealPayload = true;
  }
}

function isAssistantStreaming(index: number) {
  if (!sending.value && !awaitingAgentConfirm.value) {
    return false;
  }
  const msg = messages.value[index];
  if (!msg || msg.role === 'user') {
    return false;
  }
  return index === messages.value.length - 1;
}

/** 与 AI 对话 list.vue 完全一致：以消息 content 判断正文是否已开始展示 */
function isThinkingStreaming(msg: UiMessage, index: number) {
  if (!isAssistantStreaming(index)) {
    return false;
  }
  const content = msg.content?.trim() || '';
  const hasRealContent = content && content !== THINKING_PLACEHOLDER;
  return !hasRealContent;
}

function resolveReasoningContent(msg: UiMessage, index: number) {
  if (isAssistantStreaming(index)) {
    return streamingReasoningText.value || msg.reasoningContent || '';
  }
  return msg.reasoningContent || '';
}

function hasThinkingBlock(msg: UiMessage, index: number) {
  return (
    !!resolveReasoningContent(msg, index).trim() ||
    isThinkingStreaming(msg, index)
  );
}

function genMessageId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

async function scrollToBottom(force = false) {
  await nextTick();
  if (!force && userScrolledUp.value) {
    return;
  }
  const wrap = scrollRef.value?.wrapRef as HTMLElement | undefined;
  if (wrap) {
    wrap.scrollTop = wrap.scrollHeight;
  }
}

function handleScroll() {
  const wrap = scrollRef.value?.wrapRef as HTMLElement | undefined;
  if (!wrap) {
    return;
  }
  userScrolledUp.value =
    wrap.scrollTop + wrap.clientHeight < wrap.scrollHeight - 80;
}

function scrollToBottomManually() {
  userScrolledUp.value = false;
  void scrollToBottom(true);
}

function getLastAssistantMessage(): UiMessage | undefined {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const msg = messages.value[i];
    if (msg?.role === 'assistant') {
      return msg;
    }
  }
  return undefined;
}

function syncDisplayedContentToAssistant() {
  const assistant = getLastAssistantMessage();
  if (!assistant) {
    return;
  }
  if (receiveMessageFullText.value) {
    assistant.content = receiveMessageFullText.value;
  } else if (receiveMessageDisplayedText.value) {
    assistant.content = receiveMessageDisplayedText.value;
  }
}

function appendStreamingReasoning(delta: string) {
  if (!delta) {
    return;
  }
  streamingReasoningText.value += delta;
  const last = getLastAssistantMessage();
  if (last) {
    last.reasoningContent = streamingReasoningText.value;
  }
}

function resetStreamBuffers() {
  receiveMessageFullText.value = '';
  receiveMessageDisplayedText.value = '';
  streamingReasoningText.value = '';
  textRoleRunning.value = false;
}

function stopWatchdog() {
  if (streamWatchdog.value) {
    window.clearInterval(streamWatchdog.value);
    streamWatchdog.value = undefined;
  }
}

function stopConfirmWatchdog() {
  if (confirmWatchdog.value) {
    window.clearTimeout(confirmWatchdog.value);
    confirmWatchdog.value = undefined;
  }
}

function startConfirmWatchdog() {
  stopConfirmWatchdog();
  confirmWatchdog.value = window.setTimeout(() => {
    if (!awaitingAgentConfirm.value) {
      return;
    }
    awaitingAgentConfirm.value = false;
    const assistant = getLastAssistantMessage();
    if (assistant?.pendingConfirm) {
      assistant.pendingConfirm.status = 'denied';
    }
    ElMessage.warning('确认等待超时，请重新提问');
    abortController.value?.abort();
    if (assistant?.pending) {
      assistant.content = '确认超时，请重新提问';
      finalizeAssistant(assistant);
    }
  }, 300_000);
}

function finalizeAssistant(assistant: UiMessage) {
  syncDisplayedContentToAssistant();
  if (streamingReasoningText.value) {
    assistant.reasoningContent = streamingReasoningText.value;
  }
  assistant.pending = false;
  awaitingAgentConfirm.value = false;
  sending.value = false;
  stopWatchdog();
  stopConfirmWatchdog();
  resetStreamBuffers();
  void loadContractMemories();
  void scrollToBottom();
}

/** 与 AI 对话一致的打字机滚动效果 */
async function textRoll() {
  let index = 0;
  try {
    if (textRoleRunning.value) {
      return;
    }
    textRoleRunning.value = true;
    receiveMessageDisplayedText.value = '';
    let timer: ReturnType<typeof setTimeout>;
    async function task() {
      const diff =
        (receiveMessageFullText.value.length -
          receiveMessageDisplayedText.value.length) /
        10;
      if (diff > 5) {
        textSpeed.value = 10;
      } else if (diff > 2) {
        textSpeed.value = 30;
      } else if (diff > 1.5) {
        textSpeed.value = 50;
      } else {
        textSpeed.value = 100;
      }
      if (!sending.value) {
        textSpeed.value = 10;
      }

      if (index < receiveMessageFullText.value.length) {
        receiveMessageDisplayedText.value +=
          receiveMessageFullText.value[index];
        index++;
        const assistant = getLastAssistantMessage();
        if (assistant) {
          assistant.content = receiveMessageDisplayedText.value;
        }
        await scrollToBottom();
        timer = setTimeout(task, textSpeed.value);
      } else if (sending.value) {
        timer = setTimeout(task, textSpeed.value);
      } else {
        textRoleRunning.value = false;
        clearTimeout(timer);
      }
    }
    timer = setTimeout(task, textSpeed.value);
  } catch {
    textRoleRunning.value = false;
  }
}

function startWatchdog() {
  stopWatchdog();
  lastChunkAt.value = Date.now();
  streamWatchdog.value = window.setInterval(() => {
    if (!sending.value) {
      stopWatchdog();
      return;
    }
    const watchdogMs = agentMode.value ? 95_000 : 25_000;
    if (Date.now() - lastChunkAt.value > watchdogMs) {
      abortController.value?.abort();
      const last = messages.value[messages.value.length - 1];
      if (last?.pending) {
        syncDisplayedContentToAssistant();
        if (
          !last.content.trim() ||
          last.content === THINKING_PLACEHOLDER
        ) {
          last.content = streamConnected.value
            ? '流式响应超时，请重试'
            : '连接未建立，请重试';
        }
        finalizeAssistant(last);
      }
    }
  }, 1000);
}

async function askWithStreaming(question: string) {
  abortController.value?.abort();
  if (activeStreamTask) {
    await activeStreamTask.catch(() => {});
  }

  const user: UiMessage = { id: genMessageId('u'), role: 'user', content: question };
  const assistant: UiMessage = {
    id: genMessageId('a'),
    role: 'assistant',
    content: THINKING_PLACEHOLDER,
    reasoningContent: '',
    pending: true,
    toolTraces: [],
  };
  messages.value.push(user, assistant);
  sending.value = true;
  streamConnected.value = false;
  userScrolledUp.value = false;
  resetStreamBuffers();
  resetToolTraces();
  await scrollToBottom(true);
  void textRoll();

  const ctrl = new AbortController();
  abortController.value = ctrl;
  startWatchdog();
  const streamState = { gotRealPayload: false, isFirstChunk: true };
  let streamSettled = false;

  const settleStream = () => {
    streamSettled = true;
  };

  try {
    activeStreamTask = sendContractChatStream(
      {
        contractId: props.contractId,
        message: question,
        agentMode: agentMode.value,
        allowProposal: agentMode.value && allowProposal.value,
        sessionId: ensureChatSessionId(),
      },
      ctrl,
      (event) => {
        if (!event.data?.trim()) {
          return;
        }
        streamConnected.value = true;
        lastChunkAt.value = Date.now();
        try {
          const { code, data, msg } = JSON.parse(event.data) as {
            code: number;
            data?: LegalContractApi.ChatResp;
            msg?: string;
          };
          if (code !== 0) {
            assistant.content = msg || '问答失败，请重试';
            if (!streamSettled) {
              settleStream();
              finalizeAssistant(assistant);
            }
            return;
          }
          applyStreamPayload(data, assistant, user, streamState);
        } catch {
          // 忽略无法解析的 SSE 片段
        }
      },
      (error) => {
        if ((error as { name?: string })?.name === 'AbortError') {
          return;
        }
        if (!assistant.content || assistant.content === THINKING_PLACEHOLDER) {
          assistant.content = '流式请求异常，请重试';
        }
        if (!streamSettled) {
          settleStream();
          finalizeAssistant(assistant);
        }
        // 禁止 fetch-event-source 自动重试，避免影响下一轮提问
        throw error;
      },
      () => {
        if (streamSettled || awaitingAgentConfirm.value) {
          if (awaitingAgentConfirm.value) {
            stopWatchdog();
          }
          return;
        }
        settleStream();
        const displayed = receiveMessageDisplayedText.value.trim();
        const full = receiveMessageFullText.value.trim();
        if (
          !full &&
          !displayed &&
          !streamingReasoningText.value.trim() &&
          !assistant.reasoningContent?.trim()
        ) {
          assistant.content = streamState.gotRealPayload
            ? '（模型未返回内容）'
            : '未收到模型返回，请重试';
        }
        finalizeAssistant(assistant);
      },
    );
    await activeStreamTask;
  } catch {
    if (assistant.pending) {
      finalizeAssistant(assistant);
    }
  } finally {
    if (activeStreamTask) {
      activeStreamTask = undefined;
    }
  }
}

async function handleSend() {
  const text = input.value.trim();
  if (!text || !props.contractId || props.disabled || sending.value) {
    return;
  }
  input.value = '';
  await askWithStreaming(text);
}

async function handleCopy(content: string) {
  await copy(content || '');
  ElMessage.success('复制成功！');
}

async function handleDelete(index: number) {
  if (sending.value) {
    return;
  }
  const next = messages.value[index + 1];
  const current = messages.value[index];
  try {
    if (current?.dbId) {
      await deleteContractChatMessage(current.dbId);
    }
  } catch {
    ElMessage.error('删除消息失败');
    return;
  }
  if (current?.role === 'user' && next?.role === 'assistant') {
    messages.value.splice(index, 2);
  } else {
    messages.value.splice(index, 1);
  }
}

async function handleRefresh(index: number) {
  if (sending.value) {
    return;
  }
  const current = messages.value[index];
  if (!current || current.role !== 'user' || !current.content.trim()) {
    return;
  }
  try {
    if (current.dbId) {
      await deleteContractChatMessagesFrom(current.dbId);
    }
  } catch {
    ElMessage.error('删除历史消息失败');
    return;
  }
  messages.value.splice(index);
  await askWithStreaming(current.content);
}

function handleEdit(index: number) {
  if (sending.value) {
    return;
  }
  const current = messages.value[index];
  if (current?.role === 'user') {
    input.value = current.content;
  }
}

function handleStop() {
  abortController.value?.abort();
  const last = getLastAssistantMessage();
  if (last?.pending) {
    syncDisplayedContentToAssistant();
    if (streamingReasoningText.value) {
      last.reasoningContent = streamingReasoningText.value;
    }
    last.pending = false;
    if (
      !last.content.trim() ||
      last.content === THINKING_PLACEHOLDER
    ) {
      last.content = '已手动停止';
    }
    sending.value = false;
    stopWatchdog();
    resetStreamBuffers();
  } else {
    sending.value = false;
    stopWatchdog();
    resetStreamBuffers();
  }
}

async function handleClear() {
  if (sending.value || !props.contractId) {
    return;
  }
  try {
    await clearContractChatMessages(props.contractId);
    messages.value = [];
    chatSessionId.value = '';
  } catch {
    ElMessage.error('清空对话失败');
  }
}

async function loadModelOptions() {
  try {
    const list = await getModelSimpleList(AiModelTypeEnum.CHAT);
    modelOptions.value = (list || [])
      .map((m) => ({
        label: `${m.name}（${m.model}）`,
        value: m.id,
        sort: m.sort ?? 0,
      }))
      .sort((a, b) => a.sort - b.sort)
      .map(({ label, value }) => ({ label, value }));
  } catch {
    modelOptions.value = [];
  }
}

watch(agentMode, (enabled) => {
  if (!enabled) {
    allowProposal.value = false;
  }
});

watch(
  () => props.contractId,
  async () => {
    await loadMessagesFromServer();
    if (!modelOptions.value.length) {
      await loadModelOptions();
    }
    if (props.contractId) {
      try {
        const contract = await getContract(props.contractId);
        selectedModelId.value = contract.modelId;
      } catch {
        selectedModelId.value = undefined;
      }
    } else {
      selectedModelId.value = undefined;
    }
    void scrollToBottom();
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  abortController.value?.abort();
  stopWatchdog();
  stopConfirmWatchdog();
});
</script>

<template>
  <div
    class="contract-chat-panel relative flex h-full min-h-0 flex-col gap-3"
    :class="embedded ? 'px-2 pb-2 pt-1' : 'min-h-[480px]'"
  >
    <div class="flex shrink-0 flex-wrap items-center justify-between gap-2">
      <div class="text-sm text-muted-foreground">
        {{
          agentMode
            ? allowProposal
              ? 'Agent 执行模式：可说「采纳全部待处置意见」，将直接写入合同，无需弹框确认'
              : 'Agent 模式：按需调用工具查段落/意见/知识库'
            : '普通模式：基于合同上下文回答，支持多轮追问'
        }}
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <span class="text-xs text-muted-foreground">Agent</span>
        <ElSwitch
          v-model="agentMode"
          size="small"
          :disabled="sending || awaitingAgentConfirm"
        />
        <span class="text-xs text-muted-foreground">直接执行写操作</span>
        <ElSwitch
          v-model="allowProposal"
          size="small"
          :disabled="!agentMode || sending || awaitingAgentConfirm"
        />
        <span class="text-xs text-muted-foreground">当前模型</span>
        <ElSelect
          v-model="selectedModelId"
          class="!w-56"
          size="small"
          disabled
          placeholder="未配置模型"
        >
          <ElOption
            v-for="opt in modelOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </ElSelect>
        <ElButton
          size="small"
          :disabled="messages.length === 0 || sending"
          @click="handleClear"
        >
          清空
        </ElButton>
      </div>
    </div>

    <div
      v-loading="memoryLoading"
      class="contract-memory-panel shrink-0 rounded-lg border border-dashed border-border bg-muted/20 px-3 py-2"
    >
      <div class="flex items-center justify-between gap-2">
        <button
          type="button"
          class="contract-memory-panel__toggle flex min-w-0 flex-1 items-center justify-between gap-2 text-left text-xs text-muted-foreground"
          @click="memoryExpanded = !memoryExpanded"
        >
          <span class="flex items-center gap-2 font-medium text-foreground">
            <IconifyIcon icon="lucide:brain" class="size-3.5" />
            情节记忆
            <ElTag size="small" type="info">{{ memoryList.length }}</ElTag>
          </span>
          <IconifyIcon
            :icon="memoryExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'"
            class="size-4"
          />
        </button>
        <div class="flex shrink-0 items-center gap-1">
          <ElButton
            size="small"
            link
            :type="memoryShowAllSessions ? 'primary' : 'default'"
            @click="memoryShowAllSessions = !memoryShowAllSessions; void loadContractMemories()"
          >
            {{ memoryShowAllSessions ? '全部会话' : '当前会话' }}
          </ElButton>
          <ElButton size="small" link type="primary" @click="openMemoryDialog()">
            添加
          </ElButton>
        </div>
      </div>
      <ul v-show="memoryExpanded" class="contract-memory-panel__list mt-2 space-y-2">
        <li
          v-for="item in memoryList"
          :key="item.id"
          class="contract-memory-panel__item flex items-start justify-between gap-2 rounded-md bg-background px-2 py-1.5 text-xs leading-relaxed"
        >
          <div class="min-w-0 flex-1">
            <ElTag size="small" class="mr-1.5">{{ memoryTypeLabel(item.memoryType) }}</ElTag>
            <span>{{ item.content }}</span>
          </div>
          <div class="flex shrink-0 gap-0.5">
            <ElButton class="!px-1" text size="small" @click="openMemoryDialog(item)">
              <IconifyIcon icon="lucide:edit" class="size-3.5" />
            </ElButton>
            <ElButton class="!px-1" text size="small" @click="handleDeleteMemory(item.id)">
              <IconifyIcon icon="lucide:trash" class="size-3.5" />
            </ElButton>
          </div>
        </li>
      </ul>
      <p v-show="memoryExpanded && memoryList.length === 0" class="mt-2 text-xs text-muted-foreground">
        暂无情节记忆，可点击「添加」手动录入，或在问答后由系统自动抽取。
      </p>
    </div>

    <ElDialog
      v-model="memoryDialogVisible"
      :title="memoryEditingId ? '编辑情节记忆' : '添加情节记忆'"
      width="480px"
      destroy-on-close
    >
      <ElForm label-width="72px">
        <ElFormItem label="类型">
          <ElSelect v-model="memoryForm.memoryType" class="w-full">
            <ElOption
              v-for="opt in memoryTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="内容">
          <ElInput
            v-model="memoryForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="例如：付款周期为 30 天；违约金上限 10%"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="memoryDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="memorySaving" @click="handleSaveMemory">
          保存
        </ElButton>
      </template>
    </ElDialog>

    <ElScrollbar ref="scrollRef" class="min-h-0 flex-1 rounded-lg border border-border bg-muted/10 py-3" @scroll="handleScroll">
      <ElEmpty v-if="messages.length === 0" description="例如：请总结高风险条款，或解释第 3 条违约责任" />
      <div v-else>
        <div
          v-for="(msg, index) in messages"
          :key="msg.id"
          class="message-group flex flex-col overflow-y-hidden px-5"
          @mouseenter="hoverMessageId = msg.id"
          @mouseleave="hoverMessageId = ''"
        >
          <template v-if="msg.role === 'summary'">
            <CompactionSummaryMessage :content="msg.content" />
          </template>
          <template v-else-if="msg.role === 'user'">
            <AiChatUserMessage :content="msg.content" />
            <div class="user-extra" :class="{ visible: hoverMessageId === msg.id }">
              <div class="user-actions">
                <ElButton class="!ml-0 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleCopy(msg.content)">
                  <IconifyIcon icon="lucide:copy" />
                </ElButton>
                <ElButton class="!ml-1 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleDelete(index)">
                  <IconifyIcon icon="lucide:trash" />
                </ElButton>
                <ElButton class="!ml-1 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleRefresh(index)">
                  <IconifyIcon icon="lucide:refresh-cw" />
                </ElButton>
                <ElButton class="!ml-1 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleEdit(index)">
                  <IconifyIcon icon="lucide:edit" />
                </ElButton>
              </div>
            </div>
          </template>
          <template v-else>
            <div
              v-if="msg.proposals?.some((p) => p.status === 'executed')"
              class="agent-proposal mb-2 ml-11 rounded-md border border-green-200 bg-green-50 px-3 py-2 text-xs dark:border-green-800 dark:bg-green-950/30"
            >
              <div
                v-for="proposal in msg.proposals?.filter((p) => p.status === 'executed')"
                :key="proposal.proposalNo"
              >
                已执行：{{ proposal.proposalTitle || proposalActionLabel(proposal.proposalAction) }}
              </div>
            </div>
            <div
              v-if="msg.toolTraces?.length"
              class="agent-tool-trace mb-2 ml-11 rounded-md border border-border bg-muted/30 px-3 py-2 text-xs"
            >
              <div
                v-for="trace in msg.toolTraces"
                :key="trace.id"
                class="flex items-start gap-1.5 py-0.5 text-muted-foreground"
              >
                <IconifyIcon
                  icon="lucide:wrench"
                  class="mt-0.5 size-3.5 shrink-0"
                />
                <span>
                  {{ trace.toolName }}
                  <span v-if="trace.status === 'running'">…</span>
                  <span v-else-if="trace.summary"> — {{ trace.summary }}</span>
                </span>
              </div>
            </div>
            <AiChatAssistantMessage
              :reasoning-content="resolveReasoningContent(msg, index)"
              :content="msg.content || ''"
              :streaming="isAssistantStreaming(index)"
              :thinking-streaming="isThinkingStreaming(msg, index)"
              :show-thinking="hasThinkingBlock(msg, index)"
              :is-last="index === messages.length - 1"
            />
            <div class="assistant-extra ml-11" :class="{ visible: hoverMessageId === msg.id }">
              <div class="mt-2 flex flex-row">
                <ElButton class="!ml-0 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleCopy(msg.content)">
                  <IconifyIcon icon="lucide:copy" />
                </ElButton>
                <ElButton class="!ml-1 flex items-center bg-transparent !px-1.5 hover:bg-gray-100" text @click="handleDelete(index)">
                  <IconifyIcon icon="lucide:trash" />
                </ElButton>
              </div>
            </div>
          </template>
        </div>
      </div>
    </ElScrollbar>

    <ElButton
      v-if="userScrolledUp && messages.length > 0"
      class="absolute bottom-28 right-8 z-10 shadow-md"
      size="small"
      round
      @click="scrollToBottomManually"
    >
      回到底部
    </ElButton>

    <div
      class="contract-chat-panel__composer shrink-0 border-t border-border bg-card pt-2"
    >
      <div class="flex gap-2">
        <ElInput
          v-model="input"
          type="textarea"
          :rows="2"
          :disabled="disabled || sending"
          placeholder="输入关于本合同的问题，Enter 发送"
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="flex shrink-0 flex-col gap-2">
          <ElButton type="primary" :loading="sending" :disabled="disabled" @click="handleSend">
            <IconifyIcon icon="lucide:send" class="mr-1 size-4" />
            发送
          </ElButton>
          <ElButton v-if="sending" @click="handleStop">停止</ElButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.assistant-extra,
.user-extra {
  opacity: 0;
  transition: opacity 0.15s ease;
}
.assistant-extra.visible,
.user-extra.visible {
  opacity: 1;
}
.assistant-extra {
  margin-top: -4px;
}
.user-extra {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding-right: 44px;
  margin-top: -12px;
}
.user-actions {
  display: flex;
}
</style>
