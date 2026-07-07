<template>
  <div class="relative overflow-visible" :style="{ width: '550px', height: '400px' }">
    <!-- Purple Character (Back) -->
    <div
      ref="purpleRef"
      class="absolute bottom-0 cursor-pointer"
      :style="purpleStyle"
      @click="handleCharacterClick('purple')"
    >
      <div class="absolute flex gap-8" :style="purpleEyesOffset">
        <template v-if="dropState.purple.pain">
          <PainEye side="left" :size="13" with-socket />
          <PainEye side="right" :size="13" with-socket />
        </template>
        <template v-else>
          <EyeBall
            :size="18"
            :pupil-size="7"
            :max-distance="5"
            eye-color="white"
            pupil-color="#2D2D2D"
            :is-blinking="isPurpleBlinking"
            :force-look-x="purpleForceLookX"
            :force-look-y="purpleForceLookY"
            :mouse-pos="mousePos"
          />
          <EyeBall
            :size="18"
            :pupil-size="7"
            :max-distance="5"
            eye-color="white"
            pupil-color="#2D2D2D"
            :is-blinking="isPurpleBlinking"
            :force-look-x="purpleForceLookX"
            :force-look-y="purpleForceLookY"
            :mouse-pos="mousePos"
          />
        </template>
      </div>
    </div>

    <!-- Black Character (Middle) -->
    <div
      ref="blackRef"
      class="absolute bottom-0 cursor-pointer"
      :style="blackStyle"
      @click="handleCharacterClick('black')"
    >
      <div class="absolute flex gap-6" :style="blackEyesOffset">
        <template v-if="dropState.black.pain">
          <PainEye side="left" :size="12" with-socket />
          <PainEye side="right" :size="12" with-socket />
        </template>
        <template v-else>
          <EyeBall
            :size="16"
            :pupil-size="6"
            :max-distance="4"
            eye-color="white"
            pupil-color="#2D2D2D"
            :is-blinking="isBlackBlinking"
            :force-look-x="blackForceLookX"
            :force-look-y="blackForceLookY"
            :mouse-pos="mousePos"
          />
          <EyeBall
            :size="16"
            :pupil-size="6"
            :max-distance="4"
            eye-color="white"
            pupil-color="#2D2D2D"
            :is-blinking="isBlackBlinking"
            :force-look-x="blackForceLookX"
            :force-look-y="blackForceLookY"
            :mouse-pos="mousePos"
          />
        </template>
      </div>
    </div>

    <!-- Orange Character (Front Left) -->
    <div
      ref="orangeRef"
      class="absolute bottom-0 cursor-pointer"
      :style="orangeStyle"
      @click="handleCharacterClick('orange')"
    >
      <div class="absolute flex gap-8" :style="orangeEyesOffset">
        <template v-if="dropState.orange.pain">
          <PainEye side="left" :size="16" />
          <PainEye side="right" :size="16" />
        </template>
        <template v-else>
          <Pupil
            :size="12"
            :max-distance="5"
            pupil-color="#2D2D2D"
            :force-look-x="frontForceLookX"
            :force-look-y="frontForceLookY"
            :mouse-pos="mousePos"
          />
          <Pupil
            :size="12"
            :max-distance="5"
            pupil-color="#2D2D2D"
            :force-look-x="frontForceLookX"
            :force-look-y="frontForceLookY"
            :mouse-pos="mousePos"
          />
        </template>
      </div>
    </div>

    <!-- Yellow Character (Front Right) -->
    <div
      ref="yellowRef"
      class="absolute bottom-0 cursor-pointer"
      :style="yellowStyle"
      @click="handleCharacterClick('yellow')"
    >
      <div class="absolute flex gap-6" :style="yellowEyesOffset">
        <template v-if="dropState.yellow.pain">
          <PainEye side="left" :size="16" />
          <PainEye side="right" :size="16" />
        </template>
        <template v-else>
          <Pupil
            :size="12"
            :max-distance="5"
            pupil-color="#2D2D2D"
            :force-look-x="frontForceLookX"
            :force-look-y="frontForceLookY"
            :mouse-pos="mousePos"
          />
          <Pupil
            :size="12"
            :max-distance="5"
            pupil-color="#2D2D2D"
            :force-look-x="frontForceLookX"
            :force-look-y="frontForceLookY"
            :mouse-pos="mousePos"
          />
        </template>
      </div>
      <div
        class="absolute rounded-full bg-[#2D2D2D] transition-all duration-200 ease-out"
        :style="yellowMouthOffset"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';

