package com.laby.module.legal.service.orchestration;



import cn.hutool.core.collection.CollUtil;

import com.laby.framework.common.util.json.JsonUtils;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;

import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;

import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;

import com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum;

import com.laby.module.legal.enums.orchestration.LegalOrchestrationFileItemStatusEnum;

import com.laby.module.legal.enums.orchestration.LegalOrchestrationPhaseEnum;

import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;

import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;

import com.laby.module.legal.service.contract.LegalContractService;

import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



import java.util.ArrayList;

import java.util.List;

import java.util.Map;



import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;

import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_TYPE_NOT_RESOLVED;



/**

 * 编排提案：批量创建合同执行器

 */

@Component

public class LegalOrchestrationContractCreateExecutor {



    @Resource

    private LegalOrchestrationSessionService sessionService;

    @Resource

    private LegalOrchestrationFileItemMapper fileItemMapper;

    @Resource

    private LegalContractService contractService;

    @Resource

    private LegalAiPolicyResolver policyResolver;

    @Resource

    private LegalSkillPackSnapshotService skillPackSnapshotService;



    @Transactional(rollbackFor = Exception.class)

    public List<Long> executeCreateContractsBatch(LegalAgentProposalDO proposal) {

        Map<?, ?> payload = JsonUtils.parseObject(proposal.getPayloadJson(), Map.class);

        if (payload == null) {

            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);

        }

        Object sessionIdObj = payload.get("sessionId");

        if (sessionIdObj == null) {

            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);

        }

        Long sessionId = Long.parseLong(String.valueOf(sessionIdObj));

        LegalOrchestrationSessionDO session = sessionService.validateSessionExists(sessionId);

        LegalAiPolicyBO policy = policyResolver.resolveForExecute(payload, session);

        sessionService.syncPolicy(sessionId, policy);



        List<LegalOrchestrationFileItemDO> files = sessionService.listFileItems(sessionId);

        Boolean editable = payload.get("editable") instanceof Boolean b ? b : Boolean.TRUE;



        List<Long> contractIds = new ArrayList<>();

        for (LegalOrchestrationFileItemDO file : files) {

            Long typeId = file.getConfirmedTypeId();

            if (typeId == null) {

                throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);

            }

            LegalContractCreateReqVO createReqVO = buildCreateReq(file, typeId, policy, editable);

            Long contractId = contractService.createContractFromOrchestration(

                    proposal.getUserId(), createReqVO, LegalContractCreateSourceEnum.AI_CHAT.getSource(),

                    session.getConversationId());

            contractIds.add(contractId);

            fileItemMapper.updateById(new LegalOrchestrationFileItemDO()

                    .setId(file.getId())

                    .setContractId(contractId)

                    .setStatus(LegalOrchestrationFileItemStatusEnum.CREATED.getStatus()));

        }



        if (CollUtil.isNotEmpty(contractIds)) {

            sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.CONTRACTS_CREATED.getPhase());

        }

        return contractIds;

    }



    @Transactional(rollbackFor = Exception.class)

    public void executeClassifyConfirm(LegalAgentProposalDO proposal) {

        Map<?, ?> payload = JsonUtils.parseObject(proposal.getPayloadJson(), Map.class);

        if (payload == null) {

            return;

        }

        Object sessionIdObj = payload.get("sessionId");

        if (sessionIdObj == null) {

            return;

        }

        Long sessionId = Long.parseLong(String.valueOf(sessionIdObj));

        Object mappingsObj = payload.get("mappings");

        if (!(mappingsObj instanceof List<?> mappings)) {

            return;

        }

        for (Object mappingObj : mappings) {

            if (!(mappingObj instanceof Map<?, ?> mapping)) {

                continue;

            }

            Object fileItemIdObj = mapping.get("fileItemId");

            Object typeIdObj = mapping.get("typeId");

            if (fileItemIdObj == null || typeIdObj == null) {

                continue;

            }

            Long fileItemId = Long.parseLong(String.valueOf(fileItemIdObj));

            Long typeId = Long.parseLong(String.valueOf(typeIdObj));

            fileItemMapper.updateById(new LegalOrchestrationFileItemDO()

                    .setId(fileItemId)

                    .setConfirmedTypeId(typeId)

                    .setStatus(LegalOrchestrationFileItemStatusEnum.MAPPED.getStatus()));

        }

        sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.CLASSIFY_CONFIRMED.getPhase());

    }



    private LegalContractCreateReqVO buildCreateReq(LegalOrchestrationFileItemDO file, Long typeId,

                                                  LegalAiPolicyBO policy, Boolean editable) {

        LegalContractCreateReqVO createReqVO = new LegalContractCreateReqVO();

        String title = cn.hutool.core.util.StrUtil.subBefore(file.getFileName(), ".", true);

        createReqVO.setTitle(cn.hutool.core.util.StrUtil.isNotBlank(title) ? title : file.getFileName());

        createReqVO.setContractTypeId(typeId);

        createReqVO.setPartyRole(policy.getPartyRole());

        createReqVO.setAuditLevel(policy.getAuditLevel());

        createReqVO.setModelId(policy.getModelId());

        createReqVO.setAuditRoleId(policy.getAuditRoleId());

        createReqVO.setReauditRoleId(policy.getReauditRoleId());

        createReqVO.setEditable(editable);

        createReqVO.setSkillPackSnapshotJson(
                skillPackSnapshotService.resolveSnapshotForCreate(typeId, policy));



        LegalContractCreateReqVO.LegalContractFileItemVO fileItem = new LegalContractCreateReqVO.LegalContractFileItemVO();

        fileItem.setFileId(file.getInfraFileId());

        fileItem.setFileName(file.getFileName());

        fileItem.setMainFlag(true);

        createReqVO.setFiles(List.of(fileItem));

        return createReqVO;

    }



}

