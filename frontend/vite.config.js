import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    // '@/api/tasks' instead of '../../api/tasks' — imports don't break when files move.
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    // Listen on the local network too, so a phone on the same Wi-Fi can open http://<your-Mac-IP>:5173.
    host: true,
    // The browser only talks to the Vite dev server (same origin), which forwards /api to Spring Boot.
    // That's why the backend needs no CORS config in dev.
    // API_TARGET lets you point the dev server at another backend, e.g. API_TARGET=http://localhost:8081.
    proxy: { '/api': process.env.API_TARGET ?? 'http://localhost:8080' },
  },
})
