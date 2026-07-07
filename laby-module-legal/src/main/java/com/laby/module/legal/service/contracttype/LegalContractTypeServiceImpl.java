package com.laby.module.legal.service.contracttype;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypePageReqVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSaveReqVO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.mysql.contracttype.LegalContractTypeMapper;
import com.laby.module.legal.dal.mysql.skillpack.LegalSkillPackMapper;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_TYPE_CODE_DUPLICATE;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_TYPE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.SKILL_PACK_NOT_EXISTS;

/**
 * 法务合同类型 Service 实现类
 */
@Service
@Validated
public class LegalContractTypeServiceImpl implements LegalContractTypeService {

    @Resource
    private LegalContractTypeMapper contractTypeMapper;
    @Resource
    private LegalSkillPackMapper skillPackMapper;
    @Resource
    private AiKnowledgeService aiKnowledgeService;

    @Override
    public Long createContractType(LegalContractTypeSaveReqVO createReqVO) {
        validateContractTypeCodeUnique(null, createReqVO.getCode());
        validateSkillPackRefs(createReqVO);
        LegalContractTypeDO entity = BeanUtils.toBean(createReqVO, LegalContractTypeDO.class);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        contractTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateContractType(LegalContractTypeSaveReqVO updateReqVO) {
        validateContractTypeExists(updateReqVO.getId());
        validateContractTypeCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        validateSkillPackRefs(updateReqVO);
        contractTypeMapper.updateById(BeanUtils.toBean(updateReqVO, LegalContractTypeDO.class));
    }

    @Override
    public void deleteContractType(Long id) {
        validateContractTypeExists(id);
        contractTypeMapper.deleteById(id);
    }

    @Override
    public void deleteContractTypeList(List<Long> ids) {
        validateContractTypeExists(ids);
        contractTypeMapper.deleteByIds(ids);
    }

    @Override
    public LegalContractTypeDO getContractType(Long id) {
        return contractTypeMapper.selectById(id);
    }

    @Override
    public LegalContractTypeDO validateContractTypeExists(Long id) {
        LegalContractTypeDO entity = contractTypeMapper.selectById(id);
        if (entity == null) {
            throw exception(CONTRACT_TYPE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateContractTypeExists(List<Long> ids) {
        List<LegalContractTypeDO> list = contractTypeMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list) || list.size() != ids.size()) {
            throw exception(CONTRACT_TYPE_NOT_EXISTS);
        }
    }

    @Override
    public PageResult<LegalContractTypeDO> getContractTypePage(LegalContractTypePageReqVO pageReqVO) {
        return contractTypeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LegalContractTypeDO> getContractTypeSimpleList() {
        return contractTypeMapper.selectListEnabled();
    }

    private void validateContractTypeCodeUnique(Long id, String code) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        LegalContractTypeDO exists = contractTypeMapper.selectByCode(code);
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(CONTRACT_TYPE_CODE_DUPLICATE);
        }
    }

    private void validateSkillPackRefs(LegalContractTypeSaveReqVO reqVO) {
        validateKnowledgeId(reqVO.getKnowledgeId());
        validateSkillPackExists(reqVO.getDefaultSkillPackIdAudit());
        validateSkillPackExists(reqVO.getDefaultSkillPackIdChat());
    }

    private void validateKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        aiKnowledgeService.validateKnowledgeExists(knowledgeId);
    }

    private void validateSkillPackExists(Long skillPackId) {
        if (skillPackId == null) {
            return;
        }
        if (skillPackMapper.selectById(skillPackId) == null) {
            throw exception(SKILL_PACK_NOT_EXISTS);
        }
    }

}
