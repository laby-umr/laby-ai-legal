export type MascotCharacter = 'purple' | 'black' | 'orange' | 'yellow';

/** 气泡锚点：相对 550×400 角色舞台，气泡在角色头顶上方，尾巴朝下 */
export interface MascotBubbleAnchor {
  /** 水平锚点（px），配合 translateX(-50%) 居中对齐角色 */
  left: string;
  /** 距舞台底部的距离（px），气泡底边浮在角色顶边之上 */
  bottom: string;
}

export interface ActiveMascotBubble {
  character: MascotCharacter;
  id: string;
  message: string;
}

/**
 * 角色布局（bottom:0）：
 * - 紫紫 left:70  w:180 h:400  顶边 y=0
 * - 小黑 left:240 w:120 h:310  顶边 y=90
 * - 橙橙 left:0   w:240 h:200  顶边 y=200
 * - 小黄 left:310 w:140 h:230  顶边 y=170
 */
export const MASCOT_BUBBLE_ANCHORS: Record<MascotCharacter, MascotBubbleAnchor> =
  {
    // bottom = 角色高度 + 间距，气泡整体在角色顶边之上
    purple: { bottom: '412px', left: '160px' },
    black: { bottom: '330px', left: '300px' },
    orange: { bottom: '220px', left: '120px' },
    yellow: { bottom: '256px', left: '380px' },
  };

export const MASCOT_CHARACTER_META: Record<
  MascotCharacter,
  { accent: string; label: string }
> = {
  purple: { accent: '#6C3FF5', label: '紫紫' },
  black: { accent: '#27272a', label: '小黑' },
  orange: { accent: '#FF9B6B', label: '橙橙' },
  yellow: { accent: '#EAB308', label: '小黄' },
};