import EyeBall from './EyeBall.vue';
import PainEye from './PainEye.vue';
import Pupil from './Pupil.vue';

const props = defineProps({
  isTyping: { type: Boolean, default: false },
  showPassword: { type: Boolean, default: false },
  passwordLength: { type: Number, default: 0 },
  lookAtOffset: { type: Number, default: 0 },
  isExcited: { type: Boolean, default: false },
});

const emit = defineEmits<{
  characterClick: [character: CharacterKey];
  landed: [character: CharacterKey];
}>();

type CharacterKey = 'purple' | 'black' | 'orange' | 'yellow';

interface DropState {
  pain: boolean;
  squashX: number;
  squashY: number;
  y: number;
}

const DROP_START_Y = -480;
const DROP_DURATION = 920;
const PAIN_HOLD_MS = 750;

const mousePos = reactive({ x: 0, y: 0 });
const idleOffset = ref(0);
const purpleRef = ref<HTMLElement | null>(null);
const blackRef = ref<HTMLElement | null>(null);
const orangeRef = ref<HTMLElement | null>(null);
const yellowRef = ref<HTMLElement | null>(null);

const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);
const isLookingAtEachOther = ref(false);
const isPurplePeeking = ref(false);

const dropAnimating = reactive<Record<CharacterKey, boolean>>({
  purple: true,
  black: true,
  orange: true,
  yellow: true,
});

const dropState = reactive<Record<CharacterKey, DropState>>({
  purple: { y: DROP_START_Y, squashY: 1, squashX: 1, pain: false },
  black: { y: DROP_START_Y, squashY: 1, squashX: 1, pain: false },
  orange: { y: DROP_START_Y, squashY: 1, squashX: 1, pain: false },
  yellow: { y: DROP_START_Y, squashY: 1, squashX: 1, pain: false },
});

function notifyLanded(key: CharacterKey) {
  emit('landed', key);
}

function handleCharacterClick(key: CharacterKey) {
  emit('characterClick', key);
}

/** 下落 + 碰撞挤压 + 回弹采样 (t: 0~1) */
function sampleDropFrame(t: number) {
  if (t < 0.62) {
    const p = t / 0.62;
    const ease = p * p;
    return {
      pain: false,
      squashX: 1,
      squashY: 1,
      y: DROP_START_Y * (1 - ease),
    };
  }

  if (t < 0.7) {
    const p = (t - 0.62) / 0.08;
    return {
      pain: p > 0.25,
      squashX: 1 + 0.16 * p,
      squashY: 1 - 0.22 * p,
      y: 26 * p,
    };
  }

  if (t < 0.82) {
    const p = (t - 0.7) / 0.12;
    return {
      pain: true,
      squashX: 1.16 - 0.08 * p,
      squashY: 0.78 + 0.1 * p,
      y: 26 - 58 * p,
    };
  }

  if (t < 0.92) {
    const p = (t - 0.82) / 0.1;
    return {
      pain: p < 0.55,
      squashX: 1.08 - 0.06 * p,
      squashY: 0.88 + 0.07 * p,
      y: -32 + 38 * p,
    };
  }

  const p = (t - 0.92) / 0.08;
  return {
    pain: false,
    squashX: 1.02 - 0.02 * p,
    squashY: 0.95 + 0.05 * p,
    y: 6 * (1 - p),
  };
}

function applyDropSample(key: CharacterKey, sample: DropState) {
  dropState[key].y = sample.y;
  dropState[key].squashY = sample.squashY;
  dropState[key].squashX = sample.squashX;
  dropState[key].pain = sample.pain;
}

