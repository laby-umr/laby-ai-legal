package com.laby.module.ai.controller.admin.knowledge;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.collection.MapUtils;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.*;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.framework.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.laby.module.ai.framework.knowledge.retrieval.RecallDiagnosticsBuilder;
import com.laby.module.ai.service.knowledge.AiKnowledgeDocumentService;
import com.laby.module.ai.service.knowledge.AiKnowledgeSegmentService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchResultBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.URL;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - AI 知识库段落")
@RestController
@RequestMapping("/ai/knowledge/segment")
@Validated
public class AiKnowledgeSegmentController {

    @Resource
    private AiKnowledgeSegmentService segmentService;
    @Resource
    private AiKnowledgeDocumentService documentService;
    @Resource
    private KnowledgeRetrievalProperties knowledgeRetrievalProperties;

    @GetMapping("/get")
    @Operation(summary = "获取段落详情")
    @Parameter(name = "id", description = "段落编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<AiKnowledgeSegmentRespVO> getKnowledgeSegment(@RequestParam("id") Long id) {
        AiKnowledgeSegmentDO segment = segmentService.getKnowledgeSegment(id);
        return success(BeanUtils.toBean(segment, AiKnowledgeSegmentRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取段落分页")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<PageResult<AiKnowledgeSegmentRespVO>> getKnowledgeSegmentPage(
            @Valid AiKnowledgeSegmentPageReqVO pageReqVO) {
        PageResult<AiKnowledgeSegmentDO> pageResult = segmentService.getKnowledgeSegmentPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AiKnowledgeSegmentRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建段落")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:create')")
    public CommonResult<Long> createKnowledgeSegment(@Valid @RequestBody AiKnowledgeSegmentSaveReqVO createReqVO) {
        return success(segmentService.createKnowledgeSegment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新段落内容")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:update')")
    public CommonResult<Boolean> updateKnowledgeSegment(@Valid @RequestBody AiKnowledgeSegmentSaveReqVO reqVO) {
        segmentService.updateKnowledgeSegment(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "启禁用段落内容")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:update')")
    public CommonResult<Boolean> updateKnowledgeSegmentStatus(
            @Valid @RequestBody AiKnowledgeSegmentUpdateStatusReqVO reqVO) {
        segmentService.updateKnowledgeSegmentStatus(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除段落")
    @Parameter(name = "id", description = "段落编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:delete')")
    public CommonResult<Boolean> deleteKnowledgeSegment(@RequestParam("id") Long id) {
        segmentService.deleteKnowledgeSegment(id);
        return success(true);
    }

    @GetMapping("/split")
    @Operation(summary = "切片内容")
    @Parameters({
            @Parameter(name = "url", description = "文档 URL", required = true),
            @Parameter(name = "segmentMaxTokens", description = "分段的最大 Token 数", required = true)
    })
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<List<AiKnowledgeSegmentRespVO>> splitContent(
            @RequestParam("url") @URL String url,
            @RequestParam(value = "segmentMaxTokens") Integer segmentMaxTokens) {
        List<AiKnowledgeSegmentDO> segments = segmentService.splitContent(url, segmentMaxTokens);
        return success(BeanUtils.toBean(segments, AiKnowledgeSegmentRespVO.class));
    }

    @GetMapping("/get-process-list")
    @Operation(summary = "获取文档处理列表")
    @Parameter(name = "documentIds", description = "文档编号列表", required = true, example = "1,2,3")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<List<AiKnowledgeSegmentProcessRespVO>> getKnowledgeSegmentProcessList(
            @RequestParam("documentIds") List<Long> documentIds) {
        List<AiKnowledgeSegmentProcessRespVO> list = segmentService.getKnowledgeSegmentProcessList(documentIds);
        return success(list);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索段落内容")
    @PreAuthorize("@ss.hasPermission('ai:knowledge:query')")
    public CommonResult<AiKnowledgeSegmentSearchResultVO> searchKnowledgeSegment(
            @Valid AiKnowledgeSegmentSearchReqVO reqVO) {
        AiKnowledgeSegmentSearchResultBO searchResult = segmentService.searchKnowledgeSegmentWithDiagnostics(
                BeanUtils.toBean(reqVO, AiKnowledgeSegmentSearchReqBO.class));

        AiKnowledgeSegmentSearchResultVO result = new AiKnowledgeSegmentSearchResultVO();
        if (knowledgeRetrievalProperties.getDiagnostics().isEnabled()) {
            result.setDiagnostics(RecallDiagnosticsBuilder.toVo(searchResult.getDiagnostics()));
        }

        List<AiKnowledgeSegmentSearchRespBO> segments = searchResult.getSegments();
        if (CollUtil.isEmpty(segments)) {
            return success(result);
        }
        Map<Long, AiKnowledgeDocumentDO> documentMap = documentService.getKnowledgeDocumentMap(convertSet(
                segments, AiKnowledgeSegmentSearchRespBO::getDocumentId));
        result.setSegments(BeanUtils.toBean(segments, AiKnowledgeSegmentSearchRespVO.class,
                segment -> MapUtils.findAndThen(documentMap, segment.getDocumentId(),
                        document -> segment.setDocumentName(document.getName()))));
        return success(result);
    }

}
