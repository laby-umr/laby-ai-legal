<script lang="ts" setup>
import { onMounted, onUnmounted, ref } from 'vue';

import AnimatedCharacters from './AnimatedCharacters.vue';
import MascotSpeechBubble from './MascotSpeechBubble.vue';
import type { MascotCharacter } from './mascot-speech.types';
import { useLoginMascotSpeech } from './useLoginMascotSpeech';

defineOptions({ name: 'LoginMascotSlogan' });

const isTyping = ref(false);
const showPassword = ref(false);
const passwordLength = ref(0);
const lookAtOffset = ref(0);
const isExcited = ref(false);
const showGroundShadow = ref(false);
const focusedField = ref<'none' | 'password' | 'username'>('none');

const {
  activeBubble,
  onCharacterClick,
  onCharacterLanded,
  onPasswordFocus,
  onUsernameFocus,
  stop: stopSpeech,
} = useLoginMascotSpeech();

function playDropEntrance() {
  window.setTimeout(() => {
    showGroundShadow.value = true;
  }, 1150);
}

function isPasswordInput(target: HTMLInputElement) {
  return (
    target.type === 'password' ||
    target.id?.toLowerCase().includes('password') ||
    target.name?.toLowerCase().includes('password') ||
    target.getAttribute('autocomplete')?.includes('password')
  );
}

function isUsernameInput(target: HTMLInputElement) {
  return (
    target.id?.toLowerCase().includes('username') ||
    target.name?.toLowerCase().includes('username') ||
    target.getAttribute('autocomplete') === 'username'
  );
}

function updateLookAtOffset(target: HTMLInputElement) {
  const rect = target.getBoundingClientRect();
  const relativePos = (rect.top + rect.height / 2) / window.innerHeight;
  lookAtOffset.value = (relativePos - 0.5) * 2;
}

function handleFocusin(e: FocusEvent) {
  const target = e.target as HTMLElement;
  if (!target || (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA')) {
    return;
  }

  const input = target as HTMLInputElement;
  isTyping.value = true;
  updateLookAtOffset(input);

  if (isPasswordInput(input)) {
    focusedField.value = 'password';
    passwordLength.value = input.value.length;
    showPassword.value = input.type === 'text';
    if (input.value.length > 0) {
      onPasswordFocus();
    }
  } else if (isUsernameInput(input)) {
    focusedField.value = 'username';
    passwordLength.value = 0;
    showPassword.value = false;
    onUsernameFocus();
  } else {
    focusedField.value = 'none';
    passwordLength.value = 0;
    showPassword.value = false;
  }

  const formItemText =
    input.closest('.el-form-item')?.textContent ||
    input.closest('form')?.textContent ||
    '';
  const isSpecial =
    input.placeholder?.includes('验证码') ||
    input.id?.toLowerCase().includes('code') ||
    input.name?.toLowerCase().includes('code') ||
    formItemText.includes('注册') ||
    formItemText.includes('验证码');
  isExcited.value = !!isSpecial;
}

function handleInput(e: Event) {
  const target = e.target as HTMLInputElement;
  if (!target) return;

  if (isPasswordInput(target)) {
    const prevLength = passwordLength.value;
    passwordLength.value = target.value.length;
    showPassword.value = target.type === 'text';
    if (target.value.length > 0 && prevLength === 0) {
      onPasswordFocus();
    }
    return;
  }
}

function handleFocusout(e: FocusEvent) {
  const related = e.relatedTarget as HTMLElement | null;
  if (
    related &&
    (related.tagName === 'INPUT' || related.tagName === 'TEXTAREA')
  ) {
    return;
  }
  isTyping.value = false;
  focusedField.value = 'none';
  passwordLength.value = 0;
  showPassword.value = false;
  isExcited.value = false;
  lookAtOffset.value = 0;
}

function handleCharacterLanded(character: MascotCharacter) {
  onCharacterLanded(character);
}

function handleCharacterClick(character: MascotCharacter) {
  onCharacterClick(character);
}

onMounted(() => {
  window.addEventListener('focusin', handleFocusin);
  window.addEventListener('focusout', handleFocusout);
  window.addEventListener('input', handleInput, true);
  playDropEntrance();
});

onUnmounted(() => {
  window.removeEventListener('focusin', handleFocusin);
  window.removeEventListener('focusout', handleFocusout);
  window.removeEventListener('input', handleInput, true);
  stopSpeech();
});
</script>

<template>
  <div
    class="relative flex min-h-[360px] w-full max-w-[580px] items-end justify-center overflow-visible lg:min-h-[400px] xl:min-h-[440px]"
  >
    <div
      class="absolute bottom-6 left-1/2 h-10 w-[72%] -translate-x-1/2 scale-x-110 rounded-[100%] bg-black/5 blur-2xl transition-all duration-700 dark:bg-white/8"
      :class="showGroundShadow ? 'scale-x-110 opacity-100' : 'scale-x-50 opacity-0'"
    />
    <div
      class="origin-bottom scale-[0.88] lg:scale-[0.96] xl:scale-100 2xl:scale-[1.05]"
    >
      <div
        class="relative overflow-visible pt-[100px]"
        style="width: 550px"
      >
        <div class="relative overflow-visible" style="width: 550px; height: 400px">
          <AnimatedCharacters
            :is-typing="isTyping"
            :show-password="showPassword"
            :password-length="passwordLength"
            :look-at-offset="lookAtOffset"
            :is-excited="isExcited"
            @landed="handleCharacterLanded"
            @character-click="handleCharacterClick"
          />
          <MascotSpeechBubble
            v-if="activeBubble"
            :key="activeBubble.seq"
            :character="activeBubble.character"
            :message="activeBubble.message"
          />
        </div>
      </div>
    </div>
  </div>
</template>