function animateCharacterDrop(key: CharacterKey, delayMs: number) {
  window.setTimeout(() => {
    dropAnimating[key] = true;
    const start = performance.now();
    let painTimer: ReturnType<typeof setTimeout> | undefined;

    const tick = (now: number) => {
      const t = Math.min((now - start) / DROP_DURATION, 1);
      applyDropSample(key, sampleDropFrame(t));

      if (t < 1) {
        requestAnimationFrame(tick);
        return;
      }

      dropState[key].y = 0;
      dropState[key].squashY = 1;
      dropState[key].squashX = 1;
      dropState[key].pain = true;
      dropAnimating[key] = false;

      if (painTimer) clearTimeout(painTimer);
      painTimer = setTimeout(() => {
        dropState[key].pain = false;
        notifyLanded(key);
      }, PAIN_HOLD_MS);
    };

    requestAnimationFrame(tick);
  }, delayMs);
}

function playDropEntrance() {
  const prefersReducedMotion = window.matchMedia(
    '(prefers-reduced-motion: reduce)',
  ).matches;

  if (prefersReducedMotion) {
    (['purple', 'black', 'orange', 'yellow'] as CharacterKey[]).forEach(
      (key) => {
        dropAnimating[key] = false;
        applyDropSample(key, { y: 0, squashY: 1, squashX: 1, pain: false });
      },
    );
    window.setTimeout(() => {
      emit('landed', 'purple');
    }, 300);
    return;
  }

  const delays: Record<CharacterKey, number> = {
    purple: 80,
    black: 200,
    orange: 320,
    yellow: 440,
  };

  (Object.keys(delays) as CharacterKey[]).forEach((key) => {
    animateCharacterDrop(key, delays[key]);
  });
}

function withDropTransform(transform: string, key: CharacterKey) {
  const s = dropState[key];
  return `${transform} translateY(${s.y}px) scale(${s.squashX}, ${s.squashY})`;
}

function getBodyTransition(key: CharacterKey) {
  return dropAnimating[key]
    ? 'none'
    : 'all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1)';
}

function getBodyOpacity(key: CharacterKey) {
  return dropState[key].y <= DROP_START_Y + 40 ? 0 : 1;
}

const handleMouseMove = (e: MouseEvent) => {
  mousePos.x = e.clientX;
  mousePos.y = e.clientY;
};

let idleReq = 0;
const updateIdle = () => {
  const speed = props.isExcited ? 3000 : 1000;
  const magnitude = props.isExcited ? 12 : 5;
  idleOffset.value =
    Math.sin((Date.now() / speed) * (props.isExcited ? 10 : 1)) * magnitude;
  idleReq = requestAnimationFrame(updateIdle);
};

const setupBlink = (blinkRef: { value: boolean }, min: number, max: number) => {
  let timeout: ReturnType<typeof setTimeout> | undefined;
  const schedule = () => {
    timeout = setTimeout(() => {
      blinkRef.value = true;
      setTimeout(() => {
        blinkRef.value = false;
        schedule();
      }, 150);
    }, Math.random() * (max - min) + min);
  };
  schedule();
  return () => {
    if (timeout) clearTimeout(timeout);
  };
};

let clearPurpleBlink: (() => void) | undefined;
let clearBlackBlink: (() => void) | undefined;

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove);
  updateIdle();
  clearPurpleBlink = setupBlink(isPurpleBlinking, 3000, 7000);
  clearBlackBlink = setupBlink(isBlackBlinking, 4000, 8000);

  requestAnimationFrame(() => {
    playDropEntrance();
  });
});

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove);
  cancelAnimationFrame(idleReq);
  clearPurpleBlink?.();
  clearBlackBlink?.();
});

watch(
  () => props.isTyping,
  (val) => {
    if (val) {
      isLookingAtEachOther.value = true;
      setTimeout(() => {
        isLookingAtEachOther.value = false;
      }, 800);
    }
  },
);

