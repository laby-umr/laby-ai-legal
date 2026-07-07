package com.laby.module.legal.service.contract;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractOpinionCompleteReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphSkipReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractUploadRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalAuditReportRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 法务合同审核 Service 接口
 */
public interface LegalContractService {

    /**
     * 创建合同并发起审核流水线
     *
     * @param userId      当前用户编号
     * @param createReqVO 创建参数
     * @return 合同编号
     */
    Long createContract(Long userId, @Valid LegalContractCreateReqVO createReqVO);

    /**
     * AI 编排确认后创建合同
     *
     * @param createSource       创建来源，见 LegalContractCreateSourceEnum
     * @param createConversationId AI 对话编号
     */
    Long createContractFromOrchestration(Long userId, @Valid LegalContractCreateReqVO createReqVO,
                                         String createSource, Long createConversationId);

    /** 处理失败后重新执行解析+AI+发起流程 */
    void retryPipeline(Long userId, Long contractId);

    /**
     * 对已解析、尚未首轮 AI 审核的合同发起审核（AI 对话创建落库后使用）
     */
    void startFirstAudit(Long userId, Long contractId);

    /**
     * 更新合同状态
     *
     * @param id     合同编号
     * @param status 状态
     */
    void updateContractStatus(Long id, Integer status);

    /**
     * 更新 BPM 流程状态
     *
     * @param id        合同编号
     * @param bpmStatus BPM 状态
     */
    void updateBpmStatus(Long id, Integer bpmStatus);

    /**
     * 完成意见审核
     *
     * @param userId 当前用户编号
     * @param reqVO  完成参数
     */
    void completeOpinionReview(Long userId, @Valid LegalContractOpinionCompleteReqVO reqVO);

    /**
     * 校验当前是否允许处置意见（保存/二轮/采纳忽略等）
     */
    void validateOpinionManageable(Long contractId);

    /**
     * 更新段落跳过审核标记
     *
     * @param reqVO 更新参数
     */
    void updateParagraphSkipAudit(@Valid LegalContractParagraphSkipReqVO reqVO);

    /**
     * 获得合同
     *
     * @param id 合同编号
     * @return 合同
     */
    LegalContractDO getContract(Long id);

    /**
     * 获得合同详情（含附件、按钮权限与审核摘要）
     *
     * @param id 合同编号
     * @return 详情 VO；合同不存在时由 {@link #validateContractExists(Long)} 抛错
     */
    LegalContractRespVO getContractResp(Long id);

    /**
     * 获得合同审核分页（含列表展示字段）
     *
     * @param userId    当前用户编号
     * @param pageReqVO 分页参数
     * @return 分页结果
     */
    PageResult<LegalContractRespVO> getContractRespPage(Long userId, LegalContractPageReqVO pageReqVO);

    /**
     * 获得审核报告 Markdown；缺失时尝试按意见重建
     *
     * @param contractId 合同编号
     * @param auditRound 审核轮次，空则取合同当前轮次
     * @return 报告 VO，无内容时返回空对象
     */
    LegalAuditReportRespVO getAuditReportResp(Long contractId, Integer auditRound);

    /**
     * 获得合同版本列表；数据库未建表时降级为空列表
     *
     * @param contractId 合同编号
     * @return 版本列表
     */
    List<LegalContractVersionRespVO> getContractVersionRespList(Long contractId);

    /**
     * 按 Flowable 运行时状态刷新合同（列表/详情展示前调用）
     */
    LegalContractDO refreshAndGetContract(Long id);

    /**
     * 获得合同分页
     *
     * @param userId    当前用户编号
     * @param pageReqVO 分页参数
     * @return 合同分页
     */
    PageResult<LegalContractDO> getContractPage(Long userId, LegalContractPageReqVO pageReqVO);

    /**
     * 校验合同存在
     *
     * @param id 合同编号
     * @return 合同 DO
     */
    LegalContractDO validateContractExists(Long id);

    /**
     * 上传合同文件
     *
     * @param file 文件附件
     * @return 文件编号、名称与访问地址
     */
    LegalContractUploadRespVO uploadContractFile(MultipartFile file) throws Exception;

    /**
     * 获得合同段落列表（用于前端定位高亮）
     *
     * @param contractId 合同编号
     * @return 段落 VO 列表；合同不存在时抛错
     */
    List<LegalContractParagraphRespVO> listParagraphRespList(Long contractId);

    /**
     * 下载合同附件（校验合同归属与 infra 文件存储）
     *
     * @param fileId infra 文件编号（须已关联到合同附件表）
     * @return 展示文件名与文件内容
     */
    LegalContractFileDownloadBO downloadContractFile(Long fileId) throws Exception;

}
