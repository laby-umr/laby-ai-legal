/** 移除 emoji，避免模型回复带表情符号 */
export function stripEmojis(text: string): string {
  if (!text) {
    return '';
  }
  return text
    .replace(/\p{Extended_Pictographic}/gu, '')
    .replace(/[\u200D\uFE0F]/g, '')
    .replace(/ {2,}/g, ' ')
    .replace(/\n{3,}/g, '\n\n');
}
