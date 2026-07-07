export interface RolePromptSection {
  index: number;
  body: string;
}

export interface ParsedRolePrompt {
  intro: string;
  sections: RolePromptSection[];
}

const SECTION_HEAD_RE = /^(\d+)[.、)]\s*(.*)$/s;

/** 将角色 systemMessage 拆成导语 + 编号条目，便于结构化展示 */
export function parseRolePrompt(content: string): ParsedRolePrompt {
  const text = content.trim();
  if (!text) {
    return { intro: '', sections: [] };
  }

  const lines = text
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean);

  const sections: RolePromptSection[] = [];
  const introParts: string[] = [];
  let currentSection: RolePromptSection | null = null;

  for (const line of lines) {
    const match = line.match(SECTION_HEAD_RE);
    if (match) {
      if (currentSection) {
        sections.push(currentSection);
      }
      currentSection = {
        index: Number(match[1]),
        body: (match[2] ?? '').trim(),
      };
      continue;
    }

    if (currentSection) {
      currentSection.body = currentSection.body
        ? `${currentSection.body} ${line}`
        : line;
    } else {
      introParts.push(line);
    }
  }

  if (currentSection) {
    sections.push(currentSection);
  }

  if (sections.length === 0) {
    const inlineParts = text.split(/(?=\d+[.、)]\s*)/).map((p) => p.trim()).filter(Boolean);
    if (inlineParts.length <= 1) {
      return { intro: text, sections: [] };
    }

    const firstPart = inlineParts[0] ?? '';
    if (!SECTION_HEAD_RE.test(firstPart)) {
      const inlineSections = inlineParts.slice(1).map((part) => {
        const sectionMatch = part.match(SECTION_HEAD_RE);
        if (!sectionMatch) {
          return { index: 0, body: part };
        }
        return {
          index: Number(sectionMatch[1]),
          body: (sectionMatch[2] ?? '').trim(),
        };
      });
      return { intro: firstPart, sections: inlineSections.filter((s) => s.body) };
    }

    return {
      intro: '',
      sections: inlineParts
        .map((part) => {
          const sectionMatch = part.match(SECTION_HEAD_RE);
          if (!sectionMatch) {
            return { index: 0, body: part };
          }
          return {
            index: Number(sectionMatch[1]),
            body: (sectionMatch[2] ?? '').trim(),
          };
        })
        .filter((s) => s.body),
    };
  }

  return { intro: introParts.join(' '), sections };
}

export interface TextToken {
  type: 'text' | 'tool';
  value: string;
}

const TOOL_NAME_RE = /([a-z][a-z0-9_]*(?:_[a-z0-9_]+)+)/g;

/** 识别 snake_case 工具名，便于单独高亮 */
export function tokenizeToolNames(text: string): TextToken[] {
  if (!text) {
    return [];
  }
  const tokens: TextToken[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  TOOL_NAME_RE.lastIndex = 0;
  while ((match = TOOL_NAME_RE.exec(text)) !== null) {
    if (match.index > lastIndex) {
      tokens.push({ type: 'text', value: text.slice(lastIndex, match.index) });
    }
    tokens.push({ type: 'tool', value: match[1] ?? '' });
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    tokens.push({ type: 'text', value: text.slice(lastIndex) });
  }
  return tokens.length > 0 ? tokens : [{ type: 'text', value: text }];
}
