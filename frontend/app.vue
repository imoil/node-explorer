<template>
  <v-app theme="dark">
    <v-main>
      <v-container>
        <div class="max-w-4xl mx-auto">
          <TheHeader />

          <!-- Search Form -->
          <form @submit.prevent="handleSearch" class="mb-6">
            <v-text-field
              v-model="searchQuery"
              label="Search nodes or sensors and press Enter..."
              variant="solo"
              prepend-inner-icon="mdi-magnify"
              hide-details
              :loading="isSearching"
              :disabled="isSearching"
            />
          </form>

          <!-- Tree View Container -->
          <v-card flat border>
            <v-toolbar flat density="compact">
               <v-toolbar-title class="text-body-2">
                System Explorer
              </v-toolbar-title>
              <v-spacer></v-spacer>
              <div class="d-flex align-center text-body-2 mr-4">
                <v-icon :color="wsStatus === 'Connected' ? 'green' : 'red'" icon="mdi-circle" size="x-small" class="mr-2"></v-icon>
                WebSocket: {{ wsStatus }}
              </div>
            </v-toolbar>
            <v-divider></v-divider>
            
            <v-card-text class="pa-0" style="height: 70vh; overflow-y: auto;">
              <!-- Loading State -->
              <div v-if="isLoadingRoot" class="d-flex flex-column align-center justify-center h-100">
                <v-progress-circular indeterminate size="64" color="primary"></v-progress-circular>
                <p class="mt-4 text-grey">Loading Root Nodes...</p>
              </div>
              
              <!-- Tree View Component -->
              <VirtualTree
                v-else-if="flattenedNodes.length > 0"
                ref="virtualTreeRef"
                :nodes="flattenedNodes"
                :highlight-id="highlightedItemId"
                @toggle-node="handleToggleNode"
              />

              <!-- No Data State -->
              <div v-else class="d-flex flex-column align-center justify-center h-100">
                  <v-icon size="64" color="grey-darken-1">mdi-database-off-outline</v-icon>
                  <p class="mt-4 text-grey">Failed to load data from the server.</p>
              </div>
            </v-card-text>

            <!-- Search Status Overlay -->
            <v-overlay v-model="searchStatusOverlay" scrim="#000" class="d-flex align-center justify-center" persistent>
               <div class="d-flex flex-column align-center text-center pa-4">
                 <v-progress-circular v-if="isSearching" indeterminate size="48" class="mb-4"></v-progress-circular>
                 <p>{{ searchStatus }}</p>
               </div>
            </v-overlay>
          </v-card>
        </div>
      </v-container>
    </v-main>

    <!-- Search Results Modal Component -->
    <SearchResultsModal
      v-model="isModalOpen"
      :results="modalResults"
      @select-item="selectItem"
    />
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import TheHeader from '~/components/TheHeader.vue';
import VirtualTree from '~/components/VirtualTree.vue';
import SearchResultsModal from '~/components/SearchResultsModal.vue';
import { useWebSocket } from '~/composables/useWebSocket';
import { useTreeSearch } from '~/composables/useTreeSearch';

import { useRuntimeConfig } from '#app';

const config = useRuntimeConfig();
const API_BASE_URL = config.public.apiBaseUrl;
const treeData = ref([]);
const flattenedNodes = ref([]);
const isLoadingRoot = ref(true);
const virtualTreeRef = ref(null);

