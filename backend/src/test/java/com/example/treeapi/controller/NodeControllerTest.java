
package com.example.treeapi.controller;

import com.example.treeapi.service.TreeDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NodeController.class)
class NodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TreeDataService treeDataService;

    @Test
    void searchNodes_whenValidRequest_shouldReturnOk() throws Exception {
        // Given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", "valid_query");

        when(treeDataService.searchNodes(anyString())).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    @Test
    void searchNodes_whenQueryIsEmpty_shouldReturnBadRequest() throws Exception {
        // Given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", "");

        // When & Then
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.query").value("Search query cannot be empty."));
    }

    @Test
    void searchNodes_whenQueryIsBlank_shouldReturnBadRequest() throws Exception {
        // Given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", "   ");

        // When & Then
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.query").value("Search query cannot be empty."));
    }

    @Test
    void searchNodes_whenQueryIsTooLong_shouldReturnBadRequest() throws Exception {
        // Given
        String longQuery = "a".repeat(101);
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", longQuery);

        // When & Then
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.query").value("Search query cannot exceed 100 characters."));
    }

    @Test
    void getChildren_whenIdIsInvalid_shouldReturnBadRequest() throws Exception {
        // Given
        String invalidId = "invalid-id-with-special-chars-!";

        // When & Then
        mockMvc.perform(get("/api/nodes/{id}/children", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.id").value("Invalid node ID format"));
    }
}
