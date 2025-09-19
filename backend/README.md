

# tree-api-backend 디렉토리로 이동
cd tree-api-backend

# Maven을 사용하여 애플리케이션 실행
./mvnw spring-boot:run

**2. 프론트엔드 서버 실행 (Nuxt.js)**

```bash
# 기존 Nuxt 프로젝트 디렉토리로 이동
cd vuejs-node-explorer

# 개발 서버 실행
bun install
bun add -D sass-embedded
bun run dev
```

이제 브라우저에서 `http://localhost:3000`으로 접속하면, 모든 API 요청이 `http://localhost:8080`에서 실행 중인 Spring Boot 서버로 전송되어 동작하는 것을 확인할 수 있습니다.