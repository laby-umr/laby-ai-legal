<script lang="ts" setup>
import { ref, watch } from 'vue';

import { VBEN_LOADING_GIF_URL } from '@vben-core/shared/constants';

import { cn } from '@vben-core/shared/utils';

interface Props {
  class?: string;
  /**
   * @zh_CN 最小加载时间
   * @en_US Minimum loading time
   */
  minLoadingTime?: number;
  /**
   * @zh_CN loading状态开启
   */
  spinning?: boolean;
}

defineOptions({
  name: 'VbenSpinner',
});

const props = withDefaults(defineProps<Props>(), {
  minLoadingTime: 50,
});
// const startTime = ref(0);
const showSpinner = ref(false);
const renderSpinner = ref(false);
const timer = ref<ReturnType<typeof setTimeout>>();

watch(
  () => props.spinning,
  (show) => {
    if (!show) {
      showSpinner.value = false;
      clearTimeout(timer.value);
      return;
    }

    // startTime.value = performance.now();
    timer.value = setTimeout(() => {
      // const loadingTime = performance.now() - startTime.value;

      showSpinner.value = true;
      if (showSpinner.value) {
        renderSpinner.value = true;
      }
    }, props.minLoadingTime);
  },
  {
    immediate: true,
  },
);

function onTransitionEnd() {
  if (!showSpinner.value) {
    renderSpinner.value = false;
  }
}
</script>

<template>
  <div
    :class="
      cn(
        'flex-center bg-overlay-content absolute top-0 left-0 z-100 size-full backdrop-blur-xs transition-all duration-500',
        {
          'invisible opacity-0': !showSpinner,
        },
        props.class,
      )
    "
    @transitionend="onTransitionEnd"
  >
    <img
      v-if="renderSpinner"
      :src="VBEN_LOADING_GIF_URL"
      alt="loading"
      class="size-40 object-contain"
    />
  </div>
</template>

