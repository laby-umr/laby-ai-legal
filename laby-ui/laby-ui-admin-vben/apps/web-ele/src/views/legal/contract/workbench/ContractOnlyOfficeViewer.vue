<script lang="ts" setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { getDocumentPreviewConfig } from '#/api/legal/contract';

import {
  buildOnlyOfficeSearchVariants,
  buildParagraphBookmarkName,
  ONLYOFFICE_LOCATE,
} from './utils/onlyoffice-locate';

const props = defineProps<{
  contractId?: number;
  /** 意见定位传入的原文片段 */
  activeLocateText?: string;
  /** 段落 ID（p-1），优先通过书签 laby_p_{id} 定位 */
  activeLocateParagraphId?: string;
  /** 重复点击同一段落时递增，强制重新定位 */
  locateNonce?: number;
  /** WORKING 文档 revision，变化时重载编辑器 */
  documentRevision?: string;
}>();

const emit = defineEmits<{
  'document-revision': [revision?: string];
  'locate-failed': [searchText?: string, paragraphId?: string];
  'load-failed': [];
}>();

const loading = ref(false);
const errorMessage = ref('');
const documentReady = ref(false);
const pluginReady = ref(false);
const placeholderId = `legal-onlyoffice-${Math.random().toString(36).slice(2)}`;
const editorRef = ref<OnlyOfficeDocEditor | null>(null);

let scriptLoaded = false;
let locateRequestSeq = 0;
let pendingLocateText: string | undefined;
let pendingLocateParagraphId: string | undefined;
let locateResultHandler: ((event: MessageEvent) => void) | null = null;
let iframePermissionObserver: MutationObserver | null = null;
let editorResizeObserver: ResizeObserver | null = null;
let officeConnector: OnlyOfficeConnector | null = null;

/** Chrome 会拦截 iframe 内 unload 监听；OnlyOffice app.js 仍会注册，产生 Violation 日志 */
const ONLYOFFICE_IFRAME_ALLOW =
  'clipboard-read; clipboard-write; autoplay; fullscreen; unload';

function patchOnlyOfficeIframePermissions(root: HTMLElement | null) {
  if (!root) {
    return;
  }
  root.querySelectorAll('iframe').forEach((node) => {
    const iframe = node as HTMLIFrameElement;
    const current = iframe.getAttribute('allow') ?? '';
    const parts = new Set(
      current
        .split(';')
        .map((item) => item.trim())
        .filter(Boolean),
    );
    ONLYOFFICE_IFRAME_ALLOW.split(';')
      .map((item) => item.trim())
      .filter(Boolean)
      .forEach((item) => parts.add(item));
    iframe.setAttribute('allow', [...parts].join('; '));
  });
}

function startIframePermissionObserver() {
  stopIframePermissionObserver();
  const host = document.getElementById(placeholderId);
  if (!host) {
    return;
  }
  patchOnlyOfficeIframePermissions(host);
  iframePermissionObserver = new MutationObserver(() => {
    patchOnlyOfficeIframePermissions(host);
  });
  iframePermissionObserver.observe(host, { childList: true, subtree: true });
}

function stopIframePermissionObserver() {
  iframePermissionObserver?.disconnect();
  iframePermissionObserver = null;
}

interface OnlyOfficeConnector {
  executeMethod: (
    name: string,
    args: unknown,
    callback?: (result: unknown) => void,
  ) => void;
}

interface OnlyOfficeDocEditor {
  destroyEditor?: () => void;
  createConnector?: () => OnlyOfficeConnector;
  serviceCommand?: (command: string, data?: unknown) => void;
  resizeEditor?: () => void;
}

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (
        id: string,
        config: Record<string, unknown>,
      ) => OnlyOfficeDocEditor;
    };
  }
}

function getEditorIframe(): HTMLIFrameElement | null {
  const host = document.getElementById(placeholderId);
  return host?.querySelector('iframe') ?? null;
}

/** 社区版标准路径：docEditor.serviceCommand → Gateway.internalcommand → 插件 */
function postLocateToEditor(payload: Record<string, unknown>): boolean {
  try {
    editorRef.value?.serviceCommand?.('legalLocate', payload);
    return true;
  } catch {
    return false;
  }
}

