package com.example.treeapi.controller;

import com.example.treeapi.dto.NodeDto;
import com.example.treeapi.dto.RevealPathDto;
import com.example.treeapi.dto.SearchRequest;
import com.example.treeapi.dto.SearchResultDto;
import com.example.treeapi.service.TreeDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NodeController Practical Tests")
class NodeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TreeDataService treeDataService;

    private NodeController nodeController;
    private ObjectMapper objectMapper;

    private NodeDto nodeDto;
    private NodeDto parentNodeDto;
    private NodeDto rootNodeDto;
    private SearchResultDto searchResultDto;
    private RevealPathDto revealPathDto;

    @BeforeEach
    void setUp() {
        nodeController = new NodeController(treeDataService);
        mockMvc = MockMvcBuilders.standaloneSetup(nodeController).build();
        objectMapper = new ObjectMapper();

        // 테스트 데이터 준비
        rootNodeDto = new NodeDto();
        rootNodeDto.setId("1");
        rootNodeDto.setName("Root Node");
        rootNodeDto.setHasChildren(true);

        parentNodeDto = new NodeDto();
        parentNodeDto.setId("2");
        parentNodeDto.setName("Parent Node");
        parentNodeDto.setHasChildren(true);

        nodeDto = new NodeDto();
        nodeDto.setId("3");
        nodeDto.setName("Test Node");
        nodeDto.setHasChildren(false);

        // SearchResultDto 생성
        List<NodeDto> searchPath = Arrays.asList(rootNodeDto, parentNodeDto, nodeDto);
        searchResultDto = new SearchResultDto("4", "Search Result", "file", searchPath);

        // RevealPathDto 생성
        List<NodeDto> revealPath = Arrays.asList(rootNodeDto, parentNodeDto, nodeDto);
        Map<String, List<NodeDto>> childrenMap = new HashMap<>();
        childrenMap.put("1", Arrays.asList(parentNodeDto));
        childrenMap.put("2", Arrays.asList(nodeDto));
        childrenMap.put("3", Collections.emptyList());
        revealPathDto = new RevealPathDto(revealPath, childrenMap);
    }

    @Nested
    @DisplayName("GET /api/nodes/root - 루트 노드 조회")
    class GetRootNodesTests {

        @Test
        @DisplayName("루트 노드 목록을 성공적으로 반환한다")
        void shouldReturnRootNodes() throws Exception {
            // Given
            List<NodeDto> rootNodes = Arrays.asList(rootNodeDto, parentNodeDto);
            when(treeDataService.getRootNodes()).thenReturn(rootNodes);

            // When & Then
            mockMvc.perform(get("/api/nodes/root"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("1"))
                    .andExpect(jsonPath("$[0].name").value("Root Node"))
                    .andExpect(jsonPath("$[0].hasChildren").value(true))
                    .andExpect(jsonPath("$[1].id").value("2"))
                    .andExpect(jsonPath("$[1].name").value("Parent Node"))
                    .andExpect(jsonPath("$[1].hasChildren").value(true));
        }

        @Test
        @DisplayName("빈 루트 노드 목록을 반환한다")
        void shouldReturnEmptyRootNodes() throws Exception {
            // Given
            when(treeDataService.getRootNodes()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/nodes/root"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("단일 루트 노드를 반환한다")
        void shouldReturnSingleRootNode() throws Exception {
            // Given
            when(treeDataService.getRootNodes()).thenReturn(Arrays.asList(rootNodeDto));

            // When & Then
            mockMvc.perform(get("/api/nodes/root"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value("1"));
        }
    }

    @Nested
    @DisplayName("GET /api/nodes/{id}/children - 자식 노드 조회")
    class GetChildrenTests {

        @Test
        @DisplayName("부모 노드의 자식 노드들을 성공적으로 반환한다")
        void shouldReturnChildrenWithValidId() throws Exception {
            // Given
            Long parentId = 2L;
            List<NodeDto> children = Arrays.asList(nodeDto);
            when(treeDataService.getChildrenOf(parentId)).thenReturn(children);

            // When & Then
            mockMvc.perform(get("/api/nodes/{id}/children", parentId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value("3"))
                    .andExpect(jsonPath("$[0].name").value("Test Node"))
                    .andExpect(jsonPath("$[0].hasChildren").value(false));
        }

        @Test
        @DisplayName("여러 자식 노드들을 반환한다")
        void shouldReturnMultipleChildren() throws Exception {
            // Given
            Long parentId = 2L;
            NodeDto child1 = new NodeDto();
            child1.setId("10");
            child1.setName("Child 1");
            child1.setHasChildren(false);

            NodeDto child2 = new NodeDto();
            child2.setId("11");
            child2.setName("Child 2");
            child2.setHasChildren(true);

            List<NodeDto> children = Arrays.asList(child1, child2);
            when(treeDataService.getChildrenOf(parentId)).thenReturn(children);

            // When & Then
            mockMvc.perform(get("/api/nodes/{id}/children", parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("10"))
                    .andExpect(jsonPath("$[0].hasChildren").value(false))
                    .andExpect(jsonPath("$[1].id").value("11"))
                    .andExpect(jsonPath("$[1].hasChildren").value(true));
        }

        @Test
        @DisplayName("자식이 없는 노드는 빈 배열을 반환한다")
        void shouldReturnEmptyChildrenList() throws Exception {
            // Given
            Long leafNodeId = 99L;
            when(treeDataService.getChildrenOf(leafNodeId)).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/nodes/{id}/children", leafNodeId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }


    }

    @Nested
    @DisplayName("POST /api/nodes/search - 노드 검색")
    class SearchNodesTests {

        @Test
        @DisplayName("검색어로 노드를 성공적으로 찾는다")
        void shouldReturnSearchResults() throws Exception {
            // Given
            SearchRequest request = new SearchRequest();
            request.setQuery("test query");

            List<SearchResultDto> results = Arrays.asList(searchResultDto);
            when(treeDataService.searchNodes("test query")).thenReturn(results);

            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value("4"))
                    .andExpect(jsonPath("$[0].name").value("Search Result"))
                    .andExpect(jsonPath("$[0].type").value("file"))
                    .andExpect(jsonPath("$[0].path").isArray())
                    .andExpect(jsonPath("$[0].path.length()").value(3))
                    .andExpect(jsonPath("$[0].path[0].id").value("1"))
                    .andExpect(jsonPath("$[0].path[1].id").value("2"))
                    .andExpect(jsonPath("$[0].path[2].id").value("3"));
        }

        @Test
        @DisplayName("여러 검색 결과를 반환한다")
        void shouldReturnMultipleSearchResults() throws Exception {
            // Given
            SearchRequest request = new SearchRequest();
            request.setQuery("multiple");

            SearchResultDto result1 = new SearchResultDto("file1", "File 1", "file", Arrays.asList(rootNodeDto));
            SearchResultDto result2 = new SearchResultDto("folder1", "Folder 1", "folder", Arrays.asList(rootNodeDto, parentNodeDto));

            List<SearchResultDto> results = Arrays.asList(result1, result2);
            when(treeDataService.searchNodes("multiple")).thenReturn(results);

            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("file1"))
                    .andExpect(jsonPath("$[0].type").value("file"))
                    .andExpect(jsonPath("$[1].id").value("folder1"))
                    .andExpect(jsonPath("$[1].type").value("folder"));
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 배열을 반환한다")
        void shouldReturnEmptySearchResults() throws Exception {
            // Given
            SearchRequest request = new SearchRequest();
            request.setQuery("nonexistent");

            when(treeDataService.searchNodes("nonexistent")).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("잘못된 JSON 형식은 400 에러를 반환한다")
        void shouldReturn400ForInvalidJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"invalidJson\": }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Content-Type이 없으면 415 에러를 반환한다")
        void shouldReturn415ForMissingContentType() throws Exception {
            // Given
            SearchRequest request = new SearchRequest();
            request.setQuery("test");

            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("GET /api/nodes/reveal-path/{nodeId} - 노드 경로 조회")
    class RevealPathTests {

        @Test
        @DisplayName("노드의 전체 경로와 자식 맵을 성공적으로 반환한다")
        void shouldReturnRevealPath() throws Exception {
            // Given
            Long nodeId = 3L;
            when(treeDataService.revealPath(nodeId)).thenReturn(revealPathDto);

            // When & Then
            mockMvc.perform(get("/api/nodes/reveal-path/{nodeId}", nodeId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.path").isArray())
                    .andExpect(jsonPath("$.path.length()").value(3))
                    .andExpect(jsonPath("$.path[0].id").value("1"))
                    .andExpect(jsonPath("$.path[1].id").value("2"))
                    .andExpect(jsonPath("$.path[2].id").value("3"))
                    .andExpect(jsonPath("$.childrenMap").isMap())
                    .andExpect(jsonPath("$.childrenMap.1").isArray())
                    .andExpect(jsonPath("$.childrenMap.2").isArray())
                    .andExpect(jsonPath("$.childrenMap.3").isArray());
        }

        @Test
        @DisplayName("루트 노드의 경로를 반환한다")
        void shouldReturnRootNodePath() throws Exception {
            // Given
            Long rootId = 1L;
            RevealPathDto rootRevealPath = new RevealPathDto(
                    Arrays.asList(rootNodeDto),
                    Map.of("1", Arrays.asList(parentNodeDto))
            );
            when(treeDataService.revealPath(rootId)).thenReturn(rootRevealPath);

            // When & Then
            mockMvc.perform(get("/api/nodes/reveal-path/{nodeId}", rootId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path.length()").value(1))
                    .andExpect(jsonPath("$.path[0].id").value("1"))
                    .andExpect(jsonPath("$.childrenMap.1").isArray());
        }

        @Test
        @DisplayName("빈 경로 결과를 처리한다")
        void shouldHandleEmptyRevealPath() throws Exception {
            // Given
            Long nodeId = 404L;
            RevealPathDto emptyRevealPath = new RevealPathDto(Collections.emptyList(), new HashMap<>());
            when(treeDataService.revealPath(nodeId)).thenReturn(emptyRevealPath);

            // When & Then
            mockMvc.perform(get("/api/nodes/reveal-path/{nodeId}", nodeId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.path").isArray())
                    .andExpect(jsonPath("$.path.length()").value(0))
                    .andExpect(jsonPath("$.childrenMap").isMap());
        }


    }

    @Nested
    @DisplayName("Integration Scenarios - 통합 시나리오")
    class IntegrationTests {

        @Test
        @DisplayName("전체 워크플로우: 루트 조회 → 자식 조회 → 검색 → 경로 조회")
        void shouldHandleCompleteWorkflow() throws Exception {
            // Given
            when(treeDataService.getRootNodes()).thenReturn(Arrays.asList(rootNodeDto));
            when(treeDataService.getChildrenOf(1L)).thenReturn(Arrays.asList(parentNodeDto));
            when(treeDataService.searchNodes("Test")).thenReturn(Arrays.asList(searchResultDto));
            when(treeDataService.revealPath(4L)).thenReturn(revealPathDto);

            // When & Then
            // 1. 루트 노드 조회
            mockMvc.perform(get("/api/nodes/root"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("1"));

            // 2. 루트의 자식 노드 조회
            mockMvc.perform(get("/api/nodes/1/children"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("2"));

            // 3. 노드 검색
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery("Test");
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("4"))
                    .andExpect(jsonPath("$[0].type").value("file"));

            // 4. 검색된 노드의 경로 조회
            mockMvc.perform(get("/api/nodes/reveal-path/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.path").isArray())
                    .andExpect(jsonPath("$.childrenMap").isMap());
        }

        @Test
        @DisplayName("다양한 타입의 검색 결과 처리")
        void shouldHandleDifferentSearchResultTypes() throws Exception {
            // Given
            SearchResultDto folderResult = new SearchResultDto("folder1", "My Folder", "folder",
                    Arrays.asList(rootNodeDto));
            SearchResultDto fileResult = new SearchResultDto("file1", "My File.txt", "file",
                    Arrays.asList(rootNodeDto, parentNodeDto));
            SearchResultDto documentResult = new SearchResultDto("doc1", "Document.pdf", "document",
                    Arrays.asList(rootNodeDto, parentNodeDto, nodeDto));

            List<SearchResultDto> mixedResults = Arrays.asList(folderResult, fileResult, documentResult);
            when(treeDataService.searchNodes("mixed")).thenReturn(mixedResults);

            SearchRequest request = new SearchRequest();
            request.setQuery("mixed");

            // When & Then
            mockMvc.perform(post("/api/nodes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].type").value("folder"))
                    .andExpect(jsonPath("$[0].path.length()").value(1))
                    .andExpect(jsonPath("$[1].type").value("file"))
                    .andExpect(jsonPath("$[1].path.length()").value(2))
                    .andExpect(jsonPath("$[2].type").value("document"))
                    .andExpect(jsonPath("$[2].path.length()").value(3));
        }
    }
}