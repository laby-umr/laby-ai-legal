import type { useEcharts } from '@vben/plugins/echarts';

import type {
  AiTrendByDate,
  BpmTrendByDate,
  ContractTrendByDate,
  CustomerSummaryByDate,
  FunnelSummary,
  RankItem,
} from '../types';

import {
  createChartStyles,
  getCrmChartTheme,
} from './chart-theme';

export { CRM_CHART_THEME, getCrmChartTheme } from './chart-theme';

type ChartOption = Parameters<ReturnType<typeof useEcharts>['renderEcharts']>[0];
type GradientPair = [string, string];

/** 各图表配色（深浅主题共用 pastel 渐变） */
export const CRM_CHART_PALETTES = {
  /** 漏斗：杏橙 / 天蓝 / 薄荷 */
  funnel: {
    customer: ['#f0a878', '#fce8d4'],
    business: ['#88b4f0', '#d8e8fc'],
    win: ['#78d4b0', '#d2f2e6'],
  },
  /** 商机趋势：天蓝柱 + 蜜桃折线 */
  businessTrend: {
    bar: ['#88b4f0', '#d8e8fc'],
    line: ['#e8a088', '#f5ddd4'],
    lineArea: ['rgba(232,160,136,0.35)', 'rgba(245,221,212,0.04)'],
  },
  /** 客户统计：薄荷 + 淡紫 */
  customerTrend: {
    create: ['#78d4b0', '#d2f2e6'],
    deal: ['#b8a0e8', '#ebe0f8'],
  },
  /** 合同金额：玫瑰粉 */
  contract: {
    line: ['#e898b0', '#f8dce8'],
    area: ['rgba(232,152,176,0.42)', 'rgba(248,220,232,0.05)'],
  },
  /** 排行：每条不同浅色 */
  rank: [
    ['#f0a878', '#fce8d4'],
    ['#88b4f0', '#d8e8fc'],
    ['#78d4b0', '#d2f2e6'],
    ['#e8a088', '#f5ddd4'],
    ['#b8a0e8', '#ebe0f8'],
    ['#e8c878', '#f8eed4'],
    ['#88d4e8', '#d4f0f8'],
    ['#e898b0', '#f8dce8'],
    ['#a8d878', '#e8f4d4'],
    ['#d4a0c8', '#f4e0f0'],
  ] as GradientPair[],
  /** 工作台玫瑰图：粉 / 青 / 杏 */
  miniRose: {
    customer: ['#f0b0c8', '#fce8f0'],
    business: ['#78d0e8', '#d4f0f8'],
    win: ['#e8c878', '#f8eed4'],
  },
} as const;

export const CRM_CHART_COLORS = [
  CRM_CHART_PALETTES.funnel.customer[0],
  CRM_CHART_PALETTES.funnel.business[0],
  CRM_CHART_PALETTES.funnel.win[0],
];

const BAR_RADIUS_TOP: [number, number, number, number] = [2, 2, 0, 0];
const BAR_RADIUS_RIGHT: [number, number, number, number] = [0, 3, 3, 0];

function linearGradient(
  x: number,
  y: number,
  x2: number,
  y2: number,
  stops: GradientPair,
) {
  return {
    type: 'linear' as const,
    x,
    y,
    x2,
    y2,
    colorStops: [
      { offset: 0, color: stops[0] },
      { offset: 1, color: stops[1] },
    ],
  };
}

function getThemeContext(isDark = false) {
  const theme = getCrmChartTheme(isDark);
  return { theme, ...createChartStyles(theme) };
}

interface FunnelStage {
  name: string;
  value: number;
  shapeValue: number;
  colors: GradientPair;
  rate: number | null;
  totalRate: number | null;
}

/** 固定漏斗轮廓 + 真实数量 */
function buildFunnelStages(data: FunnelSummary): FunnelStage[] {
  const p = CRM_CHART_PALETTES.funnel;
  const counts = [
    { name: '提交审核', value: data.submitted || 0, colors: p.customer },
    { name: 'AI 审核完成', value: data.aiReviewed || 0, colors: p.business },
    { name: '已归档', value: data.archived || 0, colors: p.win },
  ];
  const shapeValues = [100, 68, 38];
  return counts.map((item, index) => {
    const prev = index > 0 ? counts[index - 1]!.value : 0;
    const first = counts[0]!.value;
    return {
      ...item,
      shapeValue: shapeValues[index]!,
      rate:
        index === 0 || prev <= 0
          ? null
          : Math.round((item.value / prev) * 1000) / 10,
      totalRate:
        index === 0 || first <= 0
          ? null
          : Math.round((item.value / first) * 1000) / 10,
    };
  });
}

