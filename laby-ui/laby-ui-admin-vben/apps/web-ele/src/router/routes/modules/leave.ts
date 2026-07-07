import type { RouteRecordRaw } from 'vue-router';

/** OA 请假隐藏路由（扁平完整 path） */
const routes: RouteRecordRaw[] = [
  {
    path: '/bpm/oa/leave',
    name: 'OALeaveIndex',
    component: () => import('#/views/bpm/oa/leave/index.vue'),
    meta: {
      title: '请假列表',
      activePath: '/bpm/oa/leave',
      hideInMenu: true,
    },
  },
  {
    path: '/bpm/oa/leave/create',
    name: 'OALeaveCreate',
    component: () => import('#/views/bpm/oa/leave/create.vue'),
    meta: {
      title: '创建请假',
      activePath: '/bpm/oa/leave',
      hideInMenu: true,
    },
  },
  {
    path: '/bpm/oa/leave/detail',
    name: 'OALeaveDetail',
    component: () => import('#/views/bpm/oa/leave/detail.vue'),
    meta: {
      title: '请假详情',
      activePath: '/bpm/oa/leave',
      hideInMenu: true,
    },
  },
];

export default routes;
