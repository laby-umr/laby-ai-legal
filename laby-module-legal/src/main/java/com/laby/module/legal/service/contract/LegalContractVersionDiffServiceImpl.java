package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionDiffItemVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionDiffRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractVersionMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser.StructureParseResult;
import com.laby.module.legal.service.contract.util.LegalContractTextDiffUtil;
import com.laby.module.legal.service.contract.util.LegalExceptionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

@Slf4j
@Service
public class LegalContractVersionDiffServiceImpl implements LegalContractVersionDiffService {

    private static final int TEXT_PREVIEW_MAX = 2000;

    @Resource
    private LegalContractVersionMapper versionMapper;
    @Resource
    private LegalContractService contractService;
    @Resource
    private FileService fileService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;

    @Override
    public LegalContractVersionDiffRespVO getVersionDiff(Long contractId, Long fromVersionId, Long toVersionId) {
        contractService.validateContractExists(contractId);
        LegalContractVersionDO fromVersion = validateVersion(contractId, fromVersionId);
        LegalContractVersionDO toVersion = validateVersion(contractId, toVersionId);

        List<LegalClauseUnitBO> fromClauses = parseClauses(fromVersion);
        List<LegalClauseUnitBO> toClauses = parseClauses(toVersion);

        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractId(contractId);
        List<LegalContractVersionDiffItemVO> diffs = buildDiffs(fromClauses, toClauses, opinions);

        LegalContractVersionDiffRespVO resp = new LegalContractVersionDiffRespVO();
        resp.setContractId(contractId);
        resp.setFromVersionId(fromVersionId);
        resp.setToVersionId(toVersionId);
        resp.setFromVersionNo(fromVersion.getVersionNo());
        resp.setToVersionNo(toVersion.getVersionNo());
        resp.setDiffs(diffs);
        return resp;
    }

    private LegalContractVersionDO validateVersion(Long contractId, Long versionId) {
        LegalContractVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getContractId(), contractId)) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return version;
    }

    private List<LegalClauseUnitBO> parseClauses(LegalContractVersionDO version) {
        byte[] bytes = readVersionBytes(version);
        try {
            StructureParseResult result = LegalContractStructureParser.parse(bytes);
            return result.getClauses() != null ? result.getClauses() : List.of();
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[parseClauses][versionId={}] 解析失败，降级空条款", version.getId(), ex);
            return List.of();
        }
    }

    private byte[] readVersionBytes(LegalContractVersionDO version) {
        if (version.getFileId() == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        try {
            FileDO file = fileService.getFile(version.getFileId());
            if (file == null) {
                throw exception(CONTRACT_FILE_NOT_EXISTS);
            }
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null || content.length == 0) {
                throw exception(CONTRACT_FILE_NOT_EXISTS);
            }
            return content;
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
    }

    private List<LegalContractVersionDiffItemVO> buildDiffs(List<LegalClauseUnitBO> fromClauses,
                                                            List<LegalClauseUnitBO> toClauses,
                                                            List<LegalAuditOpinionDO> opinions) {
        List<LegalContractVersionDiffItemVO> diffs = new ArrayList<>();
        int max = Math.max(CollUtil.size(fromClauses), CollUtil.size(toClauses));
        for (int i = 0; i < max; i++) {
            LegalClauseUnitBO from = i < fromClauses.size() ? fromClauses.get(i) : null;
            LegalClauseUnitBO to = i < toClauses.size() ? toClauses.get(i) : null;
            diffs.add(buildDiffItem(from, to, opinions));
        }
        return diffs.stream()
                .filter(item -> !"UNCHANGED".equals(item.getChangeType()))
                .collect(Collectors.toList());
    }

    private LegalContractVersionDiffItemVO buildDiffItem(LegalClauseUnitBO from, LegalClauseUnitBO to,
                                                       List<LegalAuditOpinionDO> opinions) {
        LegalContractVersionDiffItemVO item = new LegalContractVersionDiffItemVO();
        if (from == null && to != null) {
            item.setClauseId(to.getClauseId());
            item.setClauseTitle(StrUtil.blankToDefault(to.getTitle(), to.getClauseId()));
            item.setChangeType("ADDED");
            item.setAfterText(StrUtil.sub(to.getFullText(), 0, TEXT_PREVIEW_MAX));
            item.setRelatedOpinionIds(findOpinionIds(opinions, to.getParagraphIds()));
            return item;
        }
        if (from != null && to == null) {
            item.setClauseId(from.getClauseId());
            item.setClauseTitle(StrUtil.blankToDefault(from.getTitle(), from.getClauseId()));
            item.setChangeType("REMOVED");
            item.setBeforeText(StrUtil.sub(from.getFullText(), 0, TEXT_PREVIEW_MAX));
            item.setRelatedOpinionIds(findOpinionIds(opinions, from.getParagraphIds()));
            return item;
        }
        if (from != null) {
            item.setClauseId(StrUtil.blankToDefault(to.getClauseId(), from.getClauseId()));
            item.setClauseTitle(StrUtil.blankToDefault(
                    StrUtil.blankToDefault(to.getTitle(), from.getTitle()), item.getClauseId()));
            if (LegalContractTextDiffUtil.isSameText(from.getFullText(), to.getFullText())) {
                item.setChangeType("UNCHANGED");
            } else {
                item.setChangeType("MODIFIED");
                item.setBeforeText(StrUtil.sub(from.getFullText(), 0, TEXT_PREVIEW_MAX));
                item.setAfterText(StrUtil.sub(to.getFullText(), 0, TEXT_PREVIEW_MAX));
            }
            List<String> paragraphIds = new ArrayList<>();
            if (CollUtil.isNotEmpty(from.getParagraphIds())) {
                paragraphIds.addAll(from.getParagraphIds());
            }
            if (CollUtil.isNotEmpty(to.getParagraphIds())) {
                paragraphIds.addAll(to.getParagraphIds());
            }
            item.setRelatedOpinionIds(findOpinionIds(opinions, paragraphIds));
        }
        return item;
    }

    private static List<Long> findOpinionIds(List<LegalAuditOpinionDO> opinions, List<String> paragraphIds) {
        if (CollUtil.isEmpty(opinions) || CollUtil.isEmpty(paragraphIds)) {
            return List.of();
        }
        return opinions.stream()
                .filter(o -> paragraphIds.contains(o.getParagraphId()))
                .map(LegalAuditOpinionDO::getId)
                .collect(Collectors.toList());
    }

}
