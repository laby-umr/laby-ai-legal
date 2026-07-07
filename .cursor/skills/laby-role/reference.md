# 业务角色参考

## legal-backend 包路径

| 子域 | 包 |
|------|-----|
| 合同 CRUD | `service.contract` |
| 解析 | `service.contract.parse` |
| AI 审核 | `service.ai`, `service.ai.kernel` |
| OnlyOffice | `service.onlyoffice`, `framework.onlyoffice` |
| BPM | `service.bpm` |

## ai-backend 包路径

| 子域 | 包 |
|------|-----|
| Chat | `service.chat` |
| Knowledge | `service.knowledge` |
| AgentScope | `framework.agentscope` |
| LLM 门面 | `core.llm` |

## frontend 速查

```typescript
// api 与 VO 对齐；字典
import { DICT_TYPE } from '@vben/constants';
getDictOptions(DICT_TYPE.LEGAL_CONTRACT_STATUS);
```

## Spec 索引

- Legal：`docs/superpowers/specs/*legal*`
- AI/RAG：`docs/superpowers/specs/*ai*`, `*rag*`
