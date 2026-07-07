import { stripEmojis } from './ai-content';
import { renderMarkdown } from './markdown';

function countFences(text: string): number {
  return (text.match(/```/g) || []).length;
}

export function prepareStreamingMarkdown(source: string): string {
  if (!source) {
    return '';
  }
  let text = source;
  if (countFences(text) % 2 === 1) {
    text += '\n```';
  }
  return text;
}

export function renderStreamingMarkdown(
  source: string,
  streaming = false,
): string {
  if (!source) {
    return '';
  }
  const cleaned = stripEmojis(source);
  return renderMarkdown(
    streaming ? prepareStreamingMarkdown(cleaned) : cleaned,
  );
}
