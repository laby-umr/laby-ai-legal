<script lang="ts" setup>
import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { ElMessage, ElTabPane, ElTabs } from 'element-plus';

import ContractOnlyOfficeViewer from './ContractOnlyOfficeViewer.vue';
import { buildOnlyOfficeSearchVariants, buildParagraphBookmarkName } from './utils/onlyoffice-locate';

export type WorkbenchSideTab = 'opinions' | 'chat' | 'report' | 'download';

const props = defineProps<{
  contractId?: number;
  sourceFormat?: string;
  mainFileId?: number;
  activeLocateText?: string;
  activeLocateParagraphId?: string;
  locateNonce?: number;
  documentRevision?: string;
  pendingOpinionCount?: number;
  totalOpinionCount?: number;
  hasAuditReport?: boolean;
  sideTab?: WorkbenchSideTab;
}>();

const emit = defineEmits<{
  'document-revision': [revision?: string];
  'update:sideTab': [tab: WorkbenchSideTab];
}>();

const sideCollapsed = ref(false);
const onlyOfficeRef = ref<InstanceType<typeof ContractOnlyOfficeViewer>>();

const activeSideTab = computed({
  get: () => props.sideTab ?? 'opinions',
  set: (tab: WorkbenchSideTab) => emit('update:sideTab', tab),
});

const canOpenDocument = computed(
  () => !!props.mainFileId && !!props.contractId,
);

const sideTabs = computed(() => [
  {
    key: 'opinions' as const,
    label: '审核意见',
    icon: 'lucide:clipboard-list',
    badge: (props.pendingOpinionCount ?? 0) > 0
      ? props.pendingOpinionCount
      : (props.totalOpinionCount ?? 0) > 0
        ? props.totalOpinionCount
        : undefined,
    badgeType: (props.pendingOpinionCount ?? 0) > 0 ? 'warning' : 'info',
  },
  {
    key: 'chat' as const,
    label: 'AI问答',
    icon: 'lucide:message-square',
  },
  {
    key: 'report' as const,
    label: '审核报告',
    icon: 'lucide:file-text',
    badge: props.hasAuditReport ? '●' : undefined,
    badgeType: 'info' as const,
  },
  {
    key: 'download' as const,
    label: '下载',
    icon: 'lucide:download',
  },
]);

async function onOnlyOfficeLocateFailed(searchText?: string, paragraphId?: string) {
  const bookmark = buildParagraphBookmarkName(paragraphId);
  if (bookmark) {
    ElMessage.info(
      `未能自动定位到段落${paragraphId ? `（${paragraphId}）` : ''}，请确认文档已生成 WORKING 版本后重试，或使用 Ctrl+F 搜索`,
    );
    return;
  }
  const hint = buildOnlyOfficeSearchVariants(searchText)[0];
  if (hint) {
    try {
      await navigator.clipboard.writeText(hint);
      ElMessage.info(`未自动定位到段落，关键词「${hint.slice(0, 24)}${hint.length > 24 ? '…' : ''}」已复制，请在文档中按 Ctrl+F 粘贴搜索`);
      return;
    } catch {
      // ignore
    }
  }
  ElMessage.info('文档内定位失败，请使用 OnlyOffice 内搜索（Ctrl+F）或查看右侧意见');
}

function onOnlyOfficeLoadFailed() {
  ElMessage.warning(
    'OnlyOffice 未启动，请执行：cd script/docker && docker compose --env-file docker.env up -d onlyoffice',
    { duration: 8000 },
  );
}

function toggleSidePanel() {
  sideCollapsed.value = !sideCollapsed.value;
  scheduleResize();
}

function expandSidePanel(tab?: WorkbenchSideTab) {
  if (tab) {
    activeSideTab.value = tab;
  }
  sideCollapsed.value = false;
  scheduleResize();
}

function scheduleResize() {
  window.setTimeout(() => {
    window.dispatchEvent(new Event('resize'));
  }, 280);
}

watch(activeSideTab, () => {
  scheduleResize();
});

async function forceSaveWorking() {
  return onlyOfficeRef.value?.forceSaveAndWait?.() ?? false;
}

defineExpose({ forceSaveWorking });
</script>

