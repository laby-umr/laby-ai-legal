package com.laby.module.legal.service.orchestration;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSaveReqVO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationTypePackageDraftDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationTypePackageDraftMapper;
import com.laby.module.legal.enums.orchestration.LegalOrchestrationPhaseEnum;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_TYPE_NOT_RESOLVED;

@Service
public class LegalOrchestrationTypePackageService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    @Resource
    private LegalOrchestrationTypePackageDraftMapper draftMapper;
    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private LegalOrchestrationSessionService sessionService;

    public Long saveDraft(Long sessionId, Long userId, String name, String code,
                          String description, String contentJson) {
        LegalOrchestrationTypePackageDraftDO draft = LegalOrchestrationTypePackageDraftDO.builder()
                .sessionId(sessionId)
                .userId(userId)
                .name(name)
                .code(code)
                .description(description)
                .contentJson(contentJson)
                .status(STATUS_DRAFT)
                .build();
        draftMapper.insert(draft);
        sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.TYPE_PACKAGE_PENDING.getPhase());
        return draft.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishDraft(Long draftId) {
        LegalOrchestrationTypePackageDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);
        }
        LegalContractTypeSaveReqVO reqVO = new LegalContractTypeSaveReqVO();
        reqVO.setName(draft.getName());
        reqVO.setCode(draft.getCode());
        reqVO.setDescription(StrUtil.blankToDefault(draft.getDescription(), "AI 编排起草"));
        reqVO.setStatus(0);
        reqVO.setSort(0);
        Long typeId = contractTypeService.createContractType(reqVO);
        draftMapper.updateById(new LegalOrchestrationTypePackageDraftDO()
                .setId(draftId)
                .setStatus(STATUS_PUBLISHED)
                .setContractTypeId(typeId));
        sessionService.updatePhase(draft.getSessionId(), LegalOrchestrationPhaseEnum.CLASSIFY_CONFIRMED.getPhase());
        return typeId;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publishFromProposalPayload(Map<?, ?> payload) {
        Object draftIdObj = payload.get("draftId");
        if (draftIdObj == null) {
            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);
        }
        return publishDraft(Long.parseLong(String.valueOf(draftIdObj)));
    }

}
