package com.laby.module.legal.service.skillpack;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.module.ai.dal.dataobject.model.AiToolDO;
import com.laby.module.ai.service.model.AiToolService;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackLegalToolRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackPageReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSaveReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSimpleRespVO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.dal.mysql.skillpack.LegalSkillPackMapper;
import com.laby.module.legal.enums.LegalSkillPackConstants;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.SKILL_PACK_CODE_DUPLICATE;
import static com.laby.module.legal.enums.ErrorCodeConstants.SKILL_PACK_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.SKILL_PACK_SCENE_INVALID;

/**
 * 法务 AI 技能包 Service 实现类
 */
@Service
@Validated
public class LegalSkillPackServiceImpl implements LegalSkillPackService {

    @Resource
    private LegalSkillPackMapper skillPackMapper;
    @Resource
    private LegalSkillPackRegistry skillPackRegistry;
    @Resource
    private AiToolService aiToolService;

    @Override
    public Long createSkillPack(LegalSkillPackSaveReqVO createReqVO) {
        validateCodeUnique(null, createReqVO.getCode());
        LegalSkillPackDO entity = toEntity(createReqVO);
        entity.setVersion(1);
        skillPackMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateSkillPack(LegalSkillPackSaveReqVO updateReqVO) {
        LegalSkillPackDO existing = validateExists(updateReqVO.getId());
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        LegalSkillPackDO entity = toEntity(updateReqVO);
        entity.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);
        skillPackMapper.updateById(entity);
    }

    @Override
    public void updateSkillPackEnabled(Long id, Boolean enabled) {
        validateExists(id);
        skillPackMapper.updateById(new LegalSkillPackDO().setId(id).setEnabled(enabled));
    }

    @Override
    public void deleteSkillPack(Long id) {
        validateExists(id);
        skillPackMapper.deleteById(id);
    }

    @Override
    public void deleteSkillPackList(List<Long> ids) {
        List<LegalSkillPackDO> list = skillPackMapper.selectByIds(ids);
        if (CollUtil.isEmpty(list) || list.size() != ids.size()) {
            throw exception(SKILL_PACK_NOT_EXISTS);
        }
        skillPackMapper.deleteByIds(ids);
    }

    @Override
    public LegalSkillPackRespVO getSkillPack(Long id) {
        return convert(validateExists(id));
    }

    @Override
    public PageResult<LegalSkillPackRespVO> getSkillPackPage(LegalSkillPackPageReqVO pageReqVO) {
        PageResult<LegalSkillPackDO> page = skillPackMapper.selectPage(pageReqVO);
        return new PageResult<>(page.getList().stream().map(this::convert).toList(), page.getTotal());
    }

    @Override
    public List<LegalSkillPackSimpleRespVO> getSkillPackSimpleList(String scene) {
        if (StrUtil.isNotBlank(scene)) {
            validateScene(scene);
        }
        List<LegalSkillPackDO> list = skillPackMapper.selectListBySceneAndEnabled(scene, true);
        return BeanUtils.toBean(list, LegalSkillPackSimpleRespVO.class);
    }

    @Override
    public Long copySkillPack(Long id) {
        LegalSkillPackDO source = validateExists(id);
        LegalSkillPackSaveReqVO copy = new LegalSkillPackSaveReqVO();
        copy.setCode(source.getCode() + LegalSkillPackConstants.COPY_CODE_SUFFIX + System.currentTimeMillis());
        copy.setName(source.getName() + LegalSkillPackConstants.COPY_NAME_SUFFIX);
        copy.setScene(source.getScene());
        copy.setChatRoleId(source.getChatRoleId());
        copy.setToolNames(skillPackRegistry.sanitizeToolNames(source.getToolNames()));
        copy.setMcpClientNames(LegalSkillPackRegistry.parseStringList(source.getMcpClientNames()));
        copy.setModelPolicy(source.getModelPolicy());
        copy.setPlaybookId(source.getPlaybookId());
        copy.setDescription(source.getDescription());
        copy.setEnabled(Boolean.TRUE.equals(source.getEnabled()));
        return createSkillPack(copy);
    }

