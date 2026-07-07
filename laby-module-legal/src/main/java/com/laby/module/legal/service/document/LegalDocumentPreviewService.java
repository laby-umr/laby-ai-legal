package com.laby.module.legal.service.document;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.security.core.LoginUser;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import com.laby.module.legal.controller.admin.document.vo.LegalDocumentPreviewConfigRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.framework.config.LegalOnlyOfficeProperties;
import com.laby.module.legal.service.contract.LegalContractPermissionHelper;
import com.laby.module.legal.service.contract.LegalContractVersionService;
import com.laby.module.legal.service.contract.util.LegalContractFormatUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * OnlyOffice 文档编辑/预览配置
 */
@Slf4j
@Service
public class LegalDocumentPreviewService {

    /** 审阅正文定位插件（社区版无 createConnector，需 autostart 插件桥接 SearchNext） */
    private static final String LOCATE_PLUGIN_GUID = "asc.legal-locate-v3";

    @Resource
    private LegalOnlyOfficeProperties onlyOfficeProperties;
    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalOnlyOfficeJwtSigner jwtSigner;
    @Resource
    private LegalOnlyOfficeFileTokenService fileTokenService;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalAuditOpinionMapper auditOpinionMapper;

    public LegalDocumentPreviewConfigRespVO buildPreviewConfig(Long contractId) {
        LegalDocumentPreviewConfigRespVO resp = new LegalDocumentPreviewConfigRespVO();
        if (!Boolean.TRUE.equals(onlyOfficeProperties.getEnabled())) {
            resp.setEnabled(false);
            return resp;
        }
        if (StrUtil.hasBlank(onlyOfficeProperties.getDocumentServerUrl(),
                onlyOfficeProperties.getJwtSecret(), onlyOfficeProperties.getCallbackBaseUrl())) {
            log.warn("[buildPreviewConfig][contractId={}] OnlyOffice 配置不完整，降级关闭", contractId);
            resp.setEnabled(false);
            return resp;
        }

        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        LegalContractFileDO mainFile = contractFileMapper.selectByFileId(contract.getMainFileId());
        if (mainFile == null) {
            resp.setEnabled(false);
            return resp;
        }

        LegalContractSourceFormatEnum format = resolveFormat(contract, mainFile);
        if (format == LegalContractSourceFormatEnum.PDF) {
            log.info("[buildPreviewConfig][contractId={}] PDF 合同已停支持，关闭 OnlyOffice", contractId);
            resp.setEnabled(false);
            return resp;
        }

        Long documentFileId = resolveDocumentFileId(contract, false);
        LegalContractFileDO documentFile = contractFileMapper.selectByFileId(documentFileId);
        String fileName = documentFile != null && StrUtil.isNotBlank(documentFile.getFileName())
                ? documentFile.getFileName()
                : mainFile.getFileName();
        String fileType = LegalContractFormatUtils.toOnlyOfficeFileType(format);
        String documentType = LegalContractFormatUtils.toOnlyOfficeDocumentType(format);
        Long tenantId = TenantContextHolder.getTenantId();
        String documentRevision = resolveDocumentRevision(contract, contractId, false, documentFileId);
        boolean editable = LegalContractPermissionHelper.canManageOpinions(
                contract, auditOpinionMapper.selectPendingCount(contractId))
                && !"view".equalsIgnoreCase(StrUtil.blankToDefault(onlyOfficeProperties.getDefaultMode(), "edit"));

        if (tenantId == null) {
            log.warn("[buildPreviewConfig][contractId={}] 租户编号缺失，无法生成 OnlyOffice 拉流地址", contractId);
            resp.setEnabled(false);
            return resp;
        }

        String callbackBase = StrUtil.removeSuffix(onlyOfficeProperties.getCallbackBaseUrl(), "/");
        String pluginBase = resolvePluginBaseUrl(callbackBase);
        String fileUrl = fileTokenService.buildFileDownloadUrl(
                documentFileId,
                tenantId,
                callbackBase,
                onlyOfficeProperties.getJwtSecret(),
                onlyOfficeProperties.getFileTokenTtlMinutes());

        String documentKey = LegalOnlyOfficeDocumentKeyHelper.build(
                tenantId, contractId, documentFileId, documentRevision);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", fileType);
        document.put("key", documentKey);
        document.put("title", fileName);
        document.put("url", fileUrl);

        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("download", false);
        permissions.put("edit", editable);
        permissions.put("print", false);
        permissions.put("comment", editable);
        permissions.put("review", false);

        Map<String, Object> customization = new LinkedHashMap<>();
        customization.put("comments", editable);
        customization.put("feedback", false);
        customization.put("autosave", editable);
        customization.put("forcesave", editable);
        customization.put("download", false);
        customization.put("plugins", true);
        customization.put("compactHeader", false);
        customization.put("compactToolbar", false);
        customization.put("toolbarNoTabs", false);
        customization.put("hideRightMenu", false);
        customization.put("layout", buildEditorLayout());

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", editable ? "edit" : "view");
        editorConfig.put("lang", onlyOfficeProperties.getEditorLang());
        editorConfig.put("customization", customization);
        editorConfig.put("plugins", this.buildLocatePluginConfig(pluginBase));
        if (editable) {
            editorConfig.put("callbackUrl", callbackBase + "/legal/document/onlyoffice/callback");
            editorConfig.put("user", buildEditorUser());
        }

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("document", document);
        config.put("documentType", documentType);
        config.put("type", "desktop");
        config.put("width", "100%");
        config.put("height", "100%");
        config.put("editorConfig", editorConfig);
        config.put("permissions", permissions);

        String token = jwtSigner.sign(config, onlyOfficeProperties.getJwtSecret());
        if (StrUtil.isNotBlank(token)) {
            config.put("token", token);
        }

        resp.setEnabled(true);
        resp.setDocumentServerUrl(ensureTrailingSlash(onlyOfficeProperties.getDocumentServerUrl()));
        resp.setConfig(config);
        resp.setDocumentRevision(documentRevision);
        resp.setEditable(editable);
        return resp;
    }

