import { reactive } from 'vue';

/** 合同问答 UI 消息（审阅侧栏与「合同问答」Tab 共用） */
export interface ContractChatUiMessage {
  id: string;
  dbId?: number;
  role: 'assistant' | 'user';
  content: string;
  reasoningContent?: string;
  pending?: boolean;
  toolTraces?: Array<{
    id: string;
    toolName: string;
    status: 'running' | 'done';
    summary?: string;
  }>;
  proposals?: Array<{
    proposalNo: string;
    proposalAction: string;
    proposalTitle: string;
    proposalPayload?: string;
    status: 'pending' | 'executed' | 'cancelled';
  }>;
  pendingConfirm?: {
    confirmId: string;
    toolName: string;
    summary: string;
    status: 'pending' | 'approved' | 'denied';
  };
}

export interface ContractChatBucket {
  messages: ContractChatUiMessage[];
  chatSessionId: string;
  sending: boolean;
}

const buckets = reactive<Record<number, ContractChatBucket>>({});

/** 按 contractId 取共享问答桶（多实例 ContractChatPanel 读写同一份） */
export function getContractChatBucket(contractId: number): ContractChatBucket {
  if (!buckets[contractId]) {
    buckets[contractId] = {
      messages: [],
      chatSessionId: '',
      sending: false,
    };
  }
  return buckets[contractId];
}
