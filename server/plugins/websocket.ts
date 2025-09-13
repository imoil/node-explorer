/**
 * @description Nuxt.js 서버에 WebSocket 기능을 추가하는 플러그인입니다.
 * 실시간 업데이트를 배치(batch) 처리하여 클라이언트에 전송합니다.
 */
import { WebSocketServer, WebSocket } from 'ws';
import type { IncomingMessage } from 'http';
import type { Duplex } from 'stream';

// Nuxt DevTools에서 HMR(Hot Module Replacement)을 위한 WebSocket과 충돌 방지
// see: https://github.com/nuxt/nuxt/issues/22329
const VITE_WEBSOCKET_PATH = '/_nuxt';

// 전역 WebSocket 서버 인스턴스 타입 정의
declare global {
  var wss: WebSocketServer | undefined;
}

export default defineNitroPlugin((nitroApp) => {
  // 개발 모드에서만 WebSocket 서버 실행
  if (!process.dev) {
    return;
  }

  // 이미 실행 중인 서버가 있으면 재사용
  if (global.wss) {
    console.log('Reusing existing WebSocket server.');
    return;
  }
  
  // Nitro 서버(Nuxt의 서버 엔진) 가져오기
  const server = nitroApp.server.httpServer;
  if (!server) {
    console.error('WebSocket server could not be started: HTTP server is not available.');
    return;
  }

  console.log('Starting WebSocket server...');
  const wss = new WebSocketServer({ noServer: true });

  // 업데이트를 임시 저장할 큐(Queue)
  let updateQueue: any[] = [];

  // 5초마다 큐에 쌓인 업데이트들을 배치로 묶어 브로드캐스트
  const broadcastInterval = setInterval(() => {
    // 0개에서 5개 사이의 랜덤한 수의 가상 이벤트를 생성하여 큐에 추가
    const eventCount = Math.floor(Math.random() * 6);
    if (eventCount > 0) {
      console.log(`⚡️ Generating ${eventCount} new update events.`);
      for (let i = 0; i < eventCount; i++) {
        // Mock DB에 있는 실제 노드 ID 중 하나를 랜덤하게 선택
        const targetId = `node-1-1-${Math.floor(Math.random() * 3) + 1}`; // e.g., node-1-1-1
        updateQueue.push({
          id: targetId,
          newName: `UpdatedName-${Math.random().toString(36).substring(7)}`,
        });
      }
    }

    if (wss.clients.size > 0 && updateQueue.length > 0) {
      const batchMessage = {
        type: 'NODE_UPDATES_BATCH',
        payload: [...updateQueue],
      };
      
      console.log(`📢 Broadcasting batch of ${updateQueue.length} updates to ${wss.clients.size} clients.`);
      
      wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(JSON.stringify(batchMessage));
        }
      });
      updateQueue = []; // 큐 비우기
    }
  }, 5000);

  // 서버의 'upgrade' 이벤트를 가로채서 WebSocket 연결 처리
  server.on('upgrade', (req: IncomingMessage, socket: Duplex, head: Buffer) => {
    // Nuxt/Vite의 HMR 요청은 무시
    if (req.url?.startsWith(VITE_WEBSOCKET_PATH)) {
      return;
    }
    
    wss.handleUpgrade(req, socket, head, (ws) => {
      wss.emit('connection', ws, req);
    });
  });

  wss.on('connection', (ws: WebSocket) => {
    console.log(`✅ Client connected. Total clients: ${wss.clients.size}`);
    ws.on('close', () => {
      console.log(`❌ Client disconnected. Total clients: ${wss.clients.size}`);
    });
  });

  // Nitro 앱 종료 시 WebSocket 서버와 interval 정리
  nitroApp.hooks.hook('close', () => {
    console.log('Closing WebSocket server...');
    clearInterval(broadcastInterval);
    wss.close();
    global.wss = undefined;
  });

  global.wss = wss;
});

