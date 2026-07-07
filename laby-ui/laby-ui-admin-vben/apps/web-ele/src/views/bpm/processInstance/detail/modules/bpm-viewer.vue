<script setup lang="ts">
import { ref, watch } from 'vue';

import { MyProcessViewer } from '#/views/bpm/components/bpmn-process-designer/package';

defineOptions({ name: 'ProcessInstanceBpmnViewer' });

const processViewerRef = ref<InstanceType<typeof MyProcessViewer>>();

const props = withDefaults(
  defineProps<{
    bpmnXml?: string;
    loading?: boolean; // 是否加载中
    modelView?: object;
  }>(),
  {
    loading: false,
    modelView: () => ({}),
    bpmnXml: '',
  },
);

// BPMN 流程图数据
const view = ref({
  bpmnXml: '',
});

/** 监控 modelView 更新 */
watch(
  () => props.modelView,
  async (newModelView) => {
    // 加载最新
    if (newModelView) {
      // @ts-expect-error: viewer instance type is broader than local ref typing
      view.value = newModelView;
    }
  },
);

/** 监听 bpmnXml */
watch(
  () => props.bpmnXml,
  (value) => {
    view.value.bpmnXml = value;
  },
);

/** Tab 切换到流程图后，容器才有尺寸，需重新 fit */
function fitViewport() {
  processViewerRef.value?.fitViewport?.();
}

defineExpose({ fitViewport });
</script>

<template>
  <div
    v-loading="loading"
    class="relative w-full overflow-hidden rounded-lg border border-gray-200 bg-white p-4"
    style="height: min(70vh, 720px)"
  >
    <MyProcessViewer
      ref="processViewerRef"
      key="processViewer"
      :xml="view.bpmnXml"
      :view="view"
      class="h-full w-full"
    />
  </div>
</template>
