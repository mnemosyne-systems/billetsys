import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: '/app/',
  build: {
    outDir: '../backend/main/resources/META-INF/resources/app',
    emptyOutDir: true,
    assetsDir: 'assets'
  }
});