    @Override
    public List<LegalSkillPackLegalToolRespVO> getLegalAgentToolOptions() {
        Map<String, AiToolDO> registered = new LinkedHashMap<>();
        for (AiToolDO tool : aiToolService.getToolListByStatus(CommonStatusEnum.ENABLE.getStatus())) {
            if (tool == null || StrUtil.isBlank(tool.getName())) {
                continue;
            }
            if (!tool.getName().startsWith("legal_")) {
                continue;
            }
            if (!LegalSkillPackToolNames.ALLOWED.contains(tool.getName())) {
                continue;
            }
            registered.put(tool.getName(), tool);
        }
        List<LegalSkillPackLegalToolRespVO> result = new ArrayList<>();
        for (String name : LegalSkillPackToolNames.ALLOWED.stream().sorted().toList()) {
            AiToolDO tool = registered.get(name);
            result.add(LegalSkillPackLegalToolRespVO.builder()
                    .name(name)
                    .description(tool != null ? tool.getDescription() : defaultToolLabel(name))
                    .registered(tool != null)
                    .build());
        }
        return result;
    }

    private static String defaultToolLabel(String name) {
        return switch (name) {
            case "legal_search_paragraphs" -> "检索合同段落";
            case "legal_search_knowledge" -> "检索知识库";
            case "legal_get_contract_meta" -> "合同元信息";
            case "legal_get_audit_opinions" -> "审核意见";
            case "legal_get_audit_report" -> "审核报告";
            case "legal_compare_audit_rounds" -> "对比审核轮次";
            case "legal_propose_adopt_opinion" -> "提案采纳意见";
            case "legal_propose_skip_paragraph" -> "提案跳过段落";
            case "legal_adopt_opinion" -> "采纳意见";
            case "legal_batch_adopt_pending_opinions" -> "批量采纳待处理意见";
            default -> name;
        };
    }

    private LegalSkillPackDO validateExists(Long id) {
        LegalSkillPackDO pack = skillPackMapper.selectById(id);
        if (pack == null) {
            throw exception(SKILL_PACK_NOT_EXISTS);
        }
        return pack;
    }

    private void validateCodeUnique(Long id, String code) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        LegalSkillPackDO exists = skillPackMapper.selectByCode(code);
        if (exists != null && !exists.getId().equals(id)) {
            throw exception(SKILL_PACK_CODE_DUPLICATE);
        }
    }

    private void validateScene(String scene) {
        if (LegalSkillPackSceneEnum.of(scene) == null) {
            throw exception(SKILL_PACK_SCENE_INVALID);
        }
    }

    private LegalSkillPackDO toEntity(LegalSkillPackSaveReqVO reqVO) {
        validateScene(reqVO.getScene());
        LegalSkillPackDO entity = BeanUtils.toBean(reqVO, LegalSkillPackDO.class);
        List<String> tools = CollUtil.isEmpty(reqVO.getToolNames())
                ? List.of()
                : skillPackRegistry.sanitizeToolNames(JsonUtils.toJsonString(reqVO.getToolNames()));
        entity.setToolNames(CollUtil.isEmpty(tools) ? null : JsonUtils.toJsonString(tools));
        if (CollUtil.isNotEmpty(reqVO.getMcpClientNames())) {
            entity.setMcpClientNames(JsonUtils.toJsonString(reqVO.getMcpClientNames()));
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        return entity;
    }

    private LegalSkillPackRespVO convert(LegalSkillPackDO pack) {
        LegalSkillPackRespVO vo = BeanUtils.toBean(pack, LegalSkillPackRespVO.class);
        vo.setToolNames(skillPackRegistry.sanitizeToolNames(pack.getToolNames()));
        vo.setMcpClientNames(LegalSkillPackRegistry.parseStringList(pack.getMcpClientNames()));
        return vo;
    }

}
