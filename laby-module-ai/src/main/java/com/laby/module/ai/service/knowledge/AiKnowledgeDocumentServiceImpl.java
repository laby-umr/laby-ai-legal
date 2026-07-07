package com.laby.module.ai.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.http.HttpUtils;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentCreateListReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentPageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentUpdateReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentUpdateStatusReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgeDocumentCreateReqVO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeDocumentMapper;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentIngestStatusEnum;
import com.laby.module.ai.enums.knowledge.AiKnowledgeDocumentTypeEnum;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.core.token.AiTokenCounter;
import com.laby.module.ai.framework.document.DocumentParseService;
import com.laby.module.ai.framework.document.DocumentTypeResolver;
import com.laby.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.util.collection.CollectionUtils.convertList;
import static com.laby.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 知识库文档 Service 实现类
 *
 * @author xiaoxin
 */
@Service
@Slf4j
public class AiKnowledgeDocumentServiceImpl implements AiKnowledgeDocumentService {

    /** 纯文本扩展名：直接 UTF-8 读取，避免走外部解析服务 */
    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of(
            "md", "markdown", "txt", "json", "xml",
            "yaml", "yml", "log", "properties", "java", "py", "sql", "js", "ts", "css");

    @Resource
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private AiKnowledgeService knowledgeService;
    @Resource
    private FileService fileService;

    @Resource
    private DocumentParseService documentParseService;

    @Override
    public Long createKnowledgeDocument(AiKnowledgeDocumentCreateReqVO createReqVO) {
        // 1. 校验参数
        knowledgeService.validateKnowledgeExists(createReqVO.getKnowledgeId());
        validateDocumentUrlOnCreate(createReqVO.getKnowledgeId(), createReqVO.getUrl());

        // 2. 解析文档
        AiStructuredDocumentParseResult parseResult = parseDocumentUrl(createReqVO.getUrl());
        String content = parseResult.getMarkdown();
        String fileName = extractFileName(createReqVO.getUrl());
        AiKnowledgeDocumentTypeEnum documentType = DocumentTypeResolver.resolve(fileName, parseResult);

        // 3. 文档记录入库
        AiKnowledgeDocumentDO documentDO = BeanUtils.toBean(createReqVO, AiKnowledgeDocumentDO.class)
                .setContent(content).setContentLength(content.length()).setTokens(AiTokenCounter.estimate(content))
                .setParseEngine(parseResult.getEngine().getCode())
                .setParseQuality(parseResult.getQuality().getCode())
                .setDocumentType(documentType.getCode())
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setIngestStatus(AiKnowledgeDocumentIngestStatusEnum.PENDING.getStatus());
        knowledgeDocumentMapper.insert(documentDO);

        // 4. 文档切片入库（异步）
        knowledgeSegmentService.createKnowledgeSegmentByParseResultAsync(documentDO.getId(), parseResult);
        return documentDO.getId();
    }

    @Override
    public List<Long> createKnowledgeDocumentList(AiKnowledgeDocumentCreateListReqVO createListReqVO) {
        // 1. 校验参数
        knowledgeService.validateKnowledgeExists(createListReqVO.getKnowledgeId());
        validateDocumentUrlsOnCreate(createListReqVO.getKnowledgeId(),
                convertList(createListReqVO.getList(), AiKnowledgeDocumentCreateListReqVO.Document::getUrl));

        // 2. 逐文档解析并入库
        List<Long> documentIds = new ArrayList<>(createListReqVO.getList().size());
        for (AiKnowledgeDocumentCreateListReqVO.Document documentVO : createListReqVO.getList()) {
            AiStructuredDocumentParseResult parseResult = parseDocumentUrl(documentVO.getUrl());
            String content = parseResult.getMarkdown();
            String fileName = extractFileName(documentVO.getUrl());
            AiKnowledgeDocumentTypeEnum documentType = DocumentTypeResolver.resolve(fileName, parseResult);
            AiKnowledgeDocumentDO documentDO = BeanUtils.toBean(documentVO, AiKnowledgeDocumentDO.class)
                    .setKnowledgeId(createListReqVO.getKnowledgeId())
                    .setContent(content).setContentLength(content.length())
                    .setTokens(AiTokenCounter.estimate(content))
                    .setSegmentMaxTokens(createListReqVO.getSegmentMaxTokens())
                    .setParseEngine(parseResult.getEngine().getCode())
                    .setParseQuality(parseResult.getQuality().getCode())
                    .setDocumentType(documentType.getCode())
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setIngestStatus(AiKnowledgeDocumentIngestStatusEnum.PENDING.getStatus());
            knowledgeDocumentMapper.insert(documentDO);
            knowledgeSegmentService.createKnowledgeSegmentByParseResultAsync(documentDO.getId(), parseResult);
            documentIds.add(documentDO.getId());
        }
        return documentIds;
    }

