<template>
  <div class="particles-container">
    <div
      v-for="i in 50"
      :key="i"
      class="particle"
      :style="particleStyle(i)"
    ></div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'GreenParticles' })

const particleStyle = (i: number) => {
  const size = Math.random() * 6 + 2
  const left = Math.random() * 100
  const duration = Math.random() * 15 + 10
  const delay = Math.random() * 10
  const opacity = Math.random() * 0.5 + 0.2
  
  // Use HSL for a variety of greens (meadow style)
  const hue = Math.floor(Math.random() * 40) + 80 // 80-120 (Green)
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    bottom: '-20px',
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: opacity,
    backgroundColor: `hsl(${hue}, 70%, 60%)`,
    boxShadow: `0 0 10px hsl(${hue}, 70%, 60%)`
  }
}
</script>

<style lang="scss" scoped>
.particles-container {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  border-radius: 50%;
  animation: rise linear infinite;
}

@keyframes rise {
  0% {
    transform: translateY(0) scale(1) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-110vh) scale(1.5) rotate(360deg);
    opacity: 0;
  }
}
</style>