    private String resolvePluginBaseUrl(String callbackBase) {
        if (StrUtil.isNotBlank(onlyOfficeProperties.getPluginBaseUrl())) {
            return StrUtil.removeSuffix(onlyOfficeProperties.getPluginBaseUrl(), "/");
        }
        HttpServletRequest request = WebFrameworkUtils.getRequest();
        if (request != null) {
            try {
                URI callbackUri = URI.create(callbackBase);
                String path = StrUtil.blankToDefault(callbackUri.getPath(), "");
                int port = request.getServerPort();
                boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                        || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
                String portPart = defaultPort ? "" : ":" + port;
                return request.getScheme() + "://" + request.getServerName() + portPart + path;
            } catch (Exception ex) {
                log.warn("[resolvePluginBaseUrl] 从当前请求推导插件地址失败，使用回退: {}", callbackBase, ex);
            }
        }
        return callbackBase.replace("host.docker.internal", "127.0.0.1");
    }

    private static Map<String, Object> buildEditorLayout() {
        Map<String, Object> toolbar = new LinkedHashMap<>();
        toolbar.put("home", new LinkedHashMap<>());
        toolbar.put("insert", new LinkedHashMap<>());
        toolbar.put("layout", new LinkedHashMap<>());
        toolbar.put("review", new LinkedHashMap<>());
        toolbar.put("view", new LinkedHashMap<>());
        toolbar.put("plugins", true);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("editMode", true);
        header.put("save", true);

        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("toolbar", toolbar);
        layout.put("header", header);
        return layout;
    }

    private Map<String, Object> buildLocatePluginConfig(String pluginBase) {
        String pluginConfigUrl = resolveLocatePluginConfigUrl(pluginBase);
        Map<String, Object> plugins = new LinkedHashMap<>();
        plugins.put("autostart", List.of(LOCATE_PLUGIN_GUID));
        plugins.put("pluginsData", List.of(pluginConfigUrl));
        return plugins;
    }

    private String resolveLocatePluginConfigUrl(String pluginBase) {
        if (Boolean.TRUE.equals(onlyOfficeProperties.getPluginMountOnDocumentServer())
                && StrUtil.isNotBlank(onlyOfficeProperties.getDocumentServerUrl())) {
            return ensureTrailingSlash(onlyOfficeProperties.getDocumentServerUrl())
                    + "sdkjs-plugins/legal-locate-v2/config.json";
        }
        return pluginBase + "/legal/document/onlyoffice/plugin/legal-locate-v2/config.json";
    }

    private static Map<String, Object> buildEditorUser() {
        Map<String, Object> user = new LinkedHashMap<>();
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null) {
            user.put("id", String.valueOf(loginUser.getId()));
            user.put("name", StrUtil.blankToDefault(
                    MapUtil.getStr(loginUser.getInfo(), LoginUser.INFO_KEY_NICKNAME), "审阅人"));
        } else {
            user.put("id", "0");
            user.put("name", "审阅人");
        }
        return user;
    }

    private Long resolveDocumentFileId(LegalContractDO contract, boolean pdfFormat) {
        if (pdfFormat) {
            Long workingPdfId = contractVersionService.resolvePdfWorkingFileId(contract.getId());
            return workingPdfId != null ? workingPdfId : contract.getMainFileId();
        }
        return contractVersionService.resolveWorkingFileId(contract.getId());
    }

    private String resolveDocumentRevision(LegalContractDO contract, Long contractId,
                                           boolean pdfFormat, Long documentFileId) {
        if (pdfFormat) {
            return contractVersionService.readPdfDocumentRevision(contractId, documentFileId);
        }
        return contractVersionService.readWorkingDocumentRevision(contractId);
    }

    private static LegalContractSourceFormatEnum resolveFormat(LegalContractDO contract,
                                                               LegalContractFileDO mainFile) {
        LegalContractSourceFormatEnum format = LegalContractSourceFormatEnum.of(contract.getSourceFormat());
        if (format != null) {
            return format;
        }
        if (StrUtil.isNotBlank(mainFile.getFormat())) {
            format = LegalContractSourceFormatEnum.of(mainFile.getFormat());
        }
        if (format != null) {
            return format;
        }
        return LegalContractFormatUtils.detectSourceFormat(mainFile.getFileName());
    }

    private static String ensureTrailingSlash(String url) {
        if (StrUtil.isBlank(url)) {
            return url;
        }
        return url.endsWith("/") ? url : url + "/";
    }
}
