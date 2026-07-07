package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DOC 等格式转换（LibreOffice headless）
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.format-convert")
public class LegalFormatConvertProperties {

    /**
     * 是否尝试 DOC→DOCX（需本机安装 LibreOffice）
     */
    private Boolean libreOfficeEnabled = false;

    /**
     * soffice 可执行路径，默认从 PATH 查找
     */
    private String libreOfficePath = "soffice";

    /**
     * 转换超时（秒）
     */
    private Integer timeoutSeconds = 120;

}
