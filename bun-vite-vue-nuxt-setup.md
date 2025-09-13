# Bun + Vite + Vue.js + Nuxt.js 프로젝트 구성 가이드

## 개요

이 가이드는 Bun을 패키지 매니저로 사용하고, Vite를 빌드 도구로, Vue.js를 프레임워크로, Nuxt.js를 메타 프레임워크로 사용하는 현대적인 웹 개발 환경을 구성하는 방법을 다룹니다.

## 사전 요구사항

- Node.js 18+ 또는 Bun 설치
- 기본적인 Vue.js 및 Nuxt.js 지식

## 1. Bun 설치

### macOS/Linux
```bash
curl -fsSL https://bun.sh/install | bash
```

### Windows
```powershell
powershell -c "irm bun.sh/install.ps1 | iex"
```

설치 확인:
```bash
bun --version
```

## 2. Nuxt.js 프로젝트 생성

```bash
# Nuxt 프로젝트 생성
bunx nuxi@latest init my-nuxt-app

# 프로젝트 디렉토리로 이동
cd my-nuxt-app

# Bun으로 의존성 설치
bun install
```

## 3. 프로젝트 구조

생성된 Nuxt 프로젝트는 기본적으로 다음과 같은 구조를 가집니다:

```
my-nuxt-app/
├── .nuxt/              # 빌드 출력 (자동 생성)
├── assets/             # 정적 자산 (CSS, 이미지 등)
├── components/         # Vue 컴포넌트
├── composables/        # Vue 컴포저블
├── layouts/            # 레이아웃 컴포넌트
├── middleware/         # 라우트 미들웨어
├── pages/              # 페이지 컴포넌트 (자동 라우팅)
├── plugins/            # Nuxt 플러그인
├── public/             # 정적 파일
├── server/             # 서버 API 라우트
├── utils/              # 유틸리티 함수
├── app.vue             # 루트 Vue 컴포넌트
├── nuxt.config.ts      # Nuxt 설정 파일
├── package.json
└── bun.lockb           # Bun 락 파일
```

## 4. Nuxt.js 설정 (nuxt.config.ts)

Vite가 이미 Nuxt.js에 내장되어 있으므로 별도 설정이 필요하지 않습니다. 필요에 따라 커스터마이징할 수 있습니다:

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  devtools: { enabled: true },
  
  // Vite 설정 커스터마이징
  vite: {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "~/assets/_colors.scss" as *;'
        }
      }
    }
  },
  
  // CSS 프레임워크 (선택사항)
  css: ['~/assets/css/main.css'],
  
  // 모듈 추가
  modules: [
    '@nuxtjs/tailwindcss', // 예시
    '@pinia/nuxt'          // 상태 관리
  ],
  
  // TypeScript 설정
  typescript: {
    strict: true
  },
  
  // 런타임 설정
  runtimeConfig: {
    // 서버 전용 키
    apiSecret: '123',
    // 클라이언트에서 노출되는 키
    public: {
      apiBase: '/api'
    }
  }
})
```

## 5. 주요 의존성 추가

```bash
# UI 라이브러리
bun add @nuxtjs/tailwindcss

# 상태 관리
bun add @pinia/nuxt pinia

# HTTP 클라이언트
bun add @nuxt/http

# 개발 의존성
bun add -d @types/node
```

## 6. 기본 페이지 생성

### pages/index.vue
```vue
<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-4">
      Welcome to Nuxt 3
    </h1>
    <p class="text-lg mb-4">
      Built with Bun, Vite, Vue.js, and Nuxt.js
    </p>
    <NuxtLink to="/about" class="text-blue-500 hover:underline">
      Go to About page
    </NuxtLink>
  </div>
</template>

<script setup lang="ts">
// 페이지 메타데이터
useHead({
  title: 'Home - My Nuxt App',
  meta: [
    { name: 'description', content: 'Welcome to my Nuxt application' }
  ]
})
</script>
```

### pages/about.vue
```vue
<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-4">About Page</h1>
    <p class="text-lg mb-4">This is the about page.</p>
    <NuxtLink to="/" class="text-blue-500 hover:underline">
      Back to Home
    </NuxtLink>
  </div>
</template>

<script setup lang="ts">
useHead({
  title: 'About - My Nuxt App'
})
</script>
```

## 7. 컴포넌트 생성

### components/AppHeader.vue
```vue
<template>
  <header class="bg-gray-800 text-white p-4">
    <nav class="container mx-auto flex justify-between items-center">
      <NuxtLink to="/" class="text-xl font-bold">
        My App
      </NuxtLink>
      <ul class="flex space-x-4">
        <li>
          <NuxtLink to="/" class="hover:text-gray-300">Home</NuxtLink>
        </li>
        <li>
          <NuxtLink to="/about" class="hover:text-gray-300">About</NuxtLink>
        </li>
      </ul>
    </nav>
  </header>
</template>
```

### layouts/default.vue
```vue
<template>
  <div>
    <AppHeader />
    <main>
      <slot />
    </main>
    <footer class="bg-gray-100 p-4 text-center">
      <p>&copy; 2024 My Nuxt App. All rights reserved.</p>
    </footer>
  </div>
</template>
```

## 8. Composable 생성

### composables/useApi.ts
```typescript
export const useApi = () => {
  const config = useRuntimeConfig()
  
  const fetchData = async (endpoint: string) => {
    try {
      const { data } = await $fetch(`${config.public.apiBase}${endpoint}`)
      return data
    } catch (error) {
      throw createError({
        statusCode: 500,
        statusMessage: 'API fetch failed'
      })
    }
  }
  
  return {
    fetchData
  }
}
```

## 9. 스크립트 설정 (package.json)

```json
{
  "scripts": {
    "build": "nuxt build",
    "dev": "nuxt dev",
    "generate": "nuxt generate",
    "preview": "nuxt preview",
    "postinstall": "nuxt prepare"
  }
}
```

## 10. 개발 서버 실행

```bash
# 개발 서버 시작
bun run dev

# 또는
bun dev
```

브라우저에서 `http://localhost:3000`을 열어 확인할 수 있습니다.

## 11. 빌드 및 배포

```bash
# 프로덕션 빌드
bun run build

# 정적 사이트 생성 (SSG)
bun run generate

# 프로덕션 미리보기
bun run preview
```

## 12. 유용한 팁

### 환경 변수 설정
`.env` 파일을 생성하여 환경 변수를 관리합니다:

```env
# .env
NUXT_API_SECRET=your-secret-key
NUXT_PUBLIC_API_BASE=https://api.example.com
```

### TypeScript 지원
Nuxt 3는 기본적으로 TypeScript를 완전 지원합니다. `.ts` 확장자를 사용하거나 `<script setup lang="ts">`를 사용하세요.

### 자동 임포트
Nuxt는 컴포넌트, 컴포저블, 유틸리티를 자동으로 임포트합니다. 별도의 import 구문 없이 사용할 수 있습니다.

## 결론

이제 Bun, Vite, Vue.js, Nuxt.js를 사용한 현대적인 웹 개발 환경이 구성되었습니다. 이 설정을 통해 빠른 개발 경험과 우수한 성능을 얻을 수 있습니다.

## 추가 리소스

- [Nuxt.js 공식 문서](https://nuxt.com/)
- [Vue.js 공식 문서](https://vuejs.org/)
- [Vite 공식 문서](https://vitejs.dev/)
- [Bun 공식 문서](https://bun.sh/)