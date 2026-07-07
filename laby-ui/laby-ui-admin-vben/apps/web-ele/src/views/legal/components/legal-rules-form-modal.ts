/** 规则与条款模块 — 新建/编辑弹窗统一配置（无内容区滚轮、加宽） */
export const LEGAL_RULES_FORM_MODAL_OPTIONS = {
  contentClass: 'overflow-hidden',
  class: 'w-[960px] max-w-[96vw] !max-h-none',
} as const;

export const LEGAL_RULES_FORM_GRID = {
  formItemClass: 'col-span-1 items-start',
  wrapperClass: 'grid grid-cols-2 gap-x-6 gap-y-0',
  labelWidth: 100,
} as const;
