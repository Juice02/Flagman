import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv, type Plugin } from 'vite'

// Production builds get a Content-Security-Policy: scripts only from this site, and network calls only to
// this site and the API. The admin key lives in this page, so injected scripts must not run. Dev keeps no
// policy, because Vite's dev server relies on inline scripts.
function contentSecurityPolicy(apiUrl: string | undefined): Plugin {
  const api = apiUrl ? new URL(apiUrl).origin : ''
  const policy = [
    "default-src 'self'", "script-src 'self'", "style-src 'self' https://fonts.googleapis.com",
    'font-src https://fonts.gstatic.com', "img-src 'self' data:", `connect-src 'self' ${api}`.trim(),
    "base-uri 'self'", "form-action 'self'", "object-src 'none'",
  ].join('; ')
  return {
    name: 'content-security-policy',
    apply: 'build',
    transformIndexHtml: () => [{ tag: 'meta', attrs: { 'http-equiv': 'Content-Security-Policy', content: policy }, injectTo: 'head-prepend' }],
  }
}

// /api/* goes to the FlagMan API, the same as nginx.conf does in the container.
export default defineConfig(({ mode }) => ({
  plugins: [react(), contentSecurityPolicy(loadEnv(mode, process.cwd(), 'VITE_').VITE_FLAGMAN_API_URL)],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.FLAGMAN_API_URL ?? 'http://localhost:8081',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
}))
