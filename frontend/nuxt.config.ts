export default defineNuxtConfig({
  compatibilityDate: '2025-09-18',
  devtools: { enabled: true },

  // API 서버가 분리되었으므로 Nuxt의 서버 디렉토리를 사용하지 않음
  serverDir: undefined,

  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
    }
  },

  css: ['vuetify/lib/styles/main.sass'],
  
  build: {
    transpile: ['vuetify'],
  },

  vite: {
    define: {
      'process.env.DEBUG': false,
    },
  },
})