    @Override
    public PageResult<AiKnowledgeDocumentDO> getKnowledgeDocumentPage(AiKnowledgeDocumentPageReqVO pageReqVO) {
        return knowledgeDocumentMapper.selectPage(pageReqVO);
    }

    @Override
    public AiKnowledgeDocumentDO getKnowledgeDocument(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    @Override
    public void updateKnowledgeDocument(AiKnowledgeDocumentUpdateReqVO reqVO) {
        // 1. 校验文档是否存在
        AiKnowledgeDocumentDO oldDocument = validateKnowledgeDocumentExists(reqVO.getId());

        // 2. 更新文档
        AiKnowledgeDocumentDO document = BeanUtils.toBean(reqVO, AiKnowledgeDocumentDO.class);
        knowledgeDocumentMapper.updateById(document);

        // 3. 如果处于开启状态，并且最大 tokens 发生变化，则 segment 需要重新索引
        if (CommonStatusEnum.isEnable(oldDocument.getStatus())
                && reqVO.getSegmentMaxTokens() != null
                && ObjUtil.notEqual(reqVO.getSegmentMaxTokens(), oldDocument.getSegmentMaxTokens())) {
            // 删除旧的文档切片
            knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(reqVO.getId());
            // 重新创建文档切片
            knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(reqVO.getId(), oldDocument.getContent());
        }
    }

    @Override
    public void updateKnowledgeDocumentStatus(AiKnowledgeDocumentUpdateStatusReqVO reqVO) {
        // 1. 校验存在
        AiKnowledgeDocumentDO document = validateKnowledgeDocumentExists(reqVO.getId());

        // 2. 更新状态
        knowledgeDocumentMapper.updateById(new AiKnowledgeDocumentDO()
                .setId(reqVO.getId()).setStatus(reqVO.getStatus()));

        // 3. 处理文档切片
        if (CommonStatusEnum.isEnable(reqVO.getStatus())) {
            knowledgeSegmentService.createKnowledgeSegmentBySplitContentAsync(reqVO.getId(), document.getContent());
        } else {
            knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(reqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeDocument(Long id) {
        // 1. 校验存在
        validateKnowledgeDocumentExists(id);

        // 2. 先删分段与向量（未完成向量化时 vector_id 为空，需跳过 Qdrant 删除）
        knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(id);

        // 3. 再删文档
        knowledgeDocumentMapper.deleteById(id);
    }

    @Override
    public void retryKnowledgeDocumentIngest(Long id) {
        AiKnowledgeDocumentDO document = validateKnowledgeDocumentExists(id);
        if (AiKnowledgeDocumentIngestStatusEnum.isRunning(document.getIngestStatus())) {
            throw exception(KNOWLEDGE_DOCUMENT_INGEST_RUNNING);
        }
        AiStructuredDocumentParseResult parseResult = reparseDocument(document);
        if (StrUtil.isBlank(parseResult.getMarkdown())) {
            throw exception(KNOWLEDGE_DOCUMENT_INGEST_CONTENT_EMPTY);
        }
        knowledgeSegmentService.deleteKnowledgeSegmentByDocumentId(id);
        String fileName = extractFileName(document.getUrl());
        AiKnowledgeDocumentTypeEnum documentType = DocumentTypeResolver.resolve(fileName, parseResult);
        knowledgeDocumentMapper.updateById(new AiKnowledgeDocumentDO()
                .setId(id)
                .setContent(parseResult.getMarkdown())
                .setContentLength(parseResult.getMarkdown().length())
                .setTokens(AiTokenCounter.estimate(parseResult.getMarkdown()))
                .setParseEngine(parseResult.getEngine().getCode())
                .setParseQuality(parseResult.getQuality().getCode())
                .setDocumentType(documentType.getCode()));
        updateKnowledgeDocumentIngestStatus(id, AiKnowledgeDocumentIngestStatusEnum.PENDING.getStatus(), null);
        knowledgeSegmentService.createKnowledgeSegmentByParseResultAsync(id, parseResult);
    }

    @Override
    public void updateKnowledgeDocumentIngestStatus(Long id, Integer status, String error) {
        AiKnowledgeDocumentDO update = new AiKnowledgeDocumentDO().setId(id).setIngestStatus(status);
        AiKnowledgeDocumentIngestStatusEnum statusEnum = AiKnowledgeDocumentIngestStatusEnum.valueOfStatus(status);
        if (statusEnum == AiKnowledgeDocumentIngestStatusEnum.FAILED) {
            update.setIngestError(StrUtil.sub(error, 0, 500));
        } else if (error == null) {
            update.setIngestError("");
        }
        if (statusEnum == AiKnowledgeDocumentIngestStatusEnum.PENDING
                || statusEnum == AiKnowledgeDocumentIngestStatusEnum.SPLITTING) {
            update.setIngestStartedAt(LocalDateTime.now());
            update.setIngestFinishedAt(null);
        }
        if (statusEnum == AiKnowledgeDocumentIngestStatusEnum.SUCCESS
                || statusEnum == AiKnowledgeDocumentIngestStatusEnum.FAILED) {
            update.setIngestFinishedAt(LocalDateTime.now());
        }
        knowledgeDocumentMapper.updateById(update);
    }

    @Override
    public AiKnowledgeDocumentDO validateKnowledgeDocumentExists(Long id) {
        AiKnowledgeDocumentDO knowledgeDocument = knowledgeDocumentMapper.selectById(id);
        if (knowledgeDocument == null) {
            throw exception(KNOWLEDGE_DOCUMENT_NOT_EXISTS);
        }
        return knowledgeDocument;
    }

    @Override
    public String readUrl(String url) {
        return parseDocumentUrl(url).getMarkdown();
    }

    @Override
    public AiStructuredDocumentParseResult parseDocumentUrl(String url) {
        byte[] bytes = downloadBytes(url);
        String fileName = extractFileName(url);
        if (isPlainTextExtension(fileName)) {
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (StrUtil.isBlank(text)) {
                throw exception(KNOWLEDGE_DOCUMENT_FILE_READ_FAIL);
            }
            return new AiStructuredDocumentParseResult().setMarkdown(text);
        }
        AiStructuredDocumentParseResult parseResult = documentParseService.parse(bytes, fileName);
        if (StrUtil.isBlank(parseResult.getMarkdown())) {
            throw exception(KNOWLEDGE_DOCUMENT_FILE_READ_FAIL);
        }
        return parseResult;
    }

    private AiStructuredDocumentParseResult reparseDocument(AiKnowledgeDocumentDO document) {
        if (StrUtil.isNotBlank(document.getUrl())) {
            try {
                return parseDocumentUrl(document.getUrl());
            } catch (Exception ex) {
                log.warn("[reparseDocument][documentId={}] 从 URL 重新解析失败，回退已存正文", document.getId(), ex);
            }
        }
        return new AiStructuredDocumentParseResult().setMarkdown(document.getContent());
    }

    private byte[] downloadBytes(String url) {
        try {
            byte[] bytes = fileService.getFileContentByUrl(url);
            if (bytes.length == 0) {
                throw exception(KNOWLEDGE_DOCUMENT_FILE_EMPTY);
            }
            return bytes;
        } catch (Exception e) {
            log.error("[downloadBytes][url({}) 读取失败]", url, e);
            throw exception(KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL);
        }
    }

    private static String extractFileName(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        return FileNameUtil.getName(StrUtil.subBefore(url, "?", false));
    }

    private static boolean isPlainTextExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return false;
        }
        String ext = FileNameUtil.extName(fileName);
        return StrUtil.isNotBlank(ext) && PLAIN_TEXT_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    /**
     * 创建文档前校验 URL 是否已在当前知识库存在（对齐芋道：失败走重新入库，不重复新建）
     */
    private void validateDocumentUrlOnCreate(Long knowledgeId, String url) {
        AiKnowledgeDocumentDO existing = getLatestDocumentByKnowledgeIdAndUrl(knowledgeId, url);
        if (existing == null) {
            return;
        }
        AiKnowledgeDocumentIngestStatusEnum ingestStatus =
                AiKnowledgeDocumentIngestStatusEnum.valueOfStatus(existing.getIngestStatus());
        if (ingestStatus == AiKnowledgeDocumentIngestStatusEnum.SUCCESS) {
            throw exception(KNOWLEDGE_DOCUMENT_URL_EXISTS, existing.getName());
        }
        if (AiKnowledgeDocumentIngestStatusEnum.isRunning(existing.getIngestStatus())) {
            throw exception(KNOWLEDGE_DOCUMENT_INGEST_RUNNING);
        }
        throw exception(KNOWLEDGE_DOCUMENT_URL_DUPLICATE_RETRY, existing.getName());
    }

    private void validateDocumentUrlsOnCreate(Long knowledgeId, List<String> urls) {
        Set<String> seenNormalizedUrls = new HashSet<>();
        for (String url : urls) {
            String normalizedUrl = normalizeDocumentUrl(url);
            if (!seenNormalizedUrls.add(normalizedUrl)) {
                throw exception(KNOWLEDGE_DOCUMENT_URL_BATCH_DUPLICATE);
            }
            validateDocumentUrlOnCreate(knowledgeId, url);
        }
    }

    private AiKnowledgeDocumentDO getLatestDocumentByKnowledgeIdAndUrl(Long knowledgeId, String url) {
        String cleanUrl = normalizeDocumentUrl(url);
        Set<String> urlCandidates = StrUtil.equals(cleanUrl, url) ? Set.of(url) : Set.of(url, cleanUrl);
        return knowledgeDocumentMapper.selectLatestByKnowledgeIdAndUrls(knowledgeId, urlCandidates);
    }

    private static String normalizeDocumentUrl(String url) {
        return HttpUtils.removeUrlQuery(url);
    }

    @Override
    public List<AiKnowledgeDocumentDO> getKnowledgeDocumentList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return knowledgeDocumentMapper.selectByIds(ids);
    }

    @Override
    public List<AiKnowledgeDocumentDO> getKnowledgeDocumentListByKnowledgeId(Long knowledgeId) {
        return knowledgeDocumentMapper.selectListByKnowledgeId(knowledgeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeDocumentByKnowledgeId(Long knowledgeId) {
        // 1. 获取该知识库下的所有文档
        List<AiKnowledgeDocumentDO> documents = knowledgeDocumentMapper.selectListByKnowledgeId(knowledgeId);
        if (CollUtil.isEmpty(documents)) {
            return;
        }

        // 2. 逐个删除文档及其对应的段落
        for (AiKnowledgeDocumentDO document : documents) {
            deleteKnowledgeDocument(document.getId());
        }
    }

}