watch(
  () => [props.passwordLength, props.showPassword],
  ([len, show]) => {
    if (Number(len) > 0 && show) {
      const schedule = () => {
        if (!(Number(props.passwordLength) > 0 && props.showPassword)) return;
        isPurplePeeking.value = true;
        setTimeout(() => {
          isPurplePeeking.value = false;
          if (Number(props.passwordLength) > 0 && props.showPassword) {
            setTimeout(schedule, Math.random() * 3000 + 2000);
          }
        }, 800);
      };
      schedule();
    } else {
      isPurplePeeking.value = false;
    }
  },
);

const calculatePosition = (el: HTMLElement | null) => {
  if (!el) return { faceX: 0, faceY: 0, bodySkew: 0 };
  const rect = el.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 3;

  const isHiding = props.passwordLength > 0 && !props.showPassword;
  const deltaX = isHiding ? -400 : mousePos.x - centerX;
  const deltaY = mousePos.y - centerY;

  let baseFaceY = Math.max(-10, Math.min(10, deltaY / 30));
  if (props.isTyping && !isHiding) baseFaceY += 5;

  const verticalShift = props.lookAtOffset * 15;

  return {
    faceX: Math.max(-25, Math.min(25, deltaX / 15)),
    faceY: Math.max(-15, Math.min(15, baseFaceY + verticalShift)),
    bodySkew: Math.max(-8, Math.min(10, -deltaX / 80)),
  };
};

const isHidingPassword = computed(
  () => props.passwordLength > 0 && !props.showPassword,
);

