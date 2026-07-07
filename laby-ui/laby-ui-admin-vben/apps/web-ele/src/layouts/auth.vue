<script lang="ts" setup>
import { computed } from 'vue';

import { VBEN_LOGO_URL } from '@vben/constants';
import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

import { $t } from '#/locales';
import LoginMascotSlogan from '#/views/_core/authentication/components/LoginMascotSlogan.vue';

const appName = computed(() => preferences.app.name);
const logo = VBEN_LOGO_URL;
const logoDark = VBEN_LOGO_URL;
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :logo="logo"
    :logo-dark="logoDark"
    :page-description="$t('authentication.pageDesc')"
    :page-title="$t('authentication.pageTitle')"
  >
    <template #slogan>
      <LoginMascotSlogan />
    </template>
  </AuthPageLayout>
</template>
