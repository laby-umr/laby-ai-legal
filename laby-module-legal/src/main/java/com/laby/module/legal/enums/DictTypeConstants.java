package com.laby.module.legal.enums;

/**
 * 法务模块字典类型，与 system_dict_type.type 一致
 */
public interface DictTypeConstants {

    String LEGAL_CONTRACT_STATUS = "legal_contract_status";
    String LEGAL_RISK_LEVEL = "legal_risk_level";
    String LEGAL_AUDIT_LEVEL = "legal_audit_level";
    String LEGAL_PARTY_ROLE = "legal_party_role";
    /** 标准条款适用范围 {@link com.laby.module.legal.enums.standardclause.LegalStandardClauseScopeEnum} */
    String LEGAL_STANDARD_CLAUSE_SCOPE = "legal_standard_clause_scope";
    /** 条款分类（标准条款库、审核规则） {@link com.laby.module.legal.enums.standardclause.LegalClauseTypeDictEnum} */
    String LEGAL_CLAUSE_TYPE = "legal_clause_type";
    /** SkillPack / AI 追踪场景 {@link com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum} */
    String LEGAL_SKILL_PACK_SCENE = "legal_skill_pack_scene";
    /** AI 链路追踪状态 {@link com.laby.module.legal.enums.trace.LegalAiTraceStatusEnum} */
    String LEGAL_AI_TRACE_STATUS = "legal_ai_trace_status";
    /** 合同原件格式 {@link com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum} */
    String LEGAL_CONTRACT_SOURCE_FORMAT = "legal_contract_source_format";

}
