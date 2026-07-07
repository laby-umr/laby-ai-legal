<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { computed, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { usePreferences } from '@vben/preferences';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { formatDateTime } from '@vben/utils';

import dayjs from 'dayjs';
import {
  ElButton,
  ElCard,
  ElCol,
  ElDatePicker,
  ElRow,
  ElSegmented,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { getMockAnalyticsBundle, OVERVIEW_METRICS } from '../mock-data';
import {
  getAiTrendChartOption,
  getBpmTrendChartOption,
  getContractTrendChartOption,
  getCrmChartTheme,
  getFunnelChartOption,
  getRankBarChartOption,
} from './chart-options';

defineOptions({ name: 'DashboardKanban' });

const { isDark } = usePreferences();

const loading = ref(false);
const statTime = ref(formatDateTime(new Date()));
const dateRange = ref<[Date, Date]>([
  dayjs().subtract(30, 'day').toDate(),
  dayjs().toDate(),
]);

const rangePreset = ref<number | string>(30);
const rangeOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
  { label: '近90天', value: 90 },
];

const metrics = ref(getMockAnalyticsBundle(dateRange.value[0], dateRange.value[1]).metrics);
const rankList = ref(getMockAnalyticsBundle(dateRange.value[0], dateRange.value[1]).rank);
const funnelSummary = ref(getMockAnalyticsBundle(dateRange.value[0], dateRange.value[1]).funnel);

const funnelArchiveRate = computed(() => {
  const { submitted, archived } = funnelSummary.value;
  if (!submitted) return null;
  return Math.round((archived / submitted) * 1000) / 10;
});

const funnelChartRef = ref<EchartsUIType>();
const bpmChartRef = ref<EchartsUIType>();
const aiChartRef = ref<EchartsUIType>();
const contractChartRef = ref<EchartsUIType>();
const rankChartRef = ref<EchartsUIType>();

const { renderEcharts: renderFunnel } = useEcharts(funnelChartRef);
const { renderEcharts: renderBpm } = useEcharts(bpmChartRef);
const { renderEcharts: renderAi } = useEcharts(aiChartRef);
const { renderEcharts: renderContract } = useEcharts(contractChartRef);
const { renderEcharts: renderRank } = useEcharts(rankChartRef);

const metricCards = computed(() => {
  const theme = getCrmChartTheme(isDark.value);
  return [
  {
    key: 'bpm',
    title: 'BPM 待办',
    value: String(metrics.value.bpmTodoCount),
    suffix: '项',
    sub: `本期发起 ${OVERVIEW_METRICS.bpmStartedMonth} 个流程`,
    color: theme.card.customer.color,
    bg: theme.card.customer.bg,
  },
  {
    key: 'contract-review',
    title: '合同审核中',
    value: String(metrics.value.contractInReview),
    suffix: '份',
    sub: '法务 · AI 审核 / 人工复核',
    color: theme.card.deal.color,
    bg: theme.card.deal.bg,
  },
  {
    key: 'ai',
    title: 'AI 对话',
    value: metrics.value.aiChatCount.toLocaleString(),
    suffix: '次',
    sub: `Token 约 ${(OVERVIEW_METRICS.aiTokenMonth / 1000).toFixed(0)}k`,
    color: theme.card.contract.color,
    bg: theme.card.contract.bg,
  },
  {
    key: 'contract-done',
    title: '合同已归档',
    value: String(metrics.value.contractArchived),
    suffix: '份',
    sub: `累计归档 ${OVERVIEW_METRICS.contractArchivedTotal} 份`,
    color: theme.card.receivable.color,
    bg: theme.card.receivable.bg,
  },
];
});

const chartTheme = computed(() => getCrmChartTheme(isDark.value));

function applyRangePreset(days: number) {
  const end = dayjs();
  dateRange.value = [end.subtract(days, 'day').toDate(), end.toDate()];
}

async function handleRangePresetChange(val: number | string) {
  const days = Number(val);
  if (!Number.isNaN(days)) {
    applyRangePreset(days);
    await refresh();
  }
}

async function loadCharts() {
  const bundle = getMockAnalyticsBundle(dateRange.value[0], dateRange.value[1]);
  metrics.value = bundle.metrics;
  rankList.value = bundle.rank;
  funnelSummary.value = bundle.funnel;
  const dark = isDark.value;

  await Promise.all([
    renderFunnel(getFunnelChartOption(bundle.funnel, dark)),
    renderBpm(getBpmTrendChartOption(bundle.bpmTrend, dark)),
    renderAi(getAiTrendChartOption(bundle.aiTrend, dark)),
    renderContract(getContractTrendChartOption(bundle.contractTrend, dark)),
    renderRank(getRankBarChartOption(rankList.value, dark)),
  ]);
}

async function refresh() {
  loading.value = true;
  try {
    await loadCharts();
    statTime.value = formatDateTime(new Date());
  } finally {
    loading.value = false;
  }
}

function handleDateChange() {
  refresh();
}

onMounted(() => refresh());

watch(isDark, () => {
  loadCharts();
});
</script>

<template>
  <Page auto-content-height>
    <div class="flex flex-col gap-4">
      <ElCard :body-style="{ padding: '16px' }" shadow="never">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div class="text-xl font-semibold">数据看板</div>
            <div class="text-sm text-[var(--el-text-color-secondary)]">
              BPM 流程 · AI 能力 · 法务合同审核 业务概览（测试数据）
            </div>
          </div>
          <div class="flex flex-wrap items-center gap-3">
            <ElSegmented
              :model-value="rangePreset"
              :options="rangeOptions"
              @change="handleRangePresetChange"
            />
            <ElDatePicker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              @change="handleDateChange"
            />
            <ElButton :loading="loading" @click="refresh">刷新</ElButton>
          </div>
        </div>
        <ElTag type="info" class="mt-3">演示数据，指标与图表按业务模块展示</ElTag>
      </ElCard>

      <ElRow :gutter="16">
        <ElCol
          v-for="card in metricCards"
          :key="card.key"
          :xs="12"
          :sm="12"
          :md="6"
        >
          <ElCard shadow="never" :body-style="{ padding: '16px' }">
            <div
              class="rounded border border-[var(--el-border-color-lighter)] p-3"
              :style="{ backgroundColor: card.bg }"
            >
              <div class="text-sm text-[var(--el-text-color-secondary)]">
                {{ card.title }}
              </div>
              <div
                class="mt-2 text-2xl font-semibold tabular-nums"
                :style="{ color: card.color }"
              >
                {{ card.value }}
                <span
                  v-if="card.suffix"
                  class="ml-1 text-sm font-normal text-[var(--el-text-color-secondary)]"
                >
                  {{ card.suffix }}
                </span>
              </div>
              <div class="mt-1 text-xs text-[var(--el-text-color-secondary)]">
                {{ card.sub }}
              </div>
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <ElRow :gutter="16">
        <ElCol :xs="24" :lg="12">
          <ElCard shadow="never" :body-style="{ padding: '12px 16px' }">
            <div class="mb-2 flex items-center justify-between">
              <span class="font-semibold">合同审核漏斗</span>
              <span
                v-if="funnelArchiveRate != null"
                class="text-xs text-[var(--el-text-color-secondary)]"
              >
                归档率
                <span class="font-medium" :style="{ color: chartTheme.accent }">
                  {{ funnelArchiveRate }}%
                </span>
              </span>
            </div>
            <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">
              提交审核 → AI 审核完成 → 已归档
            </div>
            <div v-loading="loading" class="relative min-h-[360px]">
              <EchartsUI ref="funnelChartRef" height="360px" />
            </div>
          </ElCard>
        </ElCol>
        <ElCol :xs="24" :lg="12">
          <ElCard shadow="never" :body-style="{ padding: '12px 16px' }">
            <div class="mb-2 font-semibold">BPM 流程趋势</div>
            <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">
              流程发起与办结（待办、已办、抄送等）
            </div>
            <div v-loading="loading" class="relative min-h-[320px]">
              <EchartsUI ref="bpmChartRef" height="320px" />
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <ElRow :gutter="16">
        <ElCol :xs="24" :lg="12">
          <ElCard shadow="never" :body-style="{ padding: '12px 16px' }">
            <div class="mb-2 font-semibold">AI 使用趋势</div>
            <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">
              对话次数与 Token 消耗（聊天、知识库、工作流）
            </div>
            <div v-loading="loading" class="relative min-h-[320px]">
              <EchartsUI ref="aiChartRef" height="320px" />
            </div>
          </ElCard>
        </ElCol>
        <ElCol :xs="24" :lg="12">
          <ElCard shadow="never" :body-style="{ padding: '12px 16px' }">
            <div class="mb-2 font-semibold">合同业务趋势</div>
            <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">
              新增合同与完成归档
            </div>
            <div v-loading="loading" class="relative min-h-[320px]">
              <EchartsUI ref="contractChartRef" height="320px" />
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <ElRow :gutter="16">
        <ElCol :xs="24" :lg="14">
          <ElCard shadow="never" :body-style="{ padding: '12px 16px' }">
            <div class="mb-2 font-semibold">业务处理量 TOP</div>
            <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">
              合同类型与 BPM 流程分类
            </div>
            <div v-loading="loading" class="relative min-h-[360px]">
              <EchartsUI ref="rankChartRef" height="360px" />
            </div>
          </ElCard>
        </ElCol>
        <ElCol :xs="24" :lg="10">
          <ElCard shadow="never">
            <template #header>
              <span class="font-semibold">业务明细</span>
            </template>
            <ElTable
              v-loading="loading"
              :data="rankList"
              size="small"
              stripe
              max-height="360"
            >
              <ElTableColumn label="排名" width="64" align="center">
                <template #default="{ $index }">
                  <ElTag
                    v-if="$index < 3"
                    :type="
                      $index === 0
                        ? 'danger'
                        : $index === 1
                          ? 'warning'
                          : 'success'
                    "
                    size="small"
                  >
                    {{ $index + 1 }}
                  </ElTag>
                  <span v-else>{{ $index + 1 }}</span>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="nickname" label="名称" min-width="100" />
              <ElTableColumn prop="deptName" label="归属" min-width="90" />
              <ElTableColumn label="处理量" align="right" min-width="80">
                <template #default="{ row }">
                  {{ row.count }}
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </ElCol>
      </ElRow>

      <div class="text-center text-sm text-[var(--el-text-color-secondary)]">
        统计时间：{{ statTime }}
      </div>
    </div>
  </Page>
</template>
