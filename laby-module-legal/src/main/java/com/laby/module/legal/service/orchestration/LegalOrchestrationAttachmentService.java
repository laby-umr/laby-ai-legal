package com.laby.module.legal.service.orchestration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.dal.mysql.file.FileMapper;
import com.laby.module.legal.service.ai.LegalAiChatFacade;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationResolvedAttachmentBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 解析 AI 对话附件 URL → infra 文件编号
 */
@Service
public class LegalOrchestrationAttachmentService {

    @Resource
    private LegalAiChatFacade legalAiChatFacade;
    @Resource
    private FileMapper fileMapper;

    public List<LegalOrchestrationResolvedAttachmentBO> resolveLatestUserAttachments(Long conversationId) {
        List<String> urls = legalAiChatFacade.listLatestUserAttachmentUrls(conversationId);
        return resolveUrls(urls);
    }

    public List<LegalOrchestrationResolvedAttachmentBO> resolveUrls(List<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            return Collections.emptyList();
        }
        List<LegalOrchestrationResolvedAttachmentBO> result = new ArrayList<>();
        for (String url : urls) {
            if (StrUtil.isBlank(url)) {
                continue;
            }
            FileDO file = getLatestFileByUrl(url.trim());
            LegalOrchestrationResolvedAttachmentBO item = new LegalOrchestrationResolvedAttachmentBO();
            item.setUrl(url);
            item.setFileName(file != null && StrUtil.isNotBlank(file.getName())
                    ? file.getName() : FileNameUtil.getName(url));
            if (file != null) {
                item.setInfraFileId(file.getId());
            }
            result.add(item);
        }
        return result;
    }

    private FileDO getLatestFileByUrl(String url) {
        List<FileDO> list = fileMapper.selectList(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getUrl, url)
                .orderByDesc(FileDO::getId)
                .last("LIMIT 1"));
        return CollUtil.getFirst(list);
    }

}
