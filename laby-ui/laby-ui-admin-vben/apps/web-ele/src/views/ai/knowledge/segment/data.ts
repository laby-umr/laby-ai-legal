import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { AiKnowledgeSegmentApi } from '#/api/ai/knowledge/segment';

import { CommonStatusEnum, DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

/** 新增/修改的表单 */
export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'id',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      component: 'Input',
      fieldName: 'documentId',
      dependencies: {
        triggerFields: [''],
        show: () => false,
      },
    },
    {
      fieldName: 'content',
      label: '切片内容',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入切片内容',
        rows: 6,
        showCount: true,
      },
      rules: 'required',
    },
  ];
}
/** 列表的搜索表单 */
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'documentId',
      label: '文档编号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入文档编号',
        allowClear: true,
      },
    },
    {
      fieldName: 'status',
      label: '是否启用',
      component: 'Select',
      componentProps: {
        placeholder: '请选择是否启用',
        allowClear: true,
        options: getDictOptions(DICT_TYPE.COMMON_STATUS, 'number'),
      },
    },
  ];
}

/** 列表的字段 */
export function useGridColumns(
  onStatusChange?: (
    newStatus: number,
    row: AiKnowledgeSegmentApi.KnowledgeSegment,
  ) => PromiseLike<boolean | undefined>,
): VxeTableGridOptions['columns'] {
  return [
    {
      field: 'id',
      title: '分段编号',
      minWidth: 100,
    },
    {
      type: 'expand',
      width: 40,
      slots: { content: 'expand_content' },
    },
    {
      field: 'blockType',
      title: '块类型',
      minWidth: 110,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.AI_KNOWLEDGE_SEGMENT_BLOCK_TYPE },
      },
    },
    {
      field: 'chunkLevel',
      title: '层级',
      minWidth: 90,
      cellRender: {
        name: 'CellDict',
        props: { type: DICT_TYPE.AI_KNOWLEDGE_SEGMENT_CHUNK_LEVEL },
      },
    },
    {
      field: 'headingPath',
      title: '章节路径',
      minWidth: 180,
      formatter: ({ cellValue }) => cellValue || '-',
    },
    {
      field: 'pageStart',
      title: '页码',
      minWidth: 80,
      formatter: ({ row }) => {
        if (row.pageStart == null && row.pageEnd == null) {
          return '-';
        }
        if (row.pageEnd != null && row.pageEnd !== row.pageStart) {
          return `${row.pageStart ?? ''}-${row.pageEnd ?? ''}`;
        }
        return row.pageStart ?? row.pageEnd ?? '-';
      },
    },
    {
      field: 'content',
      title: '切片内容',
      minWidth: 200,
    },
    {
      field: 'sparseText',
      title: 'Sparse 文本',
      minWidth: 160,
      formatter: ({ cellValue }) => {
        if (!cellValue) {
          return '-';
        }
        const text = String(cellValue);
        return text.length > 40 ? `${text.slice(0, 40)}…` : text;
      },
    },
    {
      field: 'contentLength',
      title: '字符数',
      minWidth: 100,
    },
    {
      field: 'tokens',
      title: 'token 数量',
      minWidth: 120,
    },
    {
      field: 'retrievalCount',
      title: '召回次数',
      minWidth: 100,
    },
    {
      field: 'status',
      title: '状态',
      minWidth: 100,
      align: 'center',
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: 'CellSwitch',
        props: {
          activeValue: CommonStatusEnum.ENABLE,
          inactiveValue: CommonStatusEnum.DISABLE,
        },
      },
    },
    {
      field: 'createTime',
      title: '创建时间',
      minWidth: 180,
      formatter: 'formatDateTime',
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'actions' },
    },
  ];
}
