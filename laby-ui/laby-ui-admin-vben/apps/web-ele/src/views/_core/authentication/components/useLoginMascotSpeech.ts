import type { MascotCharacter } from './mascot-speech.types';

import { onUnmounted, ref } from 'vue';

export interface ActiveBubble {
  character: MascotCharacter;
  message: string;
  seq: number;
}

const DEFAULT_DURATION = 3500;

const CHARACTER_CLICK_LINES: Record<MascotCharacter, string> = {
  purple: '我是紫紫，队里最高的那个，欢迎回来～',
  black: '小黑在呢，安全交给我，尽管放心登录！',
  orange: '橙橙被你戳到啦，今天也要元气满满哦！',
  yellow: '小黄在这！填完记得点登录，冲冲冲～',
};

export function useLoginMascotSpeech() {
  const activeBubble = ref<ActiveBubble | null>(null);

  let hideTimer: ReturnType<typeof setTimeout> | undefined;
  let seq = 0;

  function clearHideTimer() {
    if (hideTimer) {
      clearTimeout(hideTimer);
      hideTimer = undefined;
    }
  }

  /** 立即替换当前气泡，仅保留一个，可重复触发 */
  function say(
    character: MascotCharacter,
    message: string,
    duration = DEFAULT_DURATION,
  ) {
    clearHideTimer();
    seq += 1;
    const currentSeq = seq;
    activeBubble.value = { character, message, seq: currentSeq };

    hideTimer = setTimeout(() => {
      if (activeBubble.value?.seq === currentSeq) {
        activeBubble.value = null;
      }
    }, duration);
  }

  function onCharacterLanded(character: MascotCharacter) {
    if (character !== 'purple') return;
    say('purple', '哎哟…着陆成功！欢迎回来呀～');
  }

  function onUsernameFocus() {
    say('yellow', '可以勾选「记住账号」，下次就不用重复填啦～');
  }

  function onPasswordFocus() {
    say('black', '密码看起来好像没有问题～');
  }

  function onCharacterClick(character: MascotCharacter) {
    say(character, CHARACTER_CLICK_LINES[character]);
  }

  function stop() {
    clearHideTimer();
    activeBubble.value = null;
  }

  onUnmounted(stop);

  return {
    activeBubble,
    onCharacterClick,
    onCharacterLanded,
    onPasswordFocus,
    onUsernameFocus,
    stop,
  };
}