const fetchNodes = async (id = null) => {
  try {
    const url = id ? `${API_BASE_URL}/api/nodes/${id}/children` : `${API_BASE_URL}/api/nodes/root`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Network response was not ok for url: ${url}`);
    const data = await response.json();
    // 백엔드에서 받은 데이터에 프론트엔드 상태값을 추가합니다.
    return data.map(node => ({ ...node, children: [], isOpen: false, isLoading: false }));
  } catch (error) {
    console.error("Failed to fetch nodes:", error);
    return [];
  }
};

const findNodeById = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node;

    // Search within the children of the current node
    if (node.children && node.children.length > 0) {
      const foundInChildren = findNodeById(node.children, id);
      if (foundInChildren) return foundInChildren;
    }

    // Search within the sensors of the current node
    if (node.sensors && node.sensors.length > 0) {
      const foundInSensors = node.sensors.find(sensor => sensor.id === id);
      if (foundInSensors) return foundInSensors;
    }
  }
  return null;
};

const countVisibleDescendants = (node) => {
  if (!node.isOpen) return 0;
  let count = (node.children?.length || 0) + (node.sensors?.length || 0);
  if (node.children) {
    for (const child of node.children) {
      count += countVisibleDescendants(child);
    }n  }
  return count;
};

const handleToggleNode = async (nodeFromEvent) => {
  const node = findNodeById(treeData.value, nodeFromEvent.id);
  if (!node || node.type !== 'folder' || !node.hasChildren) return;

  const parentIndex = flattenedNodes.value.findIndex(n => n.id === node.id);
  if (parentIndex === -1) return;

  // --- Close Node Logic ---
  if (node.isOpen) {
    const descendantCount = countVisibleDescendants(node);
    if (descendantCount > 0) {
      flattenedNodes.value.splice(parentIndex + 1, descendantCount);
    }
    node.isOpen = false;
    flattenedNodes.value[parentIndex] = { ...node }; // Update node state in flat list
    return;
  }

  // --- Open Node Logic ---
  node.isOpen = true;
  // Only fetch if children are not loaded yet
  if (!node.children || node.children.length === 0 && !node.sensors || node.sensors.length === 0) {
    node.isLoading = true;
    flattenedNodes.value[parentIndex] = { ...node }; // Show loading indicator

    const fetchedItems = await fetchNodes(node.id);
    node.children = fetchedItems.filter(item => item.type === 'folder');
    node.sensors = fetchedItems.filter(item => item.type === 'sensor');
  }

  node.isLoading = false;
  const itemsToInsert = [...(node.children || []), ...(node.sensors || [])].map(item => ({ ...item, _depth: node._depth + 1 }));
  if (itemsToInsert.length > 0) {
    flattenedNodes.value.splice(parentIndex + 1, 0, ...itemsToInsert);
  }
  flattenedNodes.value[parentIndex] = { ...node }; // Update final state
};

const flattenTreeForReveal = (nodes, depth = 0) => {
  let result = [];
  for (const node of nodes) {
    const nodeWithDepth = { ...node, _depth: depth };
    result.push(nodeWithDepth);
    if (node.isOpen) {
      const children = node.children || [];
      const sensors = node.sensors || [];
      result = result.concat(flattenTreeForReveal(children, depth + 1));
      sensors.forEach(sensor => result.push({ ...sensor, _depth: depth + 1 }));
    }
  }
  return result;
};

const handleRevealPath = async (revealData) => {
  const { path, childrenMap } = revealData;

  for (const nodeInPath of path) {
    const originalNode = findNodeById(treeData.value, nodeInPath.id);
    if (originalNode) {
      originalNode.isOpen = true;
      if (childrenMap[originalNode.id]) {
        const items = childrenMap[originalNode.id];
        originalNode.children = items.filter(item => item.type === 'folder');
        originalNode.sensors = items.filter(item => item.type === 'sensor');
      }
    }
  }
  flattenedNodes.value = flattenTreeForReveal(treeData.value);
};

const { wsStatus } = useWebSocket((payload) => {
  payload.forEach(update => {
    const node = findNodeById(treeData.value, update.id);
    if (node) {
      node.name = update.newName;
      const flatNodeIndex = flattenedNodes.value.findIndex(n => n.id === update.id);
      if (flatNodeIndex > -1) {
        flattenedNodes.value[flatNodeIndex] = { ...flattenedNodes.value[flatNodeIndex], name: update.newName };
      }
    }
  });
});

const {
  searchQuery,
  isSearching,
  searchStatus,
  highlightedItemId,
  isModalOpen,
  modalResults,
  searchStatusOverlay,
  handleSearch,
  selectItem,
} = useTreeSearch(treeData, handleToggleNode, virtualTreeRef, handleRevealPath);

onMounted(async () => {
  try {
    console.log('[onMounted] 1. Start initialization...');
    isLoadingRoot.value = true;

    console.log('[onMounted] 2. Fetching root nodes...');
    const rootNodes = await fetchNodes();
    console.log(`[onMounted] 3. Fetched ${rootNodes.length} root nodes.`);

    console.log('[onMounted] 4. Populating treeData...');
    treeData.value = rootNodes.map(node => ({ ...node, _depth: 0 }));

    console.log('[onMounted] 5. Populating flattenedNodes...');
    flattenedNodes.value = [...treeData.value];

    console.log('[onMounted] 6. Initialization complete. Setting isLoadingRoot to false.');
    isLoadingRoot.value = false;
  } catch (error) {
    console.error('[onMounted] CRITICAL ERROR during initialization:', error);
    isLoadingRoot.value = false; // Stop loading even if there is an error
  }
});
</script>

<style>
.vue-recycle-scroller__item-wrapper, .vue-recycle-scroller__item-view {
  box-sizing: border-box;
}
</style>

