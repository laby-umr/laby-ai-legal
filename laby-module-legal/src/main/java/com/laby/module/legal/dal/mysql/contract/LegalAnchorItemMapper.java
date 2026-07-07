package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalAnchorItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalAnchorItemMapper extends BaseMapperX<LegalAnchorItemDO> {

    default List<LegalAnchorItemDO> selectListBySnapshotId(Long snapshotId) {
        return selectList(new LambdaQueryWrapperX<LegalAnchorItemDO>()
                .eq(LegalAnchorItemDO::getSnapshotId, snapshotId)
                .orderByAsc(LegalAnchorItemDO::getParagraphIndex));
    }

    default void deleteBySnapshotId(Long snapshotId) {
        delete(new LambdaQueryWrapperX<LegalAnchorItemDO>()
                .eq(LegalAnchorItemDO::getSnapshotId, snapshotId));
    }

}
