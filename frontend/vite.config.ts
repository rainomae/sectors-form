/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // The API is served from the same origin in development, so the session cookie works without CORS.
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    include: ['src/**/*.test.ts'],
  },
})
