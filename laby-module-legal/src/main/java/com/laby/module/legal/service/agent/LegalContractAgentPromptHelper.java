package com.laby.module.legal.service.agent;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.module.legal.enums.contract.LegalContractChatAnswerModeEnum;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import com.laby.module.legal.service.memory.LegalContractEpisodicMemoryService;
import com.laby.module.legal.service.memory.LegalUserFactService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 法务合同 Agent System Prompt 组装（优先读 ai_chat_role 配置）
 */
@Component
public class LegalContractAgentPromptHelper {

    private static final String PROPOSAL_MODE_SUFFIX = """

            写操作规则（已开启，用户对话指令即授权，直接执行勿要求二次弹框确认）：
            6. 用户要求采纳未采纳/待处置/剩余意见时：先看 meta 的 pendingOpinionCountAllRounds，或 legal_get_audit_opinions(status=0)；若大于 0 则调用 legal_batch_adopt_pending_opinions（未指定风险等级时不要传 riskLevel）。
            7. 用户要求采纳指定一条时，先 legal_get_audit_opinions(status=0) 取 id，再 legal_adopt_opinion。
            8. 撤销采纳后意见会回到待处置(status=0)，仍应被批量采纳。
            9. 需要跳过或恢复段落审核时，调用 legal_propose_skip_paragraph。
            10. 执行后汇报已采纳条数与意见 id，禁止声称「审核不存在」除非合同真的未绑定。
            """;

    @Resource
    private LegalContractAuditRoleService auditRoleService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalContractEpisodicMemoryService episodicMemoryService;
    @Resource
    private LegalUserFactService userFactService;
    @Resource
    private LegalContractMemoryProperties memoryProperties;

    public String buildSystemPrompt(LegalContractChatAnswerModeEnum answerMode, boolean allowProposal) {
        return buildSystemPrompt(null, answerMode, allowProposal);
    }

    public String buildSystemPrompt(LegalContractDO contract, LegalContractChatAnswerModeEnum answerMode,
                                    boolean allowProposal) {
        String base = auditRoleService.resolveQaAgentSystemMessage(contract);
        if (contract != null && contract.getId() != null) {
            base += LegalAgentContractSnapshotHelper.toPromptBlock(contract, opinionMapper);
            base += episodicMemoryService.buildMemoryAppendix(contract.getId(), null);
            base += userFactService.buildUserFactAppendix(
                    contract.getId(), SecurityFrameworkUtils.getLoginUserId(),
                    memoryProperties.getMaxAppendixItems());
        }
        if (allowProposal) {
            base += PROPOSAL_MODE_SUFFIX;
        }
        return base + answerMode.getInstruction();
    }

    public String resolveBaseSystemMessage(Long contractTypeId) {
        return auditRoleService.resolveQaAgentSystemMessage(contractTypeId);
    }

}