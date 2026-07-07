package com.laby.module.legal.service.contracttype;

import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigOverviewRespVO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.dal.mysql.auditrule.LegalAuditRuleMapper;
import com.laby.module.legal.dal.mysql.skillpack.LegalSkillPackMapper;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractTypeConfigServiceImplTest {

    @InjectMocks
    private LegalContractTypeConfigServiceImpl configService;

    @Mock
    private LegalContractTypeService contractTypeService;
    @Mock
    private LegalAuditRuleMapper auditRuleMapper;
    @Mock
    private LegalSkillPackRegistry skillPackRegistry;
    @Mock
    private LegalSkillPackMapper skillPackMapper;
    @Mock
    private AiKnowledgeService aiKnowledgeService;

    @Test
    void getConfigOverview_shouldBuildChecklist() {
        LegalContractTypeDO type = LegalContractTypeDO.builder()
                .id(10L)
                .name("采购合同")
                .knowledgeId(100L)
                .defaultSkillPackIdAudit(20L)
                .build();
        when(contractTypeService.validateContractTypeExists(10L)).thenReturn(type);
        when(auditRuleMapper.selectEnabledForAudit(10L)).thenReturn(List.of());
        when(skillPackMapper.selectById(20L)).thenReturn(LegalSkillPackDO.builder()
                .id(20L)
                .name("默认审核包")
                .scene("AUDIT")
                .enabled(true)
                .chatRoleId(30L)
                .toolNames("[\"legal_search_paragraphs\"]")
                .build());
        when(skillPackRegistry.sanitizeToolNames(any())).thenReturn(List.of("legal_search_paragraphs"));

        LegalContractTypeConfigOverviewRespVO overview = configService.getConfigOverview(10L);

        assertEquals("采购合同", overview.getContractTypeName());
        assertTrue(overview.getAuditSkillPack().getConfigured());
        assertEquals(5, overview.getChecklist().size());
    }

}
