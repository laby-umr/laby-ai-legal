<template>
  <div
    ref="eyeRef"
    class="rounded-full flex items-center justify-center transition-all duration-150"
    :style="style"
  >
    <Pupil
      v-if="!isBlinking"
      :size="pupilSize"
      :max-distance="maxDistance"
      :pupil-color="pupilColor"
      :force-look-x="forceLookX"
      :force-look-y="forceLookY"
      :mouse-pos="mousePos"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import Pupil from './Pupil.vue'

const props = defineProps({
  size: { type: Number, default: 48 },
  pupilSize: { type: Number, default: 16 },
  maxDistance: { type: Number, default: 10 },
  eyeColor: { type: String, default: 'white' },
  pupilColor: { type: String, default: 'black' },
  isBlinking: { type: Boolean, default: false },
  forceLookX: { type: Number, default: undefined },
  forceLookY: { type: Number, default: undefined },
  mousePos: { type: Object, required: true }
})

const style = computed(() => ({
  width: `${props.size}px`,
  height: props.isBlinking ? '2px' : `${props.size}px`,
  backgroundColor: props.eyeColor,
  overflow: 'hidden'
}))
</script>