function funnelSeriesData(stages: FunnelStage[]) {
  return stages.map((stage) => ({
    value: stage.shapeValue,
    name: stage.name,
    count: stage.value,
    rate: stage.rate,
    totalRate: stage.totalRate,
  }));
}

/** 销售漏斗 — 双层轮廓 + 渐变层 + 右侧转化率（社区转化漏斗方案） */
export function getFunnelChartOption(
  data: FunnelSummary,
  isDark = false,
): ChartOption {
  const theme = getCrmChartTheme(isDark);
  const { baseTooltip } = createChartStyles(theme);
  const stages = buildFunnelStages(data);
  const seriesData = funnelSeriesData(stages);
  const funnelLayout = {
    left: '20%',
    width: '46%',
    top: 24,
    bottom: 24,
    min: 0,
    max: 100,
    minSize: '34%',
    maxSize: '100%',
    sort: 'none' as const,
    gap: 10,
  };

  return {
    textStyle: { color: theme.text },
    tooltip: {
      ...baseTooltip,
      trigger: 'item',
      formatter: (params: any) => {
        const d = params.data;
        let html = `${d.name}<br/>数量：${d.count}`;
        if (d.rate != null) {
          html += `<br/>阶段转化：${d.rate}%`;
          html += `<br/>总体转化：${d.totalRate}%`;
        }
        return html;
      },
    },
    series: [
      {
        name: 'outline',
        type: 'funnel',
        ...funnelLayout,
        label: { show: false },
        itemStyle: { color: theme.funnelOutline, borderWidth: 0 },
        emphasis: { disabled: true },
        data: seriesData,
        z: 1,
      },
      {
        name: '合同审核漏斗',
        type: 'funnel',
        left: '23%',
        width: '40%',
        top: 28,
        bottom: 28,
        min: 0,
        max: 100,
        minSize: '34%',
        maxSize: '100%',
        sort: 'none',
        gap: 12,
        label: {
          show: true,
          position: 'inside',
          formatter: (params: any) => `{num|${params.data.count}}`,
          rich: {
            num: {
              color: '#fff',
              fontSize: 15,
              fontWeight: 600,
              textShadowColor: 'rgba(0,0,0,0.12)',
              textShadowBlur: 4,
            },
          },
        },
        itemStyle: {
          borderColor: theme.border,
          borderWidth: 2,
        },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.1)' },
        },
        data: seriesData.map((item, index) => ({
          ...item,
          itemStyle: {
            color: linearGradient(0, 0, 0, 1, stages[index]!.colors),
          },
        })),
        z: 2,
      },
      {
        name: 'label',
        type: 'funnel',
        ...funnelLayout,
        label: {
          show: true,
          position: 'right',
          align: 'left',
          distance: 28,
          formatter: (params: any) => {
            const d = params.data;
            if (d.rate == null) {
              return `{title|${d.name}}\n{sub|${d.count} 个}`;
            }
            return `{title|${d.name}}\n{sub|${d.count} 个}   {rate|↓ ${d.rate}%}`;
          },
          rich: {
            title: {
              color: theme.text,
              fontSize: 13,
              fontWeight: 600,
              lineHeight: 20,
            },
            sub: {
              color: theme.textMuted,
              fontSize: 12,
              lineHeight: 18,
            },
            rate: {
              color: theme.accent,
              fontSize: 11,
              lineHeight: 18,
              backgroundColor: theme.rateBg,
              padding: [2, 6, 2, 6],
              borderRadius: 2,
            },
          },
        },
        labelLine: {
          show: true,
          length: 16,
          lineStyle: { color: theme.labelLine, type: 'dashed' as const },
        },
        itemStyle: { color: 'transparent', borderWidth: 0 },
        emphasis: { disabled: true },
        data: seriesData,
        z: 3,
      },
    ],
  };
}

