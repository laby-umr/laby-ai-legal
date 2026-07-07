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
  /**
   * @zh_CN 文字
   */
  text?: string;
}

defineOptions({
  name: 'VbenLoading',
});

const props = withDefaults(defineProps<Props>(), {
  minLoadingTime: 50,
  text: '',
});
// const startTime = ref(0);
const showSpinner = ref(false);
const renderSpinner = ref(false);
let timer: ReturnType<typeof setTimeout> | undefined;

watch(
  () => props.spinning,
  (show) => {
    if (!show) {
      showSpinner.value = false;
      timer && clearTimeout(timer);
      return;
    }

    // startTime.value = performance.now();
    timer = setTimeout(() => {
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
        'bg-overlay-content dark:bg-overlay absolute top-0 left-0 z-100 flex size-full flex-col items-center justify-center transition-all duration-500',
        {
          'invisible opacity-0': !showSpinner,
        },
        props.class,
      )
    "
    @transitionend="onTransitionEnd"
  >
    <slot name="icon" v-if="renderSpinner">
      <img
        :src="VBEN_LOADING_GIF_URL"
        alt="loading"
        class="size-40 object-contain"
      />
    </slot>

    <div v-if="text" class="text-primary mt-4 text-xs">{{ text }}</div>
    <slot></slot>
  </div>
</template>

