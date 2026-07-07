import { isTenantEnable } from '@vben/hooks';
import { useAccessStore } from '@vben/stores';

/** SSE 请求统一租户头（fetchEventSource 不走 axios 拦截器） */
export function buildAdminApiSseHeaders(token: string): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
  if (!isTenantEnable()) {
    return headers;
  }
  const accessStore = useAccessStore();
  headers['tenant-id'] = String(accessStore.tenantId ?? 1);
  if (accessStore.visitTenantId) {
    headers['visit-tenant-id'] = String(accessStore.visitTenantId);
  }
  return headers;
}
