import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace LegalSkillPackApi {
  export interface SkillPack {
    id?: number;
    code: string;
    name: string;
    scene: string;
    chatRoleId?: number;
    toolNames?: string[];
    mcpClientNames?: string[];
    modelPolicy?: string;
    playbookId?: number;
    description?: string;
    enabled?: boolean;
    version?: number;
    createTime?: string;
  }

  export interface SkillPackSimple {
    id: number;
    code: string;
    name: string;
    scene: string;
  }

  export interface LegalAgentTool {
    name: string;
    description?: string;
    registered?: boolean;
  }
}

export function getSkillPackPage(params: PageParam) {
  return requestClient.get<PageResult<LegalSkillPackApi.SkillPack>>(
    '/legal/skill-pack/page',
    { params },
  );
}

export function getSkillPack(id: number) {
  return requestClient.get<LegalSkillPackApi.SkillPack>(
    `/legal/skill-pack/get?id=${id}`,
  );
}

export function createSkillPack(data: LegalSkillPackApi.SkillPack) {
  return requestClient.post<number>('/legal/skill-pack/create', data);
}

export function updateSkillPack(data: LegalSkillPackApi.SkillPack) {
  return requestClient.put('/legal/skill-pack/update', data);
}

export function updateSkillPackEnabled(id: number, enabled: boolean) {
  return requestClient.put(
    `/legal/skill-pack/update-enabled?id=${id}&enabled=${enabled}`,
  );
}

export function copySkillPack(id: number) {
  return requestClient.post<number>(`/legal/skill-pack/copy?id=${id}`);
}

export function deleteSkillPack(id: number) {
  return requestClient.delete(`/legal/skill-pack/delete?id=${id}`);
}

export function deleteSkillPackList(ids: number[]) {
  return requestClient.delete(
    `/legal/skill-pack/delete-list?ids=${ids.join(',')}`,
  );
}

export function getLegalAgentTools() {
  return requestClient.get<LegalSkillPackApi.LegalAgentTool[]>(
    '/legal/skill-pack/legal-agent-tools',
  );
}

export function getSkillPackSimpleList(scene?: string) {
  return requestClient.get<LegalSkillPackApi.SkillPackSimple[]>(
    '/legal/skill-pack/simple-list',
    { params: scene ? { scene } : {} },
  );
}
