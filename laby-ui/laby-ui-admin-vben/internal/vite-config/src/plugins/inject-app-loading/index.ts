import type { PluginOption } from 'vite';

import fs from 'node:fs';
import fsp from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { readPackageJSON } from '@vben/node-utils';

/**
 * 用于生成将loading样式注入到项目中
 * 为多app提供loading样式，无需在每个 app -> index.html单独引入
 */
async function viteInjectAppLoadingPlugin(
  isBuild: boolean,
  env: Record<string, any> = {},
  loadingTemplate = 'loading.html',
): Promise<PluginOption | undefined> {
  let viteRoot = process.cwd();

  return {
    enforce: 'pre',
    name: 'vite:inject-app-loading',
    configResolved(config) {
      viteRoot = config.root;
    },
    async transformIndexHtml(html) {
      const loadingHtml = await getLoadingRawByHtmlTemplate(
        loadingTemplate,
        viteRoot,
      );

      if (!loadingHtml) {
        return html;
      }

      const { version } = await readPackageJSON(viteRoot);
      const envRaw = isBuild ? 'prod' : 'dev';
      const cacheName = `'${env.VITE_APP_NAMESPACE}-${version}-${envRaw}-preferences-theme'`;

      const injectScript = `
  <script data-app-loading="inject-js">
  var theme = localStorage.getItem(${cacheName});
  document.documentElement.classList.toggle('dark', /dark/.test(theme));
</script>
`;

      const re = /<body\s*>/;
      return html.replace(re, `<body>${injectScript}${loadingHtml}`);
    },
  };
}

/**
 * 用于获取loading的html模板
 */
async function getLoadingRawByHtmlTemplate(
  loadingTemplate: string,
  root: string,
) {
  const searchRoots = [root, process.cwd()];

  for (const searchRoot of searchRoots) {
    const appLoadingPath = join(searchRoot, loadingTemplate);
    if (fs.existsSync(appLoadingPath)) {
      return await fsp.readFile(appLoadingPath, 'utf8');
    }
  }

  const __dirname = fileURLToPath(new URL('.', import.meta.url));
  return await fsp.readFile(join(__dirname, './default-loading.html'), 'utf8');
}

export { viteInjectAppLoadingPlugin };
