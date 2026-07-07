<script lang="ts" setup>
import type { LegalContractApi } from '#/api/legal/contract';

import { computed } from 'vue';

import { ElScrollbar, ElTag } from 'element-plus';

const props = defineProps<{
  nodes: LegalContractApi.WorkbenchNavigationNode[];
  mode: 'CLAUSE' | 'PARAGRAPH';
  activeId?: string;
}>();

const emit = defineEmits<{
  select: [node: LegalContractApi.WorkbenchNavigationNode];
}>();

const modeLabel = computed(() =>
  props.mode === 'CLAUSE' ? '条款目录' : '段落列表',
);

function handleSelect(node: LegalContractApi.WorkbenchNavigationNode) {
  emit('select', node);
}

function isActive(node: LegalContractApi.WorkbenchNavigationNode) {
  return props.activeId === node.id;
}
</script>

<template>
  <div
    class="legal-clause-tree flex h-full min-h-0 w-60 shrink-0 flex-col rounded-lg border border-border bg-card"
  >
    <div
      class="flex items-center justify-between border-b border-border px-3 py-2 text-sm font-medium"
    >
      <span>{{ modeLabel }}</span>
      <ElTag size="small" type="info" effect="plain">{{ nodes.length }}</ElTag>
    </div>
    <ElScrollbar class="min-h-0 flex-1">
      <ul class="p-2">
        <li
          v-for="node in nodes"
          :key="node.id"
          class="mb-1 cursor-pointer rounded-md px-2 py-2 text-sm transition-colors hover:bg-muted/60"
          :class="{
            'bg-primary/10 text-primary ring-1 ring-primary/30': isActive(node),
          }"
          :style="{ paddingLeft: `${8 + (node.level || 0) * 12}px` }"
          @click="handleSelect(node)"
        >
          <div class="line-clamp-2 leading-snug">{{ node.label }}</div>
          <div v-if="mode === 'CLAUSE'" class="mt-0.5 text-xs text-muted-foreground">
            {{ node.id }}
          </div>
        </li>
        <li
          v-if="nodes.length === 0"
          class="px-2 py-6 text-center text-xs text-muted-foreground"
        >
          暂无目录
        </li>
      </ul>
    </ElScrollbar>
  </div>
</template>
