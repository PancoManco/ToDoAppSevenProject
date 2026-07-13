import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true, // не прыгать на другой порт — 5173 прописан в CORS бэкенда
  },
});
