<script lang="ts" setup>
defineOptions({ name: 'PainEye' });

withDefaults(
  defineProps<{
    /** 左眼 > 、右眼 < */
    side: 'left' | 'right';
    size?: number;
    /** 带白底眼眶（紫/黑角色） */
    withSocket?: boolean;
  }>(),
  {
    size: 14,
    withSocket: false,
  },
);
</script>

<template>
  <span
    class="inline-flex items-center justify-center font-black leading-none text-[#1e293b] select-none"
    :class="withSocket ? 'rounded-full bg-white' : ''"
    :style="{
      width: withSocket ? `${size + 8}px` : 'auto',
      height: withSocket ? `${size + 8}px` : 'auto',
      fontSize: `${size}px`,
    }"
  >
    {{ side === 'left' ? '>' : '<' }}
  </span>
</template>
