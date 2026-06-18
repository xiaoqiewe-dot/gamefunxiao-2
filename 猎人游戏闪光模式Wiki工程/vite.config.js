import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    chunkSizeWarningLimit: 1200
  },
  server: {
    host: '0.0.0.0',
    port: 4174
  },
  preview: {
    host: '0.0.0.0',
    port: 4175
  }
});
