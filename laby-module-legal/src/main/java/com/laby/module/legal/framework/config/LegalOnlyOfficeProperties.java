package com.laby.module.legal.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice Document Server 集成配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "laby.legal.onlyoffice")
public class LegalOnlyOfficeProperties {

    /**
     * 是否启用 OnlyOffice 预览（未部署 DS 时保持 false）
     */
    private Boolean enabled = false;

    /**
     * Document Server 根地址，如 https://onlyoffice.example.com/
     */
    private String documentServerUrl = "";

    /**
     * 与 DS 一致的 JWT 密钥
     */
    private String jwtSecret = "";

    /**
     * DS 拉取合同文件的 API 根地址（外网/DS 可达），如 https://api.example.com/admin-api
     */
    private String callbackBaseUrl = "";

    /**
     * 浏览器加载 OnlyOffice 插件的 API 根地址（须用户浏览器可达）。
     * 未配置时从当前请求或 callbackBaseUrl 自动推导（host.docker.internal → 127.0.0.1）。
     */
    private String pluginBaseUrl = "";

    /**
     * 定位插件挂载在 Document Server sdkjs-plugins 目录（docker volume）。
     * true 时 pluginsData 指向 document-server-url，社区版下比远程 API 插件更稳定。
     */
    private Boolean pluginMountOnDocumentServer = true;

    /**
     * 文件拉取短时令牌有效期（分钟）
     */
    private Integer fileTokenTtlMinutes = 15;

    /**
     * 编辑器默认语言
     */
    private String editorLang = "zh-CN";

    /**
     * 默认可编辑模式：edit / view（有权限时生效）
     */
    private String defaultMode = "edit";

    /**
     * PDF 是否尝试 edit 模式（false 时仅 view；依赖 DS 版本与 license）
     */
    private Boolean pdfEditEnabled = false;

}