function stopEditorResizeObserver() {
  editorResizeObserver?.disconnect();
  editorResizeObserver = null;
}

function measureEditorShellHeight(): number {
  const host = document.getElementById(placeholderId);
  const editorShell = host?.closest('.legal-workbench-editor');
  if (editorShell && editorShell.clientHeight > 0) {
    return editorShell.clientHeight;
  }
  const viewer = host?.closest('.legal-onlyoffice-viewer');
  if (viewer && viewer.clientHeight > 0) {
    return viewer.clientHeight;
  }
  return 0;
}

async function waitForEditorShellHeight(maxAttempts = 48): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const height = measureEditorShellHeight();
    if (height >= 240) {
      return height;
    }
    await new Promise<void>((resolve) => {
      requestAnimationFrame(() => resolve());
    });
  }
  const fallback = measureEditorShellHeight();
  if (fallback > 0) {
    return fallback;
  }
  return Math.max(520, window.innerHeight - 280);
}

function applyHostHeight(explicitHeight?: number): number {
  const host = document.getElementById(placeholderId);
  const height = explicitHeight ?? measureEditorShellHeight();
  if (!host || height <= 0) {
    return height;
  }
  host.style.height = `${height}px`;
  host.style.minHeight = '0';
  return height;
}

function scheduleEditorResize() {
  void nextTick(() => {
    applyHostHeight();
    try {
      editorRef.value?.resizeEditor?.();
    } catch {
      // ignore
    }
    window.dispatchEvent(new Event('resize'));
  });
}

function startEditorResizeObserver() {
  stopEditorResizeObserver();
  const host = document.getElementById(placeholderId);
  if (!host) {
    return;
  }
  const editorShell = host.closest('.legal-workbench-editor');
  scheduleEditorResize();
  window.setTimeout(scheduleEditorResize, 300);
  window.setTimeout(scheduleEditorResize, 1200);
  editorResizeObserver = new ResizeObserver(() => {
    scheduleEditorResize();
  });
  editorResizeObserver.observe(host);
  if (editorShell) {
    editorResizeObserver.observe(editorShell);
  }
}

function destroyEditor() {
  stopIframePermissionObserver();
  stopEditorResizeObserver();
  try {
    editorRef.value?.destroyEditor?.();
  } catch {
    // ignore
  }
  editorRef.value = null;
  officeConnector = null;
  documentReady.value = false;
  pluginReady.value = false;
  pendingLocateText = undefined;
  pendingLocateParagraphId = undefined;
}

function buildLocatePayload(
  variants: string[],
  requestId: number,
  paragraphId?: string,
) {
  return {
    type: ONLYOFFICE_LOCATE.MSG_LOCATE,
    paragraphId,
    bookmarkName: buildParagraphBookmarkName(paragraphId),
    variants,
    requestId,
  };
}

function locateViaConnector(variants: string[]): Promise<boolean> {
  const connector = officeConnector ?? editorRef.value?.createConnector?.();
  if (!connector) {
    return Promise.resolve(false);
  }
  return new Promise((resolve) => {
    try {
      let index = 0;
      const tryNext = () => {
        if (index >= variants.length) {
          resolve(false);
          return;
        }
        const query = variants[index];
        index += 1;
        connector.executeMethod(
          'SearchNext',
          [{ searchString: query, matchCase: false }, true],
          (found: unknown) => {
            if (found) {
              resolve(true);
            } else {
              tryNext();
            }
          },
        );
      };
      tryNext();
    } catch {
      resolve(false);
    }
  });
}

function waitForLocateResult(
  requestId: number,
  timeoutMs: number,
): Promise<boolean> {
  return new Promise((resolve) => {
    const timer = window.setTimeout(() => {
      cleanup();
      resolve(false);
    }, timeoutMs);
    const handler = (event: MessageEvent) => {
      const data = parsePluginEventData(event.data);
      if (
        data?.type !== ONLYOFFICE_LOCATE.MSG_RESULT ||
        data.requestId !== requestId
      ) {
        return;
      }
      cleanup();
      resolve(!!data.found);
    };
    const cleanup = () => {
      window.clearTimeout(timer);
      if (locateResultHandler) {
        window.removeEventListener('message', locateResultHandler);
        locateResultHandler = null;
      }
    };
    locateResultHandler = handler;
    window.addEventListener('message', handler);
  });
}