/** BPM 流程趋势 — 发起 / 办结 */
export function getBpmTrendChartOption(
  list: BpmTrendByDate[],
  isDark = false,
): ChartOption {
  const { theme, axisStyle, splitLine, baseTooltip, legendStyle, baseGrid } =
    getThemeContext(isDark);
  const p = CRM_CHART_PALETTES.businessTrend;
  const labels = list.map((i) => i.time);
  return {
    textStyle: { color: theme.text },
    color: [p.bar[0], p.line[0]],
    grid: baseGrid,
    legend: legendStyle,
    tooltip: { ...baseTooltip, trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: labels,
      ...axisStyle,
    },
    yAxis: [
      {
        type: 'value',
        name: '发起数',
        nameTextStyle: { color: theme.textMuted, fontSize: 11 },
        minInterval: 1,
        ...axisStyle,
        ...splitLine,
      },
      {
        type: 'value',
        name: '办结数',
        nameTextStyle: { color: theme.textMuted, fontSize: 11 },
        ...axisStyle,
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '流程发起',
        type: 'bar',
        barMaxWidth: 20,
        itemStyle: {
          borderRadius: BAR_RADIUS_TOP,
          color: linearGradient(0, 0, 0, 1, p.bar),
        },
        data: list.map((i) => i.startedCount),
      },
      {
        name: '流程办结',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, color: p.line[0] },
        itemStyle: {
          color: p.line[0],
          borderColor: theme.border,
          borderWidth: 1,
        },
        areaStyle: {
          color: linearGradient(0, 0, 0, 1, [
            p.lineArea[0],
            p.lineArea[1],
          ] as GradientPair),
        },
        data: list.map((i) => i.finishedCount),
      },
    ],
  };
}

/** AI 使用趋势 — 对话 / Token */
export function getAiTrendChartOption(
  list: AiTrendByDate[],
  isDark = false,
): ChartOption {
  const { theme, axisStyle, splitLine, baseTooltip, legendStyle, baseGrid } =
    getThemeContext(isDark);
  const p = CRM_CHART_PALETTES.customerTrend;
  return {
    textStyle: { color: theme.text },
    color: [p.create[0], p.deal[0]],
    grid: baseGrid,
    legend: legendStyle,
    tooltip: { ...baseTooltip, trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((i) => i.time),
      ...axisStyle,
    },
    yAxis: [
      {
        type: 'value',
        name: '对话次数',
        nameTextStyle: { color: theme.textMuted, fontSize: 11 },
        minInterval: 1,
        ...axisStyle,
        ...splitLine,
      },
      {
        type: 'value',
        name: 'Token(千)',
        nameTextStyle: { color: theme.textMuted, fontSize: 11 },
        ...axisStyle,
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: 'AI 对话',
        type: 'bar',
        barGap: '24%',
        barMaxWidth: 16,
        itemStyle: {
          borderRadius: BAR_RADIUS_TOP,
          color: linearGradient(0, 0, 0, 1, p.create),
        },
        data: list.map((i) => i.chatCount),
      },
      {
        name: 'Token 消耗',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { width: 2, color: p.deal[0] },
        itemStyle: { color: p.deal[0] },
        data: list.map((i) => i.tokenK),
      },
    ],
  };
}

/** 合同趋势 — 新增 / 归档 */
export function getContractTrendChartOption(
  list: ContractTrendByDate[],
  isDark = false,
): ChartOption {
  const { theme, axisStyle, splitLine, baseTooltip, legendStyle, baseGrid } =
    getThemeContext(isDark);
  const p = CRM_CHART_PALETTES.customerTrend;
  return {
    textStyle: { color: theme.text },
    color: [p.create[0], p.deal[0]],
    grid: baseGrid,
    legend: legendStyle,
    tooltip: { ...baseTooltip, trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: list.map((i) => i.time),
      ...axisStyle,
    },
    yAxis: {
      type: 'value',
      name: '份数',
      nameTextStyle: { color: theme.textMuted, fontSize: 11 },
      minInterval: 1,
      ...axisStyle,
      ...splitLine,
    },
    series: [
      {
        name: '新增合同',
        type: 'bar',
        barGap: '24%',
        barMaxWidth: 16,
        itemStyle: {
          borderRadius: BAR_RADIUS_TOP,
          color: linearGradient(0, 0, 0, 1, p.create),
        },
        data: list.map((i) => i.createdCount),
      },
      {
        name: '完成归档',
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: {
          borderRadius: BAR_RADIUS_TOP,
          color: linearGradient(0, 0, 0, 1, p.deal),
        },
        data: list.map((i) => i.archivedCount),
      },
    ],
  };
}

/** @deprecated 使用 getBpmTrendChartOption */
export function getBusinessTrendChartOption(list: BpmTrendByDate[]): ChartOption {
  return getBpmTrendChartOption(list);
}

/** @deprecated 使用 getAiTrendChartOption */
export function getCustomerTrendChartOption(list: AiTrendByDate[]): ChartOption {
  return getAiTrendChartOption(list);
}

