package com.laby.module.ai.controller.admin.knowledge;

import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgePageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgeRespVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgeSaveReqVO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiRagEvalCaseReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiRagEvalReportRespVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiVectorHealthReportRespVO;
import com.laby.module.ai.service.eval.AiRagEvalService;
import com.laby.module.ai.service.eval.bo.AiRagEvalCaseBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalExpectationBO;
import com.laby.module.ai.service.eval.bo.AiRagEvalReportBO;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import com.laby.module.ai.service.knowledge.AiVectorStoreHealthService;
import com.laby.module.ai.service.knowledge.bo.AiVectorHealthReportBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - AI 知识库")
@RestController
@RequestMapping("/ai/knowledge")
@Validated
public class AiKnowledgeController {

    @Resource
    private AiKnowledgeService knowledgeService;
    @Resource
    private AiVectorStoreHealthService vectorStoreHealthService;
    @Resource
    private AiRagEvalService ragEvalService;

    @GetMapping("/page")
    @Operation(summary = "获取知识库分页")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<PageResult<AiKnowledgeRespVO>> getKnowledgePage(@Valid AiKnowledgePageReqVO pageReqVO) {
        PageResult<AiKnowledgeDO> pageResult = knowledgeService.getKnowledgePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiKnowledgeRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<AiKnowledgeRespVO> getKnowledge(@RequestParam("id") Long id) {
        AiKnowledgeDO knowledge = knowledgeService.getKnowledge(id);
        return success(BeanUtils.toBean(knowledge, AiKnowledgeRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建知识库")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:create')")
    public CommonResult<Long> createKnowledge(@RequestBody @Valid AiKnowledgeSaveReqVO createReqVO) {
        return success(knowledgeService.createKnowledge(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新知识库")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:update')")
    public CommonResult<Boolean> updateKnowledge(@RequestBody @Valid AiKnowledgeSaveReqVO updateReqVO) {
        knowledgeService.updateKnowledge(updateReqVO);
        return success(true);
    }
    
    @DeleteMapping("/delete")
    @Operation(summary = "删除知识库")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:delete')")
    public CommonResult<Boolean> deleteKnowledge(@RequestParam("id") Long id) {
        knowledgeService.deleteKnowledge(id);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得知识库的精简列表")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<List<AiKnowledgeRespVO>> getKnowledgeSimpleList() {
        List<AiKnowledgeDO> list = knowledgeService.getKnowledgeSimpleListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, knowledge -> new AiKnowledgeRespVO()
                .setId(knowledge.getId()).setName(knowledge.getName())));
    }

    @PostMapping("/vector-health-check")
    @Operation(summary = "知识库向量健康检查（DB 与 Qdrant 对账，可选自动修复）")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:update')")
    public CommonResult<AiVectorHealthReportRespVO> vectorHealthCheck(
            @RequestParam(value = "knowledgeId", required = false) Long knowledgeId,
            @RequestParam(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun) {
        AiVectorHealthReportBO report = vectorStoreHealthService.runHealthCheck(knowledgeId, documentId, dryRun);
        return success(BeanUtils.toBean(report, AiVectorHealthReportRespVO.class));
    }

    @PostMapping("/rag-eval")
    @Operation(summary = "RAG 检索测评（对指定知识库跑黄金集或自定义用例）")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<AiRagEvalReportRespVO> ragEval(
            @RequestParam("knowledgeId") Long knowledgeId,
            @RequestBody(required = false) List<AiRagEvalCaseReqVO> cases) {
        List<AiRagEvalCaseBO> caseBOs = convertList(cases, AiKnowledgeController::toEvalCaseBO);
        AiRagEvalReportBO report = ragEvalService.runLiveEval(knowledgeId, caseBOs);
        AiRagEvalReportRespVO resp = BeanUtils.toBean(report, AiRagEvalReportRespVO.class);
        resp.setPassRate(report.passRate());
        return success(resp);
    }

    @GetMapping("/rag-eval/live-cases")
    @Operation(summary = "获得在线 RAG 测评默认用例列表")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<List<AiRagEvalCaseReqVO>> getRagEvalLiveCases() {
        return success(convertList(ragEvalService.copyLiveCases(), AiKnowledgeController::toEvalCaseVO));
    }

    private static AiRagEvalCaseBO toEvalCaseBO(AiRagEvalCaseReqVO req) {
        AiRagEvalCaseBO bo = BeanUtils.toBean(req, AiRagEvalCaseBO.class);
        if (req.getExpectation() != null) {
            bo.setExpectation(BeanUtils.toBean(req.getExpectation(), AiRagEvalExpectationBO.class));
        }
        return bo;
    }

    private static AiRagEvalCaseReqVO toEvalCaseVO(AiRagEvalCaseBO bo) {
        AiRagEvalCaseReqVO vo = BeanUtils.toBean(bo, AiRagEvalCaseReqVO.class);
        if (bo.getExpectation() != null) {
            vo.setExpectation(BeanUtils.toBean(bo.getExpectation(), AiRagEvalCaseReqVO.Expectation.class));
        }
        return vo;
    }

}
