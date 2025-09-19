package com.example.treeapi.handler;

import com.example.treeapi.service.TreeDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class WebSocketUpdateHandler extends TextWebSocketHandler {

    // [유지] 세션을 관리하는 리스트는 스레드에 안전한 CopyOnWriteArrayList를 그대로 사용합니다.
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    // [수정] 의존성은 final 필드로 선언하고 생성자를 통해 주입받는 것이 권장됩니다.
    private final TreeDataService treeDataService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    // [추가] 예약된 작업을 제어하기 위한 핸들입니다.
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public WebSocketUpdateHandler(TreeDataService treeDataService, ObjectMapper objectMapper, ScheduledExecutorService webSocketScheduler) {
        this.treeDataService = treeDataService;
        this.objectMapper = objectMapper;
        this.scheduler = webSocketScheduler; // AppConfig에 정의된 전역 스케줄러 Bean이 주입됩니다.
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        // [수정] 작업이 없거나 취소된 상태일 때만 새로 시작하도록 조건을 더 명확하게 합니다.
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            startSendingUpdates();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        if (sessions.isEmpty()) {
            stopSendingUpdates();
        }
    }

    private void startSendingUpdates() {
        // [수정] scheduler.scheduleAtFixedRate의 결과를 scheduledTask 변수에 저장합니다.
        scheduledTask = scheduler.scheduleAtFixedRate(this::sendBatchUpdate, 5, 5, TimeUnit.SECONDS);
    }

    private void stopSendingUpdates() {
        // [수정] 스케줄러를 종료하는 대신, 예약된 작업만 취소합니다.
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false); // false는 현재 실행중인 작업은 마치고 종료하라는 의미입니다.
        }
    }

    private void sendBatchUpdate() {
        // [유지] 메시지를 생성하고 보내는 로직은 원본 코드를 그대로 유지합니다.
        try {
            if (sessions.isEmpty()) return; // [개선] 메시지를 보내기 전에 세션이 비었는지 한번 더 확인합니다.

            List<Map<String, String>> batch = createMockUpdateBatch();
            
            // 프론트엔드에서 payload만 바로 사용하도록 JSON 구조를 단순화합니다.
            // 만약 원래 구조가 필요하다면 아래 주석처리된 코드를 사용하세요.
            String messageJson = objectMapper.writeValueAsString(batch);
            /*
            Map<String, Object> messagePayload = Map.of(
                    "type", "NODE_UPDATES_BATCH",
                    "payload", batch
            );
            String messageJson = objectMapper.writeValueAsString(messagePayload);
            */

            TextMessage message = new TextMessage(messageJson);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("WebSocket 메시지 전송 중 오류 발생: " + e.getMessage());
        }
    }

    private List<Map<String, String>> createMockUpdateBatch() {
        // [유지] Mock 데이터를 생성하는 로직은 원본 코드를 그대로 유지합니다.
        List<Map<String, String>> batch = new ArrayList<>();
        List<String> nodeIds = new ArrayList<>(treeDataService.getAllNodesMap().keySet());
        if (nodeIds.isEmpty()) return batch;

        Random random = new Random();
        int updatesCount = random.nextInt(3) + 1; // 1 to 3 updates per batch

        for (int i = 0; i < updatesCount; i++) {
            String randomId = nodeIds.get(random.nextInt(nodeIds.size()));
            String originalName = treeDataService.getAllNodesMap().get(randomId).getName();
            // 이름 뒤에 붙는 숫자 부분을 업데이트하는 로직 (예: "Node (12)" -> "Node (58)")
            String newName = originalName.replaceAll(" \\(\\d+\\)$", "") + " (" + (random.nextInt(90) + 10) + ")";

            batch.add(Map.of("id", randomId, "newName", newName));
        }
        return batch;
    }
}
