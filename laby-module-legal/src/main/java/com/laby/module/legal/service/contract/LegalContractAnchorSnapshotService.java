package com.laby.module.legal.service.contract;

/**
 * 合同 WORKING 版锚点快照（设计 §3.2 / §4.2）。
 */
public interface LegalContractAnchorSnapshotService {

    /**
     * 按当前段落表为指定 WORKING 版本生成锚点快照（会先删除同 version 的旧快照项）。
     *
     * @return 快照编号
     */
    Long createOrRefreshSnapshot(Long contractId, Long versionId);

}
