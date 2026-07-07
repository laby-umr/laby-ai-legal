package com.laby.module.legal.service.auditrule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRulePageReqVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleRespVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleSaveReqVO;
import com.laby.module.legal.dal.dataobject.auditrule.LegalAuditRuleDO;
import com.laby.module.legal.enums.auditrule.LegalAuditRuleTypeEnum;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.dal.mysql.auditrule.LegalAuditRuleMapper;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
import com.laby.module.legal.service.standardclause.LegalStandardClauseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.AUDIT_RULE_NAME_DUPLICATE;
import static com.laby.module.legal.enums.ErrorCodeConstants.AUDIT_RULE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.AUDIT_RULE_PREFERRED_REQUIRES_STANDARD_CLAUSE;
import static com.laby.module.legal.enums.ErrorCodeConstants.AUDIT_RULE_TYPE_INVALID;

/**
 * 法务审核规则 Service 实现类
 */
@Service
@Validated
public class LegalAuditRuleServiceImpl implements LegalAuditRuleService {

    @Resource
    private LegalAuditRuleMapper auditRuleMapper;
    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private LegalStandardClauseService standardClauseService;

    @Override
    public Long createAuditRule(LegalAuditRuleSaveReqVO createReqVO) {
        validateAuditRuleNameUnique(null, createReqVO.getName());
        validateRuleBusinessRules(createReqVO);
        LegalAuditRuleDO entity = toEntity(createReqVO);
        auditRuleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateAuditRule(LegalAuditRuleSaveReqVO updateReqVO) {
        validateAuditRuleExists(updateReqVO.getId());
        validateAuditRuleNameUnique(updateReqVO.getId(), updateReqVO.getName());
        validateRuleBusinessRules(updateReqVO);
        auditRuleMapper.updateById(toEntity(updateReqVO));
    }

    @Override
    public void updateAuditRuleEnabled(Long id, Boolean enabled) {
        validateAuditRuleExists(id);
        auditRuleMapper.updateById(new LegalAuditRuleDO().setId(id).setEnabled(enabled));
    }

    @Override
    public void deleteAuditRule(Long id) {
        validateAuditRuleExists(id);
        auditRuleMapper.deleteById(id);
    }

    @Override
    public void deleteAuditRuleList(List<Long> ids) {
        validateAuditRuleExists(ids);
        auditRuleMapper.deleteByIds(ids);
    }

    @Override
    public LegalAuditRuleRespVO getAuditRule(Long id) {
        LegalAuditRuleDO rule = validateAuditRuleExists(id);
        return convertList(List.of(rule)).get(0);
    }

    @Override
    public PageResult<LegalAuditRuleRespVO> getAuditRulePage(LegalAuditRulePageReqVO pageReqVO) {
        PageResult<LegalAuditRuleDO> pageResult = auditRuleMapper.selectPage(pageReqVO);
        return new PageResult<>(convertList(pageResult.getList()), pageResult.getTotal());
    }

    private LegalAuditRuleDO validateAuditRuleExists(Long id) {
        LegalAuditRuleDO rule = auditRuleMapper.selectById(id);
        if (rule == null) {
            throw exception(AUDIT_RULE_NOT_EXISTS);
        }
        return rule;
    }

    private void validateAuditRuleExists(List<Long> ids) {
        List<LegalAuditRuleDO> list = auditRuleMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list) || list.size() != ids.size()) {
            throw exception(AUDIT_RULE_NOT_EXISTS);
        }
    }

    private void validateAuditRuleNameUnique(Long id, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        LegalAuditRuleDO exists = auditRuleMapper.selectByName(name);
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(AUDIT_RULE_NAME_DUPLICATE);
        }
    }

    /**
     * CFG-001：推荐标准条款规则必须关联标准条款，避免 ruleContent 双写条款正文。
     */
    static void validateRuleBusinessRules(LegalAuditRuleSaveReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        LegalAuditRuleTypeEnum ruleType = resolveRuleTypeForSave(reqVO.getRuleType());
        if (ruleType == LegalAuditRuleTypeEnum.PREFERRED_CLAUSE && reqVO.getStandardClauseId() == null) {
            throw exception(AUDIT_RULE_PREFERRED_REQUIRES_STANDARD_CLAUSE);
        }
    }

    /**
     * 保存时严格解析 ruleType，禁止 typo 被静默降级为 CUSTOM_LLM。
     */
    static LegalAuditRuleTypeEnum resolveRuleTypeForSave(String code) {
        if (StrUtil.isBlank(code)) {
            return LegalAuditRuleTypeEnum.CUSTOM_LLM;
        }
        for (LegalAuditRuleTypeEnum item : LegalAuditRuleTypeEnum.values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        throw exception(AUDIT_RULE_TYPE_INVALID);
    }

    private LegalAuditRuleDO toEntity(LegalAuditRuleSaveReqVO reqVO) {
        LegalAuditRuleDO entity = BeanUtils.toBean(reqVO, LegalAuditRuleDO.class);
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        if (entity.getPriority() == null) {
            entity.setPriority(0);
        }
        if (reqVO.getStandardClauseId() != null) {
            standardClauseService.validateStandardClauseExists(reqVO.getStandardClauseId());
        }
        if (reqVO.getContractTypeId() != null) {
            contractTypeService.validateContractTypeExists(reqVO.getContractTypeId());
        }
        return entity;
    }

    private List<LegalAuditRuleRespVO> convertList(List<LegalAuditRuleDO> list) {
        if (CollUtil.isEmpty(list)) {
            return List.of();
        }
        Set<Long> typeIds = list.stream().map(LegalAuditRuleDO::getContractTypeId)
                .filter(id -> id != null).collect(Collectors.toSet());
        Set<Long> clauseIds = list.stream().map(LegalAuditRuleDO::getStandardClauseId)
                .filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, LegalContractTypeDO> typeMap = typeIds.stream()
                .map(contractTypeService::getContractType)
                .filter(t -> t != null)
                .collect(Collectors.toMap(LegalContractTypeDO::getId, t -> t, (a, b) -> a));
        Map<Long, LegalStandardClauseDO> clauseMap = standardClauseService.getStandardClauseMap(clauseIds);
        return BeanUtils.toBean(list, LegalAuditRuleRespVO.class, vo -> {
            if (vo.getContractTypeId() != null) {
                LegalContractTypeDO type = typeMap.get(vo.getContractTypeId());
                if (type != null) {
                    vo.setContractTypeName(type.getName());
                }
            }
            if (vo.getStandardClauseId() != null) {
                LegalStandardClauseDO clause = clauseMap.get(vo.getStandardClauseId());
                if (clause != null) {
                    vo.setStandardClauseName(clause.getName());
                }
            }
        });
    }

}
