<script lang="ts" setup>
import type { MascotCharacter } from './mascot-speech.types';

import { computed } from 'vue';

import { MASCOT_BUBBLE_ANCHORS, MASCOT_CHARACTER_META } from './mascot-speech.types';

defineOptions({ name: 'MascotSpeechBubble' });

const props = defineProps<{
  character: MascotCharacter;
  message: string;
}>();

const anchor = computed(() => MASCOT_BUBBLE_ANCHORS[props.character]);
const meta = computed(() => MASCOT_CHARACTER_META[props.character]);

const positionStyle = computed(() => ({
  left: anchor.value.left,
  bottom: anchor.value.bottom,
  '--bubble-accent': meta.value.accent,
  transform: 'translateX(-50%)',
}));
</script>

<template>
  <div
    class="mascot-bubble pointer-events-none absolute z-50 max-w-[168px] mascot-bubble--enter"
    :class="`mascot-bubble--${character}`"
    :style="positionStyle"
  >
      <div class="mascot-bubble__cat">
        <span class="mascot-bubble__ear mascot-bubble__ear--left" />
        <span class="mascot-bubble__ear mascot-bubble__ear--right" />
        <div class="mascot-bubble__body">
          <span class="mascot-bubble__name">{{ meta.label }}</span>
          <p class="mascot-bubble__text">{{ message }}</p>
        </div>
        <span class="mascot-bubble__tail" />
      </div>
    </div>
</template>

<style scoped>
.mascot-bubble {
  --bubble-accent: #6c3ff5;
  transform-origin: bottom center;
}

.mascot-bubble__cat {
  position: relative;
  filter: drop-shadow(0 8px 16px rgb(15 23 42 / 12%));
}

.mascot-bubble__ear {
  position: absolute;
  top: -7px;
  z-index: 1;
  display: block;
  width: 14px;
  height: 14px;
  background: var(--bubble-accent);
  border: 2px solid #fff;
  border-radius: 4px 10px 0 10px;
}

.mascot-bubble__ear--left {
  left: 14px;
  transform: rotate(-28deg);
}

.mascot-bubble__ear--right {
  right: 14px;
  transform: scaleX(-1) rotate(-28deg);
}

.mascot-bubble__body {
  position: relative;
  z-index: 2;
  padding: 10px 12px 12px;
  background: rgb(255 255 255 / 82%);
  backdrop-filter: blur(8px);
  border: 2px solid var(--bubble-accent);
  border-radius: 18px;
}

.dark .mascot-bubble__body {
  background: rgb(30 41 59 / 82%);
}

.mascot-bubble__name {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--bubble-accent);
}

.mascot-bubble__text {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
  color: #334155;
}

.dark .mascot-bubble__text {
  color: #e2e8f0;
}

.mascot-bubble__tail {
  position: absolute;
  bottom: -9px;
  left: 50%;
  z-index: 1;
  display: block;
  width: 14px;
  height: 14px;
  background: rgb(255 255 255 / 82%);
  backdrop-filter: blur(8px);
  border-right: 2px solid var(--bubble-accent);
  border-bottom: 2px solid var(--bubble-accent);
  border-bottom-right-radius: 4px;
  transform: translateX(-50%) rotate(45deg);
}

.dark .mascot-bubble__tail {
  background: rgb(30 41 59 / 82%);
}

.mascot-bubble--orange .mascot-bubble__tail {
  left: 58%;
}

.mascot-bubble--yellow .mascot-bubble__tail {
  left: 42%;
}

.mascot-bubble--enter {
  animation: mascot-bubble-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes mascot-bubble-in {
  0% {
    opacity: 0;
    transform: translateX(-50%) translateY(8px) scale(0.88);
  }

  100% {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .mascot-bubble--enter {
    animation: none !important;
  }
}
</style>