async function locateBySearchText(
  searchText?: string,
  paragraphId?: string,
): Promise<boolean> {
  const variants = buildOnlyOfficeSearchVariants(searchText);
  const hasBookmark = !!buildParagraphBookmarkName(paragraphId);
  if (variants.length === 0 && !hasBookmark) {
    return false;
  }
  if (!documentReady.value) {
    pendingLocateText = searchText;
    pendingLocateParagraphId = paragraphId;
    return false;
  }

  for (let attempt = 0; attempt < 6; attempt += 1) {
    if (attempt > 0) {
      await waitMs(400);
    }
    const requestId = ++locateRequestSeq;
    postLocateToEditor(buildLocatePayload(variants, requestId, paragraphId));
    const pluginFound = await waitForLocateResult(
      requestId,
      attempt === 0 ? 2800 : 1600,
    );
    if (pluginFound) {
      return true;
    }
  }

  if (variants.length > 0) {
    const connectorFound = await locateViaConnector(variants);
    if (connectorFound) {
      return true;
    }
  }

  return false;
}

async function flushPendingLocate() {
  if (!pendingLocateText?.trim() && !pendingLocateParagraphId?.trim()) {
    return;
  }
  const text = pendingLocateText;
  const paragraphId = pendingLocateParagraphId;
  pendingLocateText = undefined;
  pendingLocateParagraphId = undefined;
  const ok = await locateBySearchText(text, paragraphId);
  if (!ok) {
    emit('locate-failed', text, paragraphId);
  }
}

function parsePluginEventData(data: unknown): Record<string, unknown> | null {
  if (!data) {
    return null;
  }
  if (typeof data === 'string') {
    try {
      return JSON.parse(data) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
  if (typeof data === 'object') {
    return data as Record<string, unknown>;
  }
  return null;
}

function handlePluginMessage(event: MessageEvent) {
  const data = parsePluginEventData(event.data);
  if (!data) {
    return;
  }
  if (data.type === ONLYOFFICE_LOCATE.MSG_READY) {
    pluginReady.value = true;
    void flushPendingLocate();
  }
}

async function loadScript(documentServerUrl: string): Promise<void> {
  if (window.DocsAPI) {
    scriptLoaded = true;
    return;
  }
  const base = documentServerUrl.replace(/\/?$/, '/');
  const src = `${base}web-apps/apps/api/documents/api.js`;
  const existing = document.querySelector(`script[data-onlyoffice="1"]`);
  if (existing) {
    existing.remove();
    scriptLoaded = false;
  }
  await new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = src;
    script.dataset.onlyoffice = '1';
    script.onload = () => {
      scriptLoaded = true;
      resolve();
    };
    script.onerror = () =>
      reject(
        new Error(
          `OnlyOffice api.js 无法加载（${src}）。请确认 Document Server 已启动：cd script/docker && docker compose --env-file docker.env up -d onlyoffice`,
        ),
      );
    document.head.appendChild(script);
  });
}