/** 业绩排行 — 每行不同浅色 */
export function getRankBarChartOption(
  list: RankItem[],
  isDark = false,
): ChartOption {
  const { theme, axisStyle, splitLine, baseTooltip, baseGrid } =
    getThemeContext(isDark);
  const palettes = CRM_CHART_PALETTES.rank;
  const sorted = [...list].sort((a, b) => a.count - b.count).slice(-10);
  const names = sorted.map((i) => i.nickname);
  const values = sorted.map((i) => Number(i.count || 0));
  return {
    textStyle: { color: theme.text },
    grid: { ...baseGrid, left: 88 },
    tooltip: {
      ...baseTooltip,
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)',
        },
      },
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        return `${item.name}<br/>${Number(item.value)} 次`;
      },
    },
    xAxis: {
      type: 'value',
      name: '处理量',
      nameTextStyle: { color: theme.textMuted, fontSize: 11 },
      ...axisStyle,
      ...splitLine,
    },
    yAxis: {
      type: 'category',
      data: names,
      ...axisStyle,
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 14,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}',
          fontSize: 11,
          color: theme.textMuted,
        },
        data: values.map((value, index) => {
          const pair = palettes[index % palettes.length]!;
          return {
            value,
            itemStyle: {
              borderRadius: BAR_RADIUS_RIGHT,
              color: linearGradient(0, 0, 1, 0, pair),
            },
          };
        }),
      },
    ],
  };
}

/** 工作台 — 环形图 + 中心转化率 + 右侧图例（社区仪表盘常用） */
export function getWorkbenchSalesChartOption(
  data: FunnelSummary,
  isDark = false,
): ChartOption {
  const { theme, baseTooltip } = getThemeContext(isDark);
  const stages = buildFunnelStages(data);
  const p = CRM_CHART_PALETTES.funnel;
  const pairs = [p.customer, p.business, p.win];
  const customer = stages[0]?.value ?? 0;
  const win = stages[2]?.value ?? 0;
  const winRate =
    customer > 0 ? Math.round((win / customer) * 1000) / 10 : 0;
  const total = stages.reduce((sum, item) => sum + item.value, 0) || 1;

  const segments = stages.map((stage, index) => ({
    name: stage.name,
    value: stage.value,
    rate: stage.rate,
    itemStyle: {
      color: linearGradient(0, 0, 1, 1, pairs[index]!),
    },
  }));

  return {
    textStyle: { color: theme.text },
    tooltip: {
      ...baseTooltip,
      trigger: 'item',
      formatter: (params: any) => {
        const d = params.data;
        const pct = Math.round((params.value / total) * 1000) / 10;
        let html = `${d.name}<br/>数量：${d.value}（${pct}%）`;
        if (d.rate != null) html += `<br/>阶段转化：${d.rate}%`;
        return html;
      },
    },
    legend: {
      orient: 'vertical',
      right: '4%',
      top: 'middle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 14,
      formatter: (name: string) => {
        const item = segments.find((s) => s.name === name);
        if (!item) return name;
        const pct = Math.round((item.value / total) * 1000) / 10;
        const ratePart = item.rate == null ? '' : `  {rate|↓${item.rate}%}`;
        return `{name|${name}}  {val|${item.value}}${ratePart}\n{sub|占比 ${pct}%}`;
      },
      textStyle: {
        rich: {
          name: {
            color: theme.text,
            fontSize: 13,
            fontWeight: 600,
            width: 36,
          },
          val: {
            color: theme.text,
            fontSize: 14,
            fontWeight: 600,
            padding: [0, 0, 0, 4],
          },
          rate: {
            color: theme.accent,
            fontSize: 11,
            padding: [0, 0, 0, 6],
          },
          sub: {
            color: theme.textMuted,
            fontSize: 11,
            lineHeight: 18,
            padding: [2, 0, 0, 40],
          },
        },
      },
    },
    graphic: [
      {
        type: 'text',
        left: '29%',
        top: '42%',
        style: {
          text: `${winRate}%`,
          fill: theme.accent,
          font: '600 26px sans-serif',
          textAlign: 'center',
        },
      },
      {
        type: 'text',
        left: '29%',
        top: '52%',
        style: {
          text: '总转化率',
          fill: theme.textMuted,
          font: '12px sans-serif',
          textAlign: 'center',
        },
      },
    ],
    series: [
      {
        type: 'pie',
        radius: ['56%', '72%'],
        center: ['32%', '50%'],
        silent: true,
        label: { show: false },
        data: [{ value: 1, name: '' }],
        itemStyle: { color: theme.funnelOutline },
        z: 1,
      },
      {
        type: 'pie',
        radius: ['56%', '72%'],
        center: ['32%', '50%'],
        padAngle: 3,
        minAngle: 8,
        label: { show: false },
        itemStyle: {
          borderRadius: 4,
          borderColor: theme.border,
          borderWidth: 2,
        },
        emphasis: {
          scale: true,
          scaleSize: 6,
          itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.08)' },
        },
        data: segments,
        z: 2,
      },
    ],
  };
}

