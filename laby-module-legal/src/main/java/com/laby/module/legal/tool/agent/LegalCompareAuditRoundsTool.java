package com.laby.module.legal.tool.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对比一轮与二轮审核意见（contractId 来自 RuntimeContext）。
 */
@Component("legal_compare_audit_rounds")
public class LegalCompareAuditRoundsTool {

    private static final int SAMPLE_LIMIT = 10;
    private static final int TITLE_MAX_CHARS = 80;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;

    @Data
    @JsonClassDescription("对比合同第一轮与第二轮审核意见差异")
    public static class Request {

        @JsonPropertyDescription("仅对比指定段落编号")
        private String paragraphId;

        @JsonPropertyDescription("仅对比指定风险等级 HIGH/MEDIUM/LOW")
        private String riskLevel;
    }

    @Data
    public static class Response {
        private int round1Count;
        private int round2Count;
        private List<OpinionSample> addedInRound2 = new ArrayList<>();
        private List<OpinionSample> resolvedFromRound1 = new ArrayList<>();
        private List<OpinionSample> stillOpenFromRound1 = new ArrayList<>();
        private List<ParagraphDiff> paragraphDiffs = new ArrayList<>();
    }

    @Data
    public static class OpinionSample {
        private Long id;
        private String title;
        private String riskLevel;
        private String paragraphId;
        private Integer status;
        private Integer auditRound;
    }

    @Data
    public static class ParagraphDiff {
        private String paragraphId;
        private int round1Opinions;
        private int round2Opinions;
    }

    @Tool(name = "legal_compare_audit_rounds",
            description = "对比合同第一轮与第二轮审核意见差异",
            readOnly = true, concurrencySafe = true)
    public Response compareAuditRounds(
            @ToolParam(name = "paragraphId", description = "仅对比指定段落编号", required = false) String paragraphId,
            @ToolParam(name = "riskLevel", description = "仅对比指定风险等级 HIGH/MEDIUM/LOW", required = false) String riskLevel,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setParagraphId(paragraphId);
        request.setRiskLevel(riskLevel);
        return doCompare(request, toolContext);
    }

    private Response doCompare(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        List<LegalAuditOpinionDO> round1 = filter(opinionMapper.selectListByContractIdAndRound(contract.getId(), 1),
                request);
        List<LegalAuditOpinionDO> round2 = filter(opinionMapper.selectListByContractIdAndRound(contract.getId(), 2),
                request);

        Response response = new Response();
        response.setRound1Count(round1.size());
        response.setRound2Count(round2.size());

        Set<String> round1Keys = round1.stream().map(this::opinionKey).collect(Collectors.toSet());
        for (LegalAuditOpinionDO opinion : round2) {
            if (!round1Keys.contains(opinionKey(opinion))) {
                addSample(response.getAddedInRound2(), opinion);
            }
        }

        for (LegalAuditOpinionDO opinion : round1) {
            if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())
                    || LegalOpinionStatusEnum.IGNORED.getStatus().equals(opinion.getStatus())) {
                addSample(response.getResolvedFromRound1(), opinion);
            } else if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                addSample(response.getStillOpenFromRound1(), opinion);
            }
        }

        response.setParagraphDiffs(buildParagraphDiffs(round1, round2));
        return response;
    }

    private List<LegalAuditOpinionDO> filter(List<LegalAuditOpinionDO> opinions, Request request) {
        if (CollUtil.isEmpty(opinions)) {
            return List.of();
        }
        return opinions.stream()
                .filter(o -> StrUtil.isBlank(request.getParagraphId())
                        || StrUtil.equals(request.getParagraphId(), o.getParagraphId()))
                .filter(o -> StrUtil.isBlank(request.getRiskLevel())
                        || StrUtil.equalsIgnoreCase(request.getRiskLevel(), o.getRiskLevel()))
                .toList();
    }

    private void addSample(List<OpinionSample> target, LegalAuditOpinionDO opinion) {
        if (target.size() >= SAMPLE_LIMIT) {
            return;
        }
        OpinionSample sample = new OpinionSample();
        sample.setId(opinion.getId());
        sample.setTitle(StrUtil.sub(StrUtil.blankToDefault(opinion.getTitle(), ""), 0, TITLE_MAX_CHARS));
        sample.setRiskLevel(opinion.getRiskLevel());
        sample.setParagraphId(opinion.getParagraphId());
        sample.setStatus(opinion.getStatus());
        sample.setAuditRound(opinion.getAuditRound());
        target.add(sample);
    }

    private String opinionKey(LegalAuditOpinionDO opinion) {
        return StrUtil.blankToDefault(opinion.getParagraphId(), "") + "|"
                + StrUtil.blankToDefault(opinion.getTitle(), "").trim().toLowerCase();
    }

    private List<ParagraphDiff> buildParagraphDiffs(List<LegalAuditOpinionDO> round1,
                                                    List<LegalAuditOpinionDO> round2) {
        Map<String, int[]> counts = new HashMap<>();
        for (LegalAuditOpinionDO opinion : round1) {
            String pid = StrUtil.blankToDefault(opinion.getParagraphId(), "-");
            counts.computeIfAbsent(pid, k -> new int[2])[0]++;
        }
        for (LegalAuditOpinionDO opinion : round2) {
            String pid = StrUtil.blankToDefault(opinion.getParagraphId(), "-");
            counts.computeIfAbsent(pid, k -> new int[2])[1]++;
        }
        List<ParagraphDiff> diffs = new ArrayList<>();
        List<String> paragraphIds = new ArrayList<>();
        for (LegalAuditOpinionDO opinion : round1) {
            String pid = StrUtil.blankToDefault(opinion.getParagraphId(), "-");
            if (!paragraphIds.contains(pid)) {
                paragraphIds.add(pid);
            }
        }
        for (LegalAuditOpinionDO opinion : round2) {
            String pid = StrUtil.blankToDefault(opinion.getParagraphId(), "-");
            if (!paragraphIds.contains(pid)) {
                paragraphIds.add(pid);
            }
        }
        for (String pid : paragraphIds) {
            int[] c = counts.getOrDefault(pid, new int[2]);
            if (c[0] == 0 && c[1] == 0) {
                continue;
            }
            ParagraphDiff diff = new ParagraphDiff();
            diff.setParagraphId(pid);
            diff.setRound1Opinions(c[0]);
            diff.setRound2Opinions(c[1]);
            diffs.add(diff);
            if (diffs.size() >= SAMPLE_LIMIT) {
                break;
            }
        }
        return diffs;
    }

}
