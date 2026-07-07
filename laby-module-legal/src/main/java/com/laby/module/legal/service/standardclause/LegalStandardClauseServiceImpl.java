package com.laby.module.legal.service.standardclause;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClausePageReqVO;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClauseSaveReqVO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.dal.mysql.standardclause.LegalStandardClauseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import com.laby.module.legal.enums.standardclause.LegalStandardClauseScopeEnum;

import static com.laby.module.legal.enums.ErrorCodeConstants.STANDARD_CLAUSE_CATEGORY_SCOPE_INVALID;
import static com.laby.module.legal.enums.ErrorCodeConstants.STANDARD_CLAUSE_NAME_DUPLICATE;
import static com.laby.module.legal.enums.ErrorCodeConstants.STANDARD_CLAUSE_NOT_EXISTS;

/**
 * 法务标准条款 Service 实现类
 */
@Service
@Validated
public class LegalStandardClauseServiceImpl implements LegalStandardClauseService {

    @Resource
    private LegalStandardClauseMapper standardClauseMapper;

    @Override
    public Long createStandardClause(LegalStandardClauseSaveReqVO createReqVO) {
        validateStandardClauseNameUnique(null, createReqVO.getName());
        validateCategoryScope(createReqVO.getCategoryScope());
        LegalStandardClauseDO entity = BeanUtils.toBean(createReqVO, LegalStandardClauseDO.class);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getCategoryScope() == null) {
            entity.setCategoryScope("COMMON");
        }
        standardClauseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateStandardClause(LegalStandardClauseSaveReqVO updateReqVO) {
        validateStandardClauseExists(updateReqVO.getId());
        validateStandardClauseNameUnique(updateReqVO.getId(), updateReqVO.getName());
        validateCategoryScope(updateReqVO.getCategoryScope());
        standardClauseMapper.updateById(BeanUtils.toBean(updateReqVO, LegalStandardClauseDO.class));
    }

    @Override
    public void deleteStandardClause(Long id) {
        validateStandardClauseExists(id);
        standardClauseMapper.deleteById(id);
    }

    @Override
    public void deleteStandardClauseList(List<Long> ids) {
        validateStandardClauseExists(ids);
        standardClauseMapper.deleteByIds(ids);
    }

    @Override
    public LegalStandardClauseDO getStandardClause(Long id) {
        return standardClauseMapper.selectById(id);
    }

    @Override
    public LegalStandardClauseDO validateStandardClauseExists(Long id) {
        LegalStandardClauseDO entity = standardClauseMapper.selectById(id);
        if (entity == null) {
            throw exception(STANDARD_CLAUSE_NOT_EXISTS);
        }
        return entity;
    }

    private void validateStandardClauseExists(List<Long> ids) {
        List<LegalStandardClauseDO> list = standardClauseMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list) || list.size() != ids.size()) {
            throw exception(STANDARD_CLAUSE_NOT_EXISTS);
        }
    }

    @Override
    public PageResult<LegalStandardClauseDO> getStandardClausePage(LegalStandardClausePageReqVO pageReqVO) {
        return standardClauseMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LegalStandardClauseDO> getStandardClauseSimpleList() {
        return standardClauseMapper.selectList(new LambdaQueryWrapperX<LegalStandardClauseDO>()
                .eq(LegalStandardClauseDO::getStatus, 0)
                .orderByAsc(LegalStandardClauseDO::getSort)
                .orderByDesc(LegalStandardClauseDO::getId));
    }

    @Override
    public Map<Long, LegalStandardClauseDO> getStandardClauseMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return standardClauseMapper.selectListByIds(ids).stream()
                .collect(Collectors.toMap(LegalStandardClauseDO::getId, Function.identity(), (a, b) -> a));
    }

    private void validateStandardClauseNameUnique(Long id, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        LegalStandardClauseDO exists = standardClauseMapper.selectByName(name);
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(STANDARD_CLAUSE_NAME_DUPLICATE);
        }
    }

    private void validateCategoryScope(String categoryScope) {
        if (StrUtil.isBlank(categoryScope)) {
            return;
        }
        if (!LegalStandardClauseScopeEnum.isValid(categoryScope)) {
            throw exception(STANDARD_CLAUSE_CATEGORY_SCOPE_INVALID);
        }
    }

}
