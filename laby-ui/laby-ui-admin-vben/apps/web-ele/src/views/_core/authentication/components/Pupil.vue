<template>
  <div
    ref="pupilRef"
    class="rounded-full"
    :style="style"
  ></div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'

const props = defineProps({
  size: { type: Number, default: 12 },
  maxDistance: { type: Number, default: 5 },
  pupilColor: { type: String, default: 'black' },
  forceLookX: { type: Number, default: undefined },
  forceLookY: { type: Number, default: undefined },
  mousePos: { type: Object, required: true }
})

const pupilRef = ref<HTMLElement | null>(null)

const style = computed(() => {
  if (!pupilRef.value) return {}
  let x = 0, y = 0
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    x = props.forceLookX
    y = props.forceLookY
  } else {
    const rect = pupilRef.value.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    const deltaX = props.mousePos.x - centerX
    const deltaY = props.mousePos.y - centerY
    const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), props.maxDistance || 5)
    const angle = Math.atan2(deltaY, deltaX)
    x = Math.cos(angle) * distance
    y = Math.sin(angle) * distance
  }
  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    backgroundColor: props.pupilColor,
    transform: `translate(${x}px, ${y}px)`,
    transition: 'transform 0.2s ease-out'
  }
})
</script>
