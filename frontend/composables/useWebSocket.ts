import { ref, onMounted, onUnmounted } from 'vue';
import { useRuntimeConfig } from '#app';

export function useWebSocket(onBatchUpdate) {
  const { public: { apiBaseUrl } } = useRuntimeConfig();
  const WEBSOCKET_URL = `${apiBaseUrl.replace(/^http/, 'ws')}/ws`;
  const wsStatus = ref('Connecting...');
  let ws = null;
  let reconnectInterval = null;
  let isUnmounted = false;
  let retryCount = 0;
  const maxRetries = 5;
  let reconnectDelay = 1000; // Start with 1 second

  const connect = () => {
    if (isUnmounted || retryCount >= maxRetries) {
      if (retryCount >= maxRetries) {
        wsStatus.value = 'Disconnected (Max retries reached)';
        console.error('WebSocket: Max reconnection retries reached.');
      }
      return;
    }

    ws = new WebSocket(WEBSOCKET_URL);

    ws.onopen = () => {
      wsStatus.value = 'Connected';
      console.log('WebSocket connection established.');
      retryCount = 0; // Reset retry counter on successful connection
      reconnectDelay = 1000; // Reset delay
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
      
      retryCount++;
      wsStatus.value = `Disconnected (Retrying ${retryCount}/${maxRetries}... in ${reconnectDelay / 1000}s)`;
      console.log(`WebSocket connection closed. Attempting to reconnect (attempt ${retryCount})...`);
      
      if (!reconnectInterval) {
        reconnectInterval = setTimeout(() => {
          reconnectDelay = Math.min(30000, reconnectDelay * 2); // Double the delay, max 30 seconds
          connect();
          reconnectInterval = null;
        }, reconnectDelay);
      }
    };

    ws.onerror = (error) => {
      wsStatus.value = 'Error';
      console.error('WebSocket error:', error);
      ws.close(); // This will trigger onclose and the reconnect logic
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

