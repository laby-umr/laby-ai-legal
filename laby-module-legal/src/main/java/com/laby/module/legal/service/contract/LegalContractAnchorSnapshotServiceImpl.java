package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalAnchorItemDO;
import com.laby.module.legal.dal.dataobject.contract.LegalAnchorSnapshotDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import com.laby.module.legal.dal.mysql.contract.LegalAnchorItemMapper;
import com.laby.module.legal.dal.mysql.contract.LegalAnchorSnapshotMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractVersionMapper;
import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同 WORKING 版锚点快照 Service 实现类
 */
@Slf4j
@Service
public class LegalContractAnchorSnapshotServiceImpl implements LegalContractAnchorSnapshotService {

    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalAnchorSnapshotMapper anchorSnapshotMapper;
    @Resource
    private LegalAnchorItemMapper anchorItemMapper;
    @Resource
    private LegalContractVersionMapper contractVersionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrRefreshSnapshot(Long contractId, Long versionId) {
        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contractId);
        if (CollUtil.isEmpty(paragraphs)) {
            log.warn("[createOrRefreshSnapshot][contractId={} versionId={}] 无段落，跳过快照", contractId, versionId);
            return null;
        }
        paragraphs.sort(Comparator.comparing(LegalContractParagraphDO::getSort,
                Comparator.nullsLast(Integer::compareTo)));

        LegalContractVersionDO version = contractVersionMapper.selectById(versionId);
        Long existingSnapshotId = version != null ? version.getAnchorSnapshotId() : null;
        if (existingSnapshotId != null) {
            anchorItemMapper.deleteBySnapshotId(existingSnapshotId);
            String contentHash = buildContentHash(paragraphs);
            anchorSnapshotMapper.updateById(new LegalAnchorSnapshotDO()
                    .setId(existingSnapshotId)
                    .setContentHash(contentHash));
            insertAnchorItems(existingSnapshotId, paragraphs);
            log.info("[createOrRefreshSnapshot][contractId={} versionId={}] 已刷新快照 snapshotId={}",
                    contractId, versionId, existingSnapshotId);
            return existingSnapshotId;
        }

        String contentHash = buildContentHash(paragraphs);
        LegalAnchorSnapshotDO snapshot = LegalAnchorSnapshotDO.builder()
                .contractId(contractId)
                .versionId(versionId)
                .contentHash(contentHash)
                .build();
        anchorSnapshotMapper.insert(snapshot);
        insertAnchorItems(snapshot.getId(), paragraphs);
        contractVersionMapper.updateById(new LegalContractVersionDO()
                .setId(versionId)
                .setAnchorSnapshotId(snapshot.getId()));
        log.info("[createOrRefreshSnapshot][contractId={} versionId={}] 已创建快照 snapshotId={} items={}",
                contractId, versionId, snapshot.getId(), paragraphs.size());
        return snapshot.getId();
    }

    private void insertAnchorItems(Long snapshotId, List<LegalContractParagraphDO> paragraphs) {
        int index = 0;
        for (LegalContractParagraphDO paragraph : paragraphs) {
            if (StrUtil.isBlank(paragraph.getParagraphId())) {
                continue;
            }
            index++;
            String text = StrUtil.blankToDefault(paragraph.getText(), "");
            anchorItemMapper.insert(LegalAnchorItemDO.builder()
                    .snapshotId(snapshotId)
                    .anchorId(paragraph.getParagraphId())
                    .bookmarkName(LegalContractDocxRenderUtil.bookmarkNameForParagraph(paragraph.getParagraphId()))
                    .paragraphHash(DigestUtil.sha256Hex(text))
                    .paragraphIndex(index)
                    .path(paragraph.getPath())
                    .build());
        }
    }

    private static String buildContentHash(List<LegalContractParagraphDO> paragraphs) {
        String joined = paragraphs.stream()
                .map(p -> StrUtil.blankToDefault(p.getParagraphId(), "") + ":"
                        + StrUtil.blankToDefault(p.getText(), ""))
                .collect(Collectors.joining("\n"));
        return DigestUtil.sha256Hex(joined);
    }

}
