<script lang="ts" setup>
import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import MemoryList from './modules/memory-list.vue';
import UserFactList from './modules/user-fact-list.vue';

defineOptions({ name: 'LegalContractMemory' });

const activeTab = ref<'memory' | 'user-fact'>('memory');

const tabs = [
  {
    key: 'memory' as const,
    label: '情节记忆',
    icon: 'lucide:layers',
  },
  {
    key: 'user-fact' as const,
    label: '用户事实',
    icon: 'lucide:user-round',
  },
];
</script>

<template>
  <Page auto-content-height>
    <div class="legal-memory-page flex h-full min-h-0 flex-col">
      <section
        class="legal-memory-shell flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-card"
      >
        <header class="shrink-0 border-b border-border px-4">
          <nav class="legal-memory-nav flex gap-6" role="tablist" aria-label="记忆类型">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              role="tab"
              class="legal-memory-nav__item"
              :aria-selected="activeTab === tab.key"
              :class="{ 'is-active': activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              <IconifyIcon :icon="tab.icon" class="size-4 shrink-0" />
              <span>{{ tab.label }}</span>
            </button>
          </nav>
        </header>

        <div class="legal-memory-body min-h-0 flex-1">
          <MemoryList v-show="activeTab === 'memory'" />
          <UserFactList v-show="activeTab === 'user-fact'" />
        </div>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.legal-memory-nav__item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: hsl(var(--muted-foreground));
  transition: color 0.2s ease;
}

.legal-memory-nav__item:hover {
  color: hsl(var(--foreground));
}

.legal-memory-nav__item::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  border-radius: 2px 2px 0 0;
  content: '';
  background: hsl(var(--primary));
  opacity: 0;
  transform: scaleX(0.6);
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.legal-memory-nav__item.is-active {
  color: hsl(var(--primary));
}

.legal-memory-nav__item.is-active::after {
  opacity: 1;
  transform: scaleX(1);
}

.legal-memory-body :deep(.vben-grid) {
  height: 100%;
}

.legal-memory-body :deep(.vxe-grid) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}
</style>