/** 工作台 — 横向转化条（窄栏布局，无环形图留白） */
export function getWorkbenchConversionBarOption(
  data: FunnelSummary,
  isDark = false,
): ChartOption {
  const { theme, baseTooltip } = getThemeContext(isDark);
  const stages = buildFunnelStages(data);
  const p = CRM_CHART_PALETTES.funnel;
  const pairs = [p.customer, p.business, p.win];
  const max = Math.max(stages[0]?.value ?? 0, 1);

  return {
    textStyle: { color: theme.text },
    grid: { left: 4, right: 68, top: 4, bottom: 4, containLabel: true },
    tooltip: {
      ...baseTooltip,
      trigger: 'item',
      formatter: (params: any) => {
        const stage = stages[params.dataIndex];
        if (!stage) return '';
        let html = `${stage.name}<br/>数量：${stage.value}`;
        if (stage.rate != null) html += `<br/>阶段转化：${stage.rate}%`;
        return html;
      },
    },
    xAxis: { type: 'value', max, show: false },
    yAxis: {
      type: 'category',
      data: stages.map((s) => s.name),
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: theme.text,
        fontSize: 12,
        margin: 10,
      },
    },
    series: [
      {
        type: 'bar',
        barWidth: 26,
        showBackground: true,
        backgroundStyle: { color: theme.funnelOutline, borderRadius: 2 },
        data: stages.map((stage, index) => ({
          value: stage.value,
          itemStyle: {
            borderRadius: 2,
            color: linearGradient(0, 0, 1, 0, pairs[index]!),
          },
          label: {
            show: true,
            position: 'right',
            distance: 6,
            formatter:
              stage.rate == null
                ? `{v|${stage.value}}`
                : `{v|${stage.value}}  {r|↓${stage.rate}%}`,
            rich: {
              v: {
                color: theme.text,
                fontSize: 12,
                fontWeight: 600,
              },
              r: { color: theme.accent, fontSize: 11 },
            },
          },
        })),
      },
    ],
  };
}

/** 工作台 — 近7天客户新增趋势（全宽） */
export function getWorkbenchWeekTrendChartOption(
  list: CustomerSummaryByDate[],
  isDark = false,
): ChartOption {
  const { theme, axisStyle, splitLine, baseTooltip } = getThemeContext(isDark);
  const p = CRM_CHART_PALETTES.customerTrend.create;
  return {
    textStyle: { color: theme.text },
    grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
    tooltip: {
      ...baseTooltip,
      trigger: 'axis',
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params;
        return `${item.name}<br/>新增用户：${item.value}`;
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: list.map((i) => i.time),
      ...axisStyle,
      axisLabel: { color: theme.textMuted, fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      ...axisStyle,
      ...splitLine,
      axisLabel: { color: theme.textMuted, fontSize: 10 },
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, color: p[0] },
        itemStyle: { color: p[0] },
        areaStyle: {
          color: linearGradient(0, 0, 0, 1, [
            'rgba(120,212,176,0.38)',
            'rgba(210,242,230,0.04)',
          ]),
        },
        data: list.map((i) => i.customerCreateCount),
      },
    ],
  };
}

export function getWorkbenchFunnelMeta(data: FunnelSummary) {
  const stages = buildFunnelStages(data);
  const customer = stages[0]?.value ?? 0;
  const win = stages[2]?.value ?? 0;
  return {
    stages: stages.map((stage) => ({
      name: stage.name,
      value: stage.value,
      rate: stage.rate,
      color: stage.colors[0],
      bg: `${stage.colors[1]}55`,
    })),
    winRate:
      customer > 0 ? Math.round((win / customer) * 1000) / 10 : null,
  };
}

/** @deprecated 使用 getWorkbenchSalesChartOption */
export function getMiniFunnelChartOption(
  data: FunnelSummary,
): ChartOption {
  return getWorkbenchSalesChartOption(data);
}
