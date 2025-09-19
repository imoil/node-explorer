package com.example.treeapi.handler;

import com.example.treeapi.domain.Node;
import com.example.treeapi.repository.NodeRepository;
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
import java.util.stream.Collectors;

@Component
public class WebSocketUpdateHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final TreeDataService treeDataService;
    private final NodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    @Autowired
    public WebSocketUpdateHandler(TreeDataService treeDataService, NodeRepository nodeRepository, ObjectMapper objectMapper, ScheduledExecutorService webSocketScheduler) {
        this.treeDataService = treeDataService;
        this.nodeRepository = nodeRepository;
        this.objectMapper = objectMapper;
        this.scheduler = webSocketScheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
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
        scheduledTask = scheduler.scheduleAtFixedRate(this::sendBatchUpdate, 5, 5, TimeUnit.SECONDS);
    }

    private void stopSendingUpdates() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
        }
    }

    private void sendBatchUpdate() {
        try {
            if (sessions.isEmpty()) return;

            List<Map<String, String>> batch = createMockUpdateBatch();
            if (batch.isEmpty()) return; // Do not send empty updates

            String messageJson = objectMapper.writeValueAsString(batch);
            TextMessage message = new TextMessage(messageJson);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("WebSocket message sending error: " + e.getMessage());
        }
    }

    private List<Map<String, String>> createMockUpdateBatch() {
        List<Map<String, String>> batch = new ArrayList<>();
        List<Node> allNodes = nodeRepository.findAll();
        if (allNodes.isEmpty()) return batch;

        Random random = new Random();
        int updatesCount = random.nextInt(3) + 1; // 1 to 3 updates per batch

        for (int i = 0; i < updatesCount; i++) {
            Node randomNode = allNodes.get(random.nextInt(allNodes.size()));
            String newName = treeDataService.updateRandomNodeName(randomNode.getId());
            if (newName != null) {
                batch.add(Map.of("id", randomNode.getId(), "newName", newName));
            }
        }
        return batch;
    }
}
