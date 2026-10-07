import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, Vite redirige las llamadas a los microservicios (igual que nginx en producción).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/analitica': {
        target: 'http://localhost:8000',
        rewrite: (p) => p.replace(/^\/analitica/, ''),
      },
    },
  },
})