const purpleStyle = computed(() => {
  const pos = calculatePosition(purpleRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  let transform = `skewX(${pos.bodySkew}deg) translateY(${idleOffset.value}px)`;
  if (peeking) transform = `skewX(0deg) translateY(${idleOffset.value}px)`;
  else if (isHidingPassword.value)
    transform = `skewX(${-25}deg) translateX(-80px) translateY(${idleOffset.value}px)`;
  else if (props.isTyping)
    transform = `skewX(${pos.bodySkew - 12}deg) translateX(40px) translateY(${idleOffset.value}px)`;

  transform = withDropTransform(transform, 'purple');

  return {
    left: '70px',
    width: '180px',
    height: isHidingPassword.value ? '420px' : props.isTyping ? '440px' : '400px',
    backgroundColor: '#6C3FF5',
    borderRadius: '10px 10px 0 0',
    zIndex: 1,
    transform,
    transformOrigin: 'bottom center',
    transition: getBodyTransition('purple'),
    opacity: getBodyOpacity('purple'),
  };
});

const purpleEyesOffset = computed(() => {
  const pos = calculatePosition(purpleRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (isHidingPassword.value) return { left: '10px', top: '50px', opacity: '0.8' };
  return {
    left: peeking ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + pos.faceX}px`,
    top: peeking ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + pos.faceY}px`,
    transition: dropAnimating.purple ? 'none' : 'all 0.6s ease-in-out',
  };
});

const blackStyle = computed(() => {
  const pos = calculatePosition(blackRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  let transform = `skewX(${pos.bodySkew}deg) translateY(${idleOffset.value * 0.8}px)`;
  if (peeking) transform = `skewX(0deg) translateY(${idleOffset.value * 0.8}px)`;
  else if (isHidingPassword.value)
    transform = `skewX(${-20}deg) translateX(-50px) translateY(${idleOffset.value * 0.8}px)`;
  else if (isLookingAtEachOther.value)
    transform = `skewX(${pos.bodySkew * 1.5 + 10}deg) translateX(20px) translateY(${idleOffset.value * 0.8}px)`;
  else if (props.isTyping)
    transform = `skewX(${pos.bodySkew * 1.5}deg) translateY(${idleOffset.value * 0.8}px)`;

  transform = withDropTransform(transform, 'black');

  return {
    left: '240px',
    width: '120px',
    height: '310px',
    backgroundColor: '#2D2D2D',
    borderRadius: '8px 8px 0 0',
    zIndex: 2,
    transform,
    transformOrigin: 'bottom center',
    transition: getBodyTransition('black'),
    opacity: getBodyOpacity('black'),
  };
});

const blackEyesOffset = computed(() => {
  const pos = calculatePosition(blackRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (isHidingPassword.value) return { left: '5px', top: '40px', opacity: '0.8' };
  return {
    left: peeking ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + pos.faceX}px`,
    top: peeking ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + pos.faceY}px`,
    transition: dropAnimating.black ? 'none' : 'all 0.6s ease-in-out',
  };
});

const orangeStyle = computed(() => {
  const pos = calculatePosition(orangeRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  let transform = peeking
    ? 'skewX(0deg)'
    : `skewX(${pos.bodySkew}deg) translateY(${idleOffset.value * 1.2}px)`;
  transform = withDropTransform(transform, 'orange');

  return {
    left: '0px',
    width: '240px',
    height: isHidingPassword.value ? '160px' : '200px',
    zIndex: 3,
    backgroundColor: '#FF9B6B',
    borderRadius: '120px 120px 0 0',
    transform,
    transformOrigin: 'bottom center',
    transition: getBodyTransition('orange'),
    opacity: getBodyOpacity('orange'),
  };
});

const orangeEyesOffset = computed(() => {
  const pos = calculatePosition(orangeRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (isHidingPassword.value) return { left: '20px', top: '70px', opacity: '0.9' };
  return {
    left: peeking ? '50px' : `${82 + pos.faceX}px`,
    top: peeking ? '85px' : `${90 + pos.faceY}px`,
    transition: dropAnimating.orange ? 'none' : 'all 0.2s ease-out',
  };
});

const yellowStyle = computed(() => {
  const pos = calculatePosition(yellowRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  let transform = peeking
    ? 'skewX(0deg)'
    : `skewX(${pos.bodySkew}deg) translateY(${idleOffset.value * 1.1}px)`;
  transform = withDropTransform(transform, 'yellow');

  return {
    left: '310px',
    width: '140px',
    height: isHidingPassword.value ? '180px' : '230px',
    backgroundColor: '#E8D754',
    borderRadius: '70px 70px 0 0',
    zIndex: 4,
    transform,
    transformOrigin: 'bottom center',
    transition: getBodyTransition('yellow'),
    opacity: getBodyOpacity('yellow'),
  };
});

const yellowEyesOffset = computed(() => {
  const pos = calculatePosition(yellowRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (isHidingPassword.value) return { left: '10px', top: '35px', opacity: '0.9' };
  return {
    left: peeking ? '20px' : `${52 + pos.faceX}px`,
    top: peeking ? '35px' : `${40 + pos.faceY}px`,
    transition: dropAnimating.yellow ? 'none' : 'all 0.2s ease-out',
  };
});

const yellowMouthOffset = computed(() => {
  const pos = calculatePosition(yellowRef.value);
  const peeking = props.passwordLength > 0 && props.showPassword;
  const pain = dropState.yellow.pain;

  return {
    left: peeking ? '10px' : `${40 + pos.faceX}px`,
    top: peeking ? '88px' : `${88 + pos.faceY}px`,
    width: pain ? '48px' : '80px',
    height: pain ? '3px' : '4px',
    borderRadius: pain ? '999px' : '9999px',
    transform: pain ? 'rotate(-8deg)' : 'none',
  };
});

const purpleForceLookX = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (peeking) return isPurplePeeking.value ? 4 : -4;
  return isLookingAtEachOther.value ? 3 : undefined;
});
const purpleForceLookY = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (peeking) return isPurplePeeking.value ? 5 : -4;
  return isLookingAtEachOther.value ? 4 : undefined;
});
const blackForceLookX = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (peeking) return -4;
  return isLookingAtEachOther.value ? 0 : undefined;
});
const blackForceLookY = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  if (peeking) return -4;
  return isLookingAtEachOther.value ? -4 : undefined;
});
const frontForceLookX = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  return peeking ? -5 : undefined;
});
const frontForceLookY = computed(() => {
  const peeking = props.passwordLength > 0 && props.showPassword;
  return peeking ? -4 : undefined;
});
</script>