<template>
  <div class="legal-workbench">
    <section class="legal-workbench-editor">
      <ContractOnlyOfficeViewer
        v-if="canOpenDocument"
        ref="onlyOfficeRef"
        class="min-h-0 flex-1"
        :key="`${contractId ?? 0}-${documentRevision ?? 'init'}`"
        :contract-id="contractId"
        :active-locate-text="activeLocateText"
        :active-locate-paragraph-id="activeLocateParagraphId"
        :locate-nonce="locateNonce"
        :document-revision="documentRevision"
        @document-revision="emit('document-revision', $event)"
        @locate-failed="onOnlyOfficeLocateFailed"
        @load-failed="onOnlyOfficeLoadFailed"
      />
      <div
        v-else
        class="flex h-full items-center justify-center text-sm text-muted-foreground"
      >
        暂无合同文档
      </div>
    </section>

    <aside
      class="legal-workbench-aside"
      :class="{ 'is-collapsed': sideCollapsed }"
    >
      <button
        type="button"
        class="legal-workbench-aside-toggle"
        :title="sideCollapsed ? '展开侧栏' : '收起侧栏'"
        @click="toggleSidePanel"
      >
        <IconifyIcon
          :icon="sideCollapsed ? 'lucide:chevron-left' : 'lucide:chevron-right'"
          class="size-4"
        />
      </button>

      <div v-if="sideCollapsed" class="legal-workbench-aside-rail">
        <button
          v-for="tab in sideTabs"
          :key="tab.key"
          type="button"
          class="legal-workbench-aside-rail-btn"
          :class="{ 'is-active': activeSideTab === tab.key }"
          :title="tab.label"
          @click="expandSidePanel(tab.key)"
        >
          <IconifyIcon :icon="tab.icon" class="size-4 shrink-0" />
          <span class="legal-workbench-aside-rail-title">{{ tab.label }}</span>
          <span
            v-if="tab.badge && tab.badge !== '●'"
            class="legal-workbench-aside-rail-badge"
            :class="tab.badgeType === 'warning' ? 'is-warning' : 'is-info'"
          >
            {{ tab.badge }}
          </span>
          <span
            v-else-if="tab.badge === '●'"
            class="legal-workbench-aside-rail-dot"
          />
        </button>
      </div>

      <div v-else class="legal-workbench-aside-panel">
        <ElTabs v-model="activeSideTab" class="legal-workbench-side-tabs">
          <ElTabPane name="opinions">
            <template #label>
              <span class="legal-workbench-side-tab-label">
                <IconifyIcon
                  icon="lucide:clipboard-list"
                  class="size-3.5 shrink-0"
                />
                <span>审核意见</span>
                <span
                  v-if="
                    (pendingOpinionCount ?? 0) > 0 ||
                    (totalOpinionCount ?? 0) > 0
                  "
                  class="legal-workbench-side-tab-badge"
                  :class="
                    (pendingOpinionCount ?? 0) > 0 ? 'is-warning' : 'is-info'
                  "
                >
                  {{
                    (pendingOpinionCount ?? 0) > 0
                      ? pendingOpinionCount
                      : totalOpinionCount
                  }}
                </span>
              </span>
            </template>
            <div class="legal-workbench-aside-pane">
              <slot name="opinions" />
            </div>
          </ElTabPane>

          <ElTabPane name="chat">
            <template #label>
              <span class="legal-workbench-side-tab-label">
                <IconifyIcon
                  icon="lucide:message-square"
                  class="size-3.5 shrink-0"
                />
                <span>AI问答</span>
              </span>
            </template>
            <div class="legal-workbench-aside-pane">
              <slot name="chat" />
            </div>
          </ElTabPane>

          <ElTabPane name="report">
            <template #label>
              <span class="legal-workbench-side-tab-label">
                <IconifyIcon icon="lucide:file-text" class="size-3.5 shrink-0" />
                <span>审核报告</span>
                <span
                  v-if="hasAuditReport"
                  class="legal-workbench-side-tab-dot"
                />
              </span>
            </template>
            <div class="legal-workbench-aside-pane">
              <slot name="report" />
            </div>
          </ElTabPane>

          <ElTabPane name="download">
            <template #label>
              <span class="legal-workbench-side-tab-label">
                <IconifyIcon icon="lucide:download" class="size-3.5 shrink-0" />
                <span>下载</span>
              </span>
            </template>
            <div class="legal-workbench-aside-pane">
              <slot name="download" />
            </div>
          </ElTabPane>
        </ElTabs>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.legal-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: minmax(0, 1fr);
  gap: 12px;
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.legal-workbench-editor {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.legal-workbench-aside {
  position: relative;
  display: flex;
  width: 440px;
  min-width: 440px;
  height: 100%;
  min-height: 0;
  transition: width 0.22s ease, min-width 0.22s ease;
}

.legal-workbench-aside.is-collapsed {
  width: 40px;
  min-width: 40px;
}

.legal-workbench-aside-toggle {
  position: absolute;
  left: -12px;
  top: 50%;
  z-index: 3;
  display: flex;
  height: 48px;
  width: 24px;
  transform: translateY(-50%);
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 9999px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  box-shadow: 0 1px 4px rgb(0 0 0 / 8%);
  cursor: pointer;
}

.legal-workbench-aside-toggle:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.legal-workbench-aside-panel {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.legal-workbench-side-tabs {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.legal-workbench-side-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin: 0;
  padding: 6px 8px 0;
}

.legal-workbench-side-tabs :deep(.el-tabs__nav-wrap) {
  margin-bottom: 0;
}

.legal-workbench-side-tabs :deep(.el-tabs__nav-scroll) {
  overflow-x: auto;
  scrollbar-width: none;
}

.legal-workbench-side-tabs :deep(.el-tabs__nav-scroll::-webkit-scrollbar) {
  display: none;
}

.legal-workbench-side-tabs :deep(.el-tabs__item) {
  height: 34px;
  padding: 0 10px;
  font-size: 12px;
}

.legal-workbench-side-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.legal-workbench-side-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.legal-workbench-side-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.legal-workbench-side-tab-badge {
  display: inline-flex;
  min-width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 600;
}

.legal-workbench-side-tab-badge.is-warning {
  background: var(--el-color-warning);
  color: #fff;
}

.legal-workbench-side-tab-badge.is-info {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}

.legal-workbench-side-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--el-color-success);
}

.legal-workbench-aside-pane {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.legal-workbench-aside-rail {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  overflow-y: auto;
  border-left: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  border-radius: 0 8px 8px 0;
  padding: 8px 0;
}

.legal-workbench-aside-rail-btn {
  display: flex;
  width: 100%;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 2px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--el-text-color-regular);
}

.legal-workbench-aside-rail-btn:hover,
.legal-workbench-aside-rail-btn.is-active {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}

.legal-workbench-aside-rail-title {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 1px;
}

.legal-workbench-aside-rail-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background: var(--el-color-success);
}

.legal-workbench-aside-rail-badge {
  display: inline-flex;
  min-width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 600;
}

.legal-workbench-aside-rail-badge.is-warning {
  background: var(--el-color-warning);
  color: #fff;
}

.legal-workbench-aside-rail-badge.is-info {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}
</style>
