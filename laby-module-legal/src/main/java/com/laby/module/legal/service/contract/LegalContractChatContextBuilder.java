package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.enums.contract.LegalContractChatAnswerModeEnum;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackModelPolicyBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 合同问答上下文：段落、意见、报告截断与拼装。
 */
@Component
public class LegalContractChatContextBuilder {

    /** 注入 Prompt 的合同上下文字符上限 */
    private static final int MAX_CONTEXT_CHARS = 14_000;
    /** 上下文中最多引用的段落条数 */
    private static final int MAX_PARAGRAPH_IN_CONTEXT = 40;
    /** 单段落在上下文中截断长度 */
    private static final int MAX_PARAGRAPH_CHARS = 400;
    /** 上下文中最多引用的审核意见条数 */
    private static final int MAX_OPINIONS_IN_CONTEXT = 30;
    private static final int MAX_REPORT_CHARS = 4_000;

    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAuditReportMapper reportMapper;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private LegalSkillPackRegistry skillPackRegistry;

    public static String buildSystemPrompt(LegalContractChatAnswerModeEnum answerMode) {
        return """
                你是法务合同审核助手。仅根据提供的合同上下文、AI 审核意见与报告回答问题。
                若上下文不足以回答，请明确说明缺少哪些信息，不要编造条款。
                回答使用 Markdown，可引用段落编号（如 p-12）与意见标题。
                """ + answerMode.getInstruction();
    }

    public String buildContractContext(LegalContractDO contract) {
        StringBuilder sb = new StringBuilder();
        sb.append("合同标题：").append(StrUtil.blankToDefault(contract.getTitle(), "-")).append('\n');
        sb.append("业务状态：").append(contract.getStatus()).append("，AI 轮次：")
                .append(contract.getAuditRound() != null ? contract.getAuditRound() : 1).append('\n');

        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contract.getId());
        if (CollUtil.isNotEmpty(paragraphs)) {
            sb.append("\n## 合同段落（节选）\n");
            int count = 0;
            for (LegalContractParagraphDO p : paragraphs) {
                if (count >= MAX_PARAGRAPH_IN_CONTEXT || sb.length() > MAX_CONTEXT_CHARS) {
                    sb.append("…（段落过多，已截断）\n");
                    break;
                }
                if (Boolean.TRUE.equals(p.getSkipAudit())) {
                    continue;
                }
                String text = StrUtil.sub(StrUtil.blankToDefault(p.getText(), ""), 0, MAX_PARAGRAPH_CHARS);
                sb.append('[').append(p.getParagraphId()).append("] ").append(text).append('\n');
                count++;
            }
        }

        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractId(contract.getId());
        if (CollUtil.isNotEmpty(opinions)) {
            sb.append("\n## AI/人工审核意见\n");
            int i = 0;
            for (LegalAuditOpinionDO o : opinions) {
                if (i++ >= MAX_OPINIONS_IN_CONTEXT || sb.length() > MAX_CONTEXT_CHARS) {
                    break;
                }
                sb.append("- [").append(o.getRiskLevel()).append("] ")
                        .append(o.getTitle()).append("：")
                        .append(StrUtil.sub(o.getContent(), 0, 200));
                if (StrUtil.isNotBlank(o.getParagraphId())) {
                    sb.append(" （段落 ").append(o.getParagraphId()).append('）');
                }
                sb.append('\n');
            }
        }

        int round = contract.getAuditRound() != null ? contract.getAuditRound() : 1;
        LegalAuditReportDO report = reportMapper.selectByContractIdAndRound(contract.getId(), round);
        if (report == null) {
            report = reportMapper.selectLatestByContractId(contract.getId());
        }
        if (report != null && StrUtil.isNotBlank(report.getContent())) {
            sb.append("\n## 审核报告摘要\n");
            sb.append(StrUtil.sub(report.getContent(), 0, MAX_REPORT_CHARS));
            if (report.getContent().length() > MAX_REPORT_CHARS) {
                sb.append("\n…（报告已截断）");
            }
        }
        return sb.toString();
    }

    public int resolveChatMaxTokens(int defaultMax, LegalContractDO contract) {
        return skillPackSnapshotService.resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                .flatMap(skillPackRegistry::parseModelPolicy)
                .map(LegalSkillPackModelPolicyBO::getMaxTokens)
                .filter(max -> max != null && max > 0)
                .orElse(defaultMax);
    }

}
