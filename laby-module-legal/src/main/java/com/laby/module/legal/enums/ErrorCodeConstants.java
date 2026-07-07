package com.laby.module.legal.enums;

import com.laby.framework.common.exception.ErrorCode;

/**
 * Legal 错误码，使用 1-050-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode CONTRACT_NOT_EXISTS = new ErrorCode(1_050_000_000, "合同不存在");
    ErrorCode CONTRACT_PROCESS_NOT_STARTED = new ErrorCode(1_050_000_001, "流程未发起，请先在 BPM 中部署流程 legal_contract_review");
    ErrorCode CONTRACT_OPINION_FEEDBACK_REQUIRED = new ErrorCode(1_050_000_002, "提交二轮 AI 审核时，反馈说明至少 20 字");
    ErrorCode CONTRACT_FILE_REQUIRED = new ErrorCode(1_050_000_003, "请至少上传一个合同文件");
    ErrorCode CONTRACT_FILE_NOT_EXISTS = new ErrorCode(1_050_000_004, "合同附件不存在");
    ErrorCode CONTRACT_OPINION_NOT_EDITABLE = new ErrorCode(1_050_000_005, "当前流程节点不允许处置意见，请在「工作流-我的待办」办理对应任务");
    ErrorCode CONTRACT_OPINION_PENDING_NOT_DISPOSED = new ErrorCode(1_050_000_056, "仍有待处置意见，请先采纳或忽略后再完成复核");
    ErrorCode CONTRACT_MAIN_FILE_EMPTY = new ErrorCode(1_050_000_006, "合同主文件为空");
    ErrorCode CONTRACT_FILE_FORMAT_NOT_SUPPORTED = new ErrorCode(1_050_000_031, "仅支持 .doc、.docx 格式");
    ErrorCode CONTRACT_FILE_SIZE_EXCEEDED = new ErrorCode(1_050_000_032, "合同文件不能超过 {}MB");
    ErrorCode CONTRACT_PARAGRAPH_EMPTY = new ErrorCode(1_050_000_007, "合同段落为空，请先完成解析");
    ErrorCode CONTRACT_PARSE_NO_PARAGRAPH = new ErrorCode(1_050_000_008, "未解析到有效段落");
    ErrorCode CONTRACT_PARSE_NOT_SUCCESS = new ErrorCode(1_050_000_009, "合同解析未完成或失败");
    ErrorCode CONTRACT_PARSE_IN_PROGRESS = new ErrorCode(1_050_000_064, "合同正在解析中，请勿重复提交");
    ErrorCode CONTRACT_AI_AUDIT_FAILED = new ErrorCode(1_050_000_012, "AI 审核失败，请检查模型配置或稍后重试");
    ErrorCode CONTRACT_PROCESS_START_FAILED = new ErrorCode(1_050_000_013, "流程发起失败");
    ErrorCode CONTRACT_RETRY_NOT_FAILED = new ErrorCode(1_050_000_014, "仅处理失败的合同可重试");
    ErrorCode CONTRACT_AUDIT_NOT_READY = new ErrorCode(1_050_000_055, "合同尚未解析完成或已发起审核，无法重复触发首轮 AI 审核");
    ErrorCode CONTRACT_EXPORT_REPORT_FAILED = new ErrorCode(1_050_000_015, "审核报告 Word 生成失败");
    ErrorCode CONTRACT_CHAT_CONTEXT_EMPTY = new ErrorCode(1_050_000_016, "合同尚未解析完成，暂无法问答");
    ErrorCode CONTRACT_CHAT_MESSAGE_NOT_EXISTS = new ErrorCode(1_050_000_027, "合同问答消息不存在");
    ErrorCode CONTRACT_PARAGRAPH_NOT_EXISTS = new ErrorCode(1_050_000_017, "合同段落不存在");
    ErrorCode CONTRACT_EXPORT_VISIBILITY_INVALID = new ErrorCode(1_050_000_018, "导出可见性参数不正确");
    ErrorCode CONTRACT_EXPORT_MODE_INVALID = new ErrorCode(1_050_000_019, "导出模式参数不正确");
    ErrorCode CONTRACT_TYPE_NOT_EXISTS = new ErrorCode(1_050_000_020, "合同类型不存在");
    ErrorCode STANDARD_CLAUSE_NOT_EXISTS = new ErrorCode(1_050_000_021, "标准条款不存在");
    ErrorCode AUDIT_RULE_NOT_EXISTS = new ErrorCode(1_050_000_022, "审核规则不存在");
    ErrorCode CONTRACT_TYPE_CODE_DUPLICATE = new ErrorCode(1_050_000_023, "合同类型编码已存在");
    ErrorCode STANDARD_CLAUSE_NAME_DUPLICATE = new ErrorCode(1_050_000_024, "标准条款名称已存在");
    ErrorCode AUDIT_RULE_NAME_DUPLICATE = new ErrorCode(1_050_000_025, "审核规则名称已存在");
    ErrorCode AUDIT_RULE_PREFERRED_REQUIRES_STANDARD_CLAUSE = new ErrorCode(1_050_000_061, "推荐标准条款规则必须关联标准条款");
    ErrorCode AUDIT_RULE_TYPE_INVALID = new ErrorCode(1_050_000_062, "审核规则类型无效");
    ErrorCode STANDARD_CLAUSE_CATEGORY_SCOPE_INVALID = new ErrorCode(1_050_000_026, "标准条款适用范围无效");

    ErrorCode OPINION_NOT_EXISTS = new ErrorCode(1_050_000_010, "审核意见不存在");
    ErrorCode OPINION_NOT_MANUAL = new ErrorCode(1_050_000_011, "仅允许编辑手工新增的意见");

    ErrorCode AGENT_PROPOSAL_NOT_EXISTS = new ErrorCode(1_050_000_030, "Agent 提案不存在或已失效");

    ErrorCode SKILL_PACK_NOT_EXISTS = new ErrorCode(1_050_000_028, "AI 技能包不存在");
    ErrorCode SKILL_PACK_CODE_DUPLICATE = new ErrorCode(1_050_000_029, "AI 技能包编码已存在");
    ErrorCode SKILL_PACK_SCENE_INVALID = new ErrorCode(1_050_000_063, "AI 技能包场景无效");

    ErrorCode ORCHESTRATION_SESSION_NOT_EXISTS = new ErrorCode(1_050_000_040, "法务编排会话不存在");
    ErrorCode ORCHESTRATION_CHECKPOINT_NOT_FOUND = new ErrorCode(1_050_000_052, "法务编排 Checkpoint 不存在");
    ErrorCode CONTRACT_MEMORY_NOT_EXISTS = new ErrorCode(1_050_000_053, "合同情节记忆不存在");
    ErrorCode CONTRACT_MEMORY_TYPE_INVALID = new ErrorCode(1_050_000_054, "合同情节记忆类型无效");
    ErrorCode CONTRACT_DELIVERABLE_INVALID = new ErrorCode(1_050_000_057, "交付物类型不正确");
    ErrorCode CONTRACT_DELIVERABLE_NOT_SUPPORTED = new ErrorCode(1_050_000_058, "当前合同格式不支持该交付物：{}");
    ErrorCode CONTRACT_WORKING_NOT_READY = new ErrorCode(1_050_000_059, "工作版尚未生成，请先在审阅工作台打开合同");
    ErrorCode ORCHESTRATION_FILE_NOT_EXISTS = new ErrorCode(1_050_000_041, "法务编排文件项不存在");
    ErrorCode ORCHESTRATION_PHASE_INVALID = new ErrorCode(1_050_000_042, "当前编排阶段不允许该操作");
    ErrorCode ORCHESTRATION_CLASSIFY_EMPTY = new ErrorCode(1_050_000_043, "无待分类的编排文件");
    ErrorCode ORCHESTRATION_TYPE_NOT_RESOLVED = new ErrorCode(1_050_000_044, "合同类型尚未确认");
    ErrorCode ORCHESTRATION_PROPOSAL_EXPIRED = new ErrorCode(1_050_000_045, "法务编排提案已过期");
    ErrorCode ORCHESTRATION_POLICY_MODEL_MISSING = new ErrorCode(1_050_000_046, "编排策略缺少大模型，请先在对话中选择模型");
    ErrorCode ORCHESTRATION_POLICY_INVALID = new ErrorCode(1_050_000_047, "编排策略参数无效（立场或审核强度）");
    ErrorCode ORCHESTRATION_PREVIEW_FILE_NOT_CLASSIFIED = new ErrorCode(1_050_000_048, "文件尚未完成分类，无法预览审核");
    ErrorCode ORCHESTRATION_PREVIEW_PARSE_FAILED = new ErrorCode(1_050_000_049, "预览审核文件解析失败");
    ErrorCode ORCHESTRATION_PREVIEW_EMPTY = new ErrorCode(1_050_000_050, "暂无预览审核结果，请先调用 preview_audit");
    ErrorCode ORCHESTRATION_PREVIEW_FILE_MISSING = new ErrorCode(1_050_000_051, "编排文件缺少存储编号，无法预览审核");

}
