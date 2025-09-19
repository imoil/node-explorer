export default defineNuxtConfig({
  compatibilityDate: '2025-09-18',
  devtools: { enabled: true },

  // API 서버가 분리되었으므로 Nuxt의 서버 디렉토리를 사용하지 않음
  serverDir: undefined,

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