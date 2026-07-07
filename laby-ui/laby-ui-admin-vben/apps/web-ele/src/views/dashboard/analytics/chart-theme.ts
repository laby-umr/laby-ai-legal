export interface CrmChartTheme {
  text: string;
  textMuted: string;
  axis: string;
  split: string;
  border: string;
  accent: string;
  rateBg: string;
  funnelOutline: string;
  labelLine: string;
  tooltipBg: string;
  tooltipBorder: string;
  tooltipShadow: string;
  card: {
    customer: { color: string; bg: string };
    deal: { color: string; bg: string };
    contract: { color: string; bg: string };
    receivable: { color: string; bg: string };
  };
}

const LIGHT_THEME: CrmChartTheme = {
  text: '#5c5348',
  textMuted: '#9a9086',
  axis: '#ebe4dc',
  split: '#f5f0ea',
  border: '#ffffff',
  accent: '#d97852',
  rateBg: '#fff6f0',
  funnelOutline: '#f5f0ea',
  labelLine: '#e8dfd6',
  tooltipBg: 'rgba(255,255,255,0.96)',
  tooltipBorder: '#ebe4dc',
  tooltipShadow: '0 2px 12px rgba(0,0,0,0.06)',
  card: {
    customer: { color: '#d97852', bg: '#fff6f0' },
    deal: { color: '#5bb898', bg: '#f0faf6' },
    contract: { color: '#9b7fd4', bg: '#f6f2fc' },
    receivable: { color: '#d4a060', bg: '#fff9f3' },
  },
};

const DARK_THEME: CrmChartTheme = {
  text: '#e5eaf3',
  textMuted: '#a3a6ad',
  axis: '#4c4d4f',
  split: '#363637',
  border: '#414243',
  accent: '#f0a878',
  rateBg: 'rgba(240,168,120,0.18)',
  funnelOutline: '#363637',
  labelLine: '#4c4d4f',
  tooltipBg: 'rgba(29,30,31,0.96)',
  tooltipBorder: '#4c4d4f',
  tooltipShadow: '0 2px 12px rgba(0,0,0,0.35)',
  card: {
    customer: { color: '#f0a878', bg: 'rgba(240,168,120,0.12)' },
    deal: { color: '#78d4b0', bg: 'rgba(120,212,176,0.12)' },
    contract: { color: '#b8a0e8', bg: 'rgba(184,160,232,0.12)' },
    receivable: { color: '#e8c878', bg: 'rgba(232,200,120,0.12)' },
  },
};

/** @deprecated 使用 getCrmChartTheme(isDark) */
export const CRM_CHART_THEME = LIGHT_THEME;

export function getCrmChartTheme(isDark = false): CrmChartTheme {
  return isDark ? DARK_THEME : LIGHT_THEME;
}

export function createChartStyles(theme: CrmChartTheme) {
  const axisStyle = {
    axisLine: { lineStyle: { color: theme.axis } },
    axisTick: { show: false },
    axisLabel: { color: theme.textMuted, fontSize: 11 },
  };

  const splitLine = {
    splitLine: {
      lineStyle: { color: theme.split, type: 'dashed' as const },
    },
  };

  const baseTooltip = {
    backgroundColor: theme.tooltipBg,
    borderColor: theme.tooltipBorder,
    borderWidth: 1,
    textStyle: { color: theme.text, fontSize: 12 },
    extraCssText: `box-shadow: ${theme.tooltipShadow}; border-radius: 4px;`,
  };

  const legendStyle = {
    top: 8,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: theme.textMuted, fontSize: 12 },
  };

  const baseGrid = {
    left: 28,
    right: 24,
    top: 52,
    bottom: 28,
    containLabel: true,
  };

  return { axisStyle, splitLine, baseTooltip, legendStyle, baseGrid };
}
