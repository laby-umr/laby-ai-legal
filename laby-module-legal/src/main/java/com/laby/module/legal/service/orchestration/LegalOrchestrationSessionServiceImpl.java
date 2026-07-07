package com.laby.module.legal.service.orchestration;



import cn.hutool.core.collection.CollUtil;

import com.laby.framework.common.util.json.JsonUtils;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;

import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;

import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;

import com.laby.module.legal.enums.ai.LegalAiPolicyConstants;

import com.laby.module.legal.enums.orchestration.LegalOrchestrationFileItemStatusEnum;

import com.laby.module.legal.enums.orchestration.LegalOrchestrationPhaseEnum;

import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.validation.annotation.Validated;



import java.util.ArrayList;

import java.util.List;



import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;

import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_SESSION_NOT_EXISTS;



@Service

@Validated

public class LegalOrchestrationSessionServiceImpl implements LegalOrchestrationSessionService {



    @Resource
    private LegalOrchestrationSessionMapper sessionMapper;

    @Resource
    private LegalOrchestrationFileItemMapper fileItemMapper;

    @Resource
    private LegalOrchestrationCheckpointService checkpointService;



    @Override

    @Transactional(rollbackFor = Exception.class)

    public LegalOrchestrationSessionDO getOrCreateSession(Long conversationId, Long userId, Long modelId) {

        if (modelId == null) {

            return getOrCreateSession(conversationId, userId, (LegalAiPolicyBO) null);

        }

        return getOrCreateSession(conversationId, userId, LegalAiPolicyBO.builder().modelId(modelId).build());

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public LegalOrchestrationSessionDO getOrCreateSession(Long conversationId, Long userId, LegalAiPolicyBO policy) {

        LegalOrchestrationSessionDO existing = sessionMapper.selectByConversationId(conversationId);

        if (existing != null) {

            if (policy != null) {

                mergePolicy(existing, policy);

            }

            return existing;

        }

        LegalOrchestrationSessionDO session = LegalOrchestrationSessionDO.builder()

                .conversationId(conversationId)

                .userId(userId)

                .modelId(policy != null ? policy.getModelId() : null)

                .partyRole(policy != null ? policy.getPartyRole() : LegalAiPolicyConstants.DEFAULT_PARTY_ROLE)

                .auditLevel(policy != null ? policy.getAuditLevel() : LegalAiPolicyConstants.DEFAULT_AUDIT_LEVEL)

                .auditRoleId(policy != null ? policy.getAuditRoleId() : null)

                .policyJson(policy != null ? JsonUtils.toJsonString(policy) : null)

                .phase(LegalOrchestrationPhaseEnum.INIT.getPhase())

                .build();

        sessionMapper.insert(session);

        return session;

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void syncPolicy(Long sessionId, LegalAiPolicyBO policy) {

        LegalOrchestrationSessionDO session = validateSessionExists(sessionId);

        mergePolicy(session, policy);

    }



    @Override

    public LegalOrchestrationSessionDO validateSessionExists(Long sessionId) {

        LegalOrchestrationSessionDO session = sessionMapper.selectById(sessionId);

        if (session == null) {

            throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);

        }

        return session;

    }



    @Override

    public LegalOrchestrationSessionDO getByConversationId(Long conversationId) {

        return sessionMapper.selectByConversationId(conversationId);

    }



    @Override

    public void updatePhase(Long sessionId, String phase) {

        LegalOrchestrationSessionDO session = validateSessionExists(sessionId);

        checkpointService.savePhaseCheckpoint(session, phase);

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public List<LegalOrchestrationFileItemDO> registerFiles(Long sessionId, List<Long> infraFileIds,

                                                            List<String> fileNames) {

        validateSessionExists(sessionId);

        if (CollUtil.isEmpty(infraFileIds)) {

            return List.of();

        }

        List<LegalOrchestrationFileItemDO> existing = fileItemMapper.selectListBySessionId(sessionId);

        int sortBase = existing.size();

        List<LegalOrchestrationFileItemDO> created = new ArrayList<>();

        for (int i = 0; i < infraFileIds.size(); i++) {

            Long fileId = infraFileIds.get(i);

            String fileName = i < fileNames.size() ? fileNames.get(i) : "file-" + fileId;

            LegalOrchestrationFileItemDO item = LegalOrchestrationFileItemDO.builder()

                    .sessionId(sessionId)

                    .infraFileId(fileId)

                    .fileName(fileName)

                    .status(LegalOrchestrationFileItemStatusEnum.REGISTERED.getStatus())

                    .sort(sortBase + i)

                    .build();

            fileItemMapper.insert(item);

            created.add(item);

        }

        updatePhase(sessionId, LegalOrchestrationPhaseEnum.FILE_REGISTERED.getPhase());

        return created;

    }



    @Override

    public List<LegalOrchestrationFileItemDO> listFileItems(Long sessionId) {

        validateSessionExists(sessionId);

        return fileItemMapper.selectListBySessionId(sessionId);

    }



    private void mergePolicy(LegalOrchestrationSessionDO existing, LegalAiPolicyBO policy) {

        LegalOrchestrationSessionDO update = new LegalOrchestrationSessionDO().setId(existing.getId());

        boolean needUpdate = false;



        if (policy.getModelId() != null && !policy.getModelId().equals(existing.getModelId())) {

            update.setModelId(policy.getModelId());

            existing.setModelId(policy.getModelId());

            needUpdate = true;

        }

        if (policy.getPartyRole() != null && !policy.getPartyRole().equals(existing.getPartyRole())) {

            update.setPartyRole(policy.getPartyRole());

            existing.setPartyRole(policy.getPartyRole());

            needUpdate = true;

        }

        if (policy.getAuditLevel() != null && !policy.getAuditLevel().equals(existing.getAuditLevel())) {

            update.setAuditLevel(policy.getAuditLevel());

            existing.setAuditLevel(policy.getAuditLevel());

            needUpdate = true;

        }

        if (policy.getAuditRoleId() != null && !policy.getAuditRoleId().equals(existing.getAuditRoleId())) {

            update.setAuditRoleId(policy.getAuditRoleId());

            existing.setAuditRoleId(policy.getAuditRoleId());

            needUpdate = true;

        }

        if (policy.getModelId() != null) {

            update.setPolicyJson(JsonUtils.toJsonString(policy));

            existing.setPolicyJson(update.getPolicyJson());

            needUpdate = true;

        }

        if (needUpdate) {

            sessionMapper.updateById(update);

        }

    }



}

