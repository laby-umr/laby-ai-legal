import MarkdownIt from 'markdown-it';
import markdownItMark from 'markdown-it-mark';
import markdownItMultimdTable from 'markdown-it-multimd-table';
import markdownItTaskLists from 'markdown-it-task-lists';
import hljs from 'highlight.js';

function encodeCodeForAttr(code: string): string {
  try {
    return btoa(unescape(encodeURIComponent(code)));
  } catch {
    return '';
  }
}

export function decodeCodeFromAttr(encoded: string): string {
  try {
    return decodeURIComponent(escape(atob(encoded)));
  } catch {
    return '';
  }
}

function renderCodeBlock(str: string, lang?: string): string {
  const langLabel = (lang || 'text').toLowerCase();
  let highlighted: string;
  if (langLabel && hljs.getLanguage(langLabel)) {
    highlighted = hljs.highlight(str, {
      language: langLabel,
      ignoreIllegals: true,
    }).value;
  } else {
    highlighted = hljs.highlightAuto(str).value;
  }
  const encoded = encodeCodeForAttr(str);
  const safeLang = md.utils.escapeHtml(langLabel);
  return (
    `<div class="code-block-wrap">` +
    `<div class="code-block-header">` +
    `<span class="code-lang">${safeLang}</span>` +
    `<button type="button" class="code-copy-btn" data-code="${encoded}" title="复制代码">复制</button>` +
    `</div>` +
    `<pre><code class="hljs language-${safeLang}">${highlighted}</code></pre>` +
    `</div>`
  );
}

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: (str, lang) => renderCodeBlock(str, lang),
});

md.use(markdownItMultimdTable, {
  multiline: true,
  rowspan: true,
  headerless: true,
});
md.use(markdownItTaskLists, { enabled: true, label: true, labelAfter: true });
md.use(markdownItMark);

const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, _env, self) =>
    self.renderToken(tokens, idx, options));

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  if (!token) {
    return defaultLinkOpen(tokens, idx, options, env, self);
  }
  token.attrSet('target', '_blank');
  token.attrSet('rel', 'noopener noreferrer');
  return defaultLinkOpen(tokens, idx, options, env, self);
};

const defaultImage =
  md.renderer.rules.image ||
  ((tokens, idx, options, _env, self) =>
    self.renderToken(tokens, idx, options));

md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  if (token) {
    token.attrSet('loading', 'lazy');
    token.attrSet('class', 'md-image');
  }
  return defaultImage(tokens, idx, options, env, self);
};

const defaultTableOpen =
  md.renderer.rules.table_open ||
  ((tokens, idx, options, _env, self) =>
    self.renderToken(tokens, idx, options));

md.renderer.rules.table_open = (tokens, idx, options, env, self) => {
  return `<div class="table-scroll">${defaultTableOpen(tokens, idx, options, env, self)}`;
};

const defaultTableClose =
  md.renderer.rules.table_close ||
  ((tokens, idx, options, _env, self) =>
    self.renderToken(tokens, idx, options));

md.renderer.rules.table_close = (tokens, idx, options, env, self) => {
  return `${defaultTableClose(tokens, idx, options, env, self)}</div>`;
};

export function renderMarkdown(markdown: string): string {
  if (!markdown) {
    return '';
  }
  try {
    return md.render(markdown);
  } catch {
    return md.utils.escapeHtml(markdown).replace(/\n/g, '<br>');
  }
}