async function renderPreview() {
  if (!props.contractId) {
    errorMessage.value = '暂无合同';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  pluginReady.value = false;
  destroyEditor();
  try {
    const resp = await getDocumentPreviewConfig(props.contractId);
    if (!resp.enabled || !resp.config || !resp.documentServerUrl) {
      errorMessage.value = 'ONLYOFFICE_DISABLED';
      return;
    }
    if (resp.documentRevision) {
      emit('document-revision', resp.documentRevision);
    }
    await loadScript(resp.documentServerUrl);
    if (!window.DocsAPI) {
      throw new Error('DocsAPI 未就绪');
    }
    loading.value = false;
    await nextTick();
    await waitForEditorShellHeight();
    const editorHeight = applyHostHeight();
    const baseConfig = { ...(resp.config as Record<string, unknown>) };
    baseConfig.height = `${editorHeight}px`;
    baseConfig.width = '100%';
    const events = (baseConfig.events as Record<string, unknown> | undefined) ?? {};
    baseConfig.events = {
      ...events,
      onDocumentReady: () => {
        documentReady.value = true;
        try {
          officeConnector = editorRef.value?.createConnector?.() ?? null;
        } catch {
          officeConnector = null;
        }
        startEditorResizeObserver();
        void nextTick(() => {
          void flushPendingLocate();
        });
      },
    };
    editorRef.value = new window.DocsAPI.DocEditor(placeholderId, baseConfig);
    startIframePermissionObserver();
    startEditorResizeObserver();
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '文档编辑器加载失败';
    if (message.includes('api.js')) {
      emit('load-failed');
      return;
    }
    errorMessage.value = message;
  } finally {
    loading.value = false;
  }
}

function waitMs(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

async function runLocate(text?: string, paragraphId?: string) {
  if (!text?.trim() && !paragraphId?.trim()) {
    return;
  }
  pendingLocateText = text;
  pendingLocateParagraphId = paragraphId;
  await nextTick();

  for (let attempt = 0; attempt < 20; attempt += 1) {
    if (!documentReady.value) {
      await waitMs(400);
      continue;
    }
    if (!pluginReady.value && attempt < 10) {
      await waitMs(350);
      continue;
    }
    const ok = await locateBySearchText(text, paragraphId);
    if (ok) {
      pendingLocateText = undefined;
      pendingLocateParagraphId = undefined;
      return;
    }
    await waitMs(600);
  }

  if (!documentReady.value) {
    return;
  }
  pendingLocateText = undefined;
  pendingLocateParagraphId = undefined;
  emit('locate-failed', text, paragraphId);
}

watch(
  () =>
    [props.activeLocateText, props.activeLocateParagraphId, props.locateNonce] as const,
  async ([text, paragraphId]) => {
    await runLocate(text, paragraphId);
  },
);

watch(
  () => props.documentRevision,
  (revision, previous) => {
    if (!revision || revision === previous || !props.contractId) {
      return;
    }
    void renderPreview();
  },
);

watch(
  () => props.contractId,
  () => {
    void renderPreview();
  },
  { immediate: true },
);

onMounted(() => {
  window.addEventListener('message', handlePluginMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handlePluginMessage);
  destroyEditor();
});

async function forceSaveAndWait(timeoutMs = 8000): Promise<boolean> {
  if (!props.contractId || !documentReady.value || !editorRef.value) {
    return false;
  }
  const revisionBefore = props.documentRevision;
  try {
    editorRef.value.serviceCommand?.('forcesave', '');
  } catch {
    return false;
  }
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    await waitMs(450);
    try {
      const resp = await getDocumentPreviewConfig(props.contractId);
      if (
        resp.documentRevision &&
        resp.documentRevision !== revisionBefore
      ) {
        emit('document-revision', resp.documentRevision);
        return true;
      }
    } catch {
      // ignore poll errors
    }
  }
  try {
    const resp = await getDocumentPreviewConfig(props.contractId);
    if (resp.documentRevision) {
      emit('document-revision', resp.documentRevision);
    }
  } catch {
    // ignore
  }
  return true;
}

defineExpose({ reload: renderPreview, locateBySearchText, forceSaveAndWait });
</script>

<template>
  <div class="legal-onlyoffice-viewer relative flex h-full min-h-0 w-full flex-col">
    <div
      v-if="loading"
      class="absolute inset-0 z-10 flex items-center justify-center bg-muted/30 text-sm text-muted-foreground"
    >
      正在加载文档编辑器…
    </div>
    <div
      v-if="errorMessage === 'ONLYOFFICE_DISABLED'"
      class="flex h-full items-center justify-center px-6 text-center text-sm text-muted-foreground"
    >
      未启用 OnlyOffice，请联系管理员部署 Document Server 并配置 laby.legal.onlyoffice.enabled=true。
    </div>
    <div
      v-else-if="errorMessage"
      class="flex h-full items-center justify-center px-6 text-center text-sm text-destructive"
    >
      {{ errorMessage }}
    </div>
    <div
      v-show="!errorMessage"
      :id="placeholderId"
      class="legal-onlyoffice-viewer__host"
    />
  </div>
</template>

<style scoped>
.legal-onlyoffice-viewer__host {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
}

.legal-onlyoffice-viewer__host :deep(iframe) {
  display: block;
  width: 100%;
  border: 0;
}
</style>
