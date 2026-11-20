import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// /api/* goes to the FlagMan API, the same as nginx.conf does in the container.
export default defineConfig({
  plugins: [react()],
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
})
