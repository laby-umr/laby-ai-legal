package com.laby.module.infra.api.file;

import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 文件 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public String createFile(byte[] content, String name, String directory, String type) {
        return fileService.createFile(content, name, directory, type);
    }

    @Override
    public Long createFileReturnId(byte[] content, String name, String directory, String type) {
        return fileService.createFileReturnId(content, name, directory, type);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        return fileService.presignGetUrl(url, expirationSeconds);
    }

    @Override
    @SneakyThrows
    public byte[] getFileContent(Long id) {
        FileDO file = fileService.getFile(id);
        return fileService.getFileContent(file.getConfigId(), file.getPath());
    }

}
