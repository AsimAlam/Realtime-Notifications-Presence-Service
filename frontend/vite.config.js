import { defineConfig } from 'vite'

export default defineConfig({
  define: {
    // replace occurrences of `global` in code with `window` at build-time
    global: 'window',
    'process.env': {}
  },
  optimizeDeps: {
    // include packages that may require pre-bundling so vite transforms them correctly
    include: ['sockjs-client', '@stomp/stompjs']
  },
  server: {
    port: 3000,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/notify': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true }
    }
  }
})
