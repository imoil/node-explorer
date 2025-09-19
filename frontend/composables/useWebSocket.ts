import { ref, onMounted, onUnmounted } from 'vue';

const WEBSOCKET_URL = 'ws://localhost:8080/ws';

export function useWebSocket(onBatchUpdate) {
  const wsStatus = ref('Connecting...');
  let ws = null;
  let reconnectInterval = null;
  let isUnmounted = false;

  const connect = () => {
    if (isUnmounted) return;

    ws = new WebSocket(WEBSOCKET_URL);

    ws.onopen = () => {
      wsStatus.value = 'Connected';
      console.log('WebSocket connection established.');
      if (reconnectInterval) {
        clearInterval(reconnectInterval);
        reconnectInterval = null;
      }
    };

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (Array.isArray(payload)) {
            onBatchUpdate(payload);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    ws.onclose = () => {
      if (isUnmounted) return;
      wsStatus.value = 'Disconnected';
      console.log('WebSocket connection closed. Attempting to reconnect...');
      if (!reconnectInterval) {
        reconnectInterval = setInterval(connect, 5000);
      }
    };

    ws.onerror = (error) => {
      wsStatus.value = 'Error';
      console.error('WebSocket error:', error);
      ws.close();
    };
  }

  onMounted(connect);

  onUnmounted(() => {
    isUnmounted = true;
    if (reconnectInterval) {
      clearInterval(reconnectInterval);
    }
    if (ws) {
      ws.onclose = null; // Prevent reconnect logic on manual close
      ws.close();
    }
  });

  return {
    wsStatus,
  };
}

