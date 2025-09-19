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
                v-else-if="treeData.length > 0"
                :nodes="treeData"
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

<script setup>
import { ref, onMounted } from 'vue';
import TheHeader from '~/components/TheHeader.vue';
import VirtualTree from '~/components/VirtualTree.vue';
import SearchResultsModal from '~/components/SearchResultsModal.vue';
import { useWebSocket } from '~/composables/useWebSocket';
import { useTreeSearch } from '~/composables/useTreeSearch';

const API_BASE_URL = 'http://localhost:8080';
const treeData = ref([]);
const isLoadingRoot = ref(true);

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

const handleToggleNode = async (node) => {
  // ========================[ 디버깅 로그 추가 ]========================
  console.log('Toggling node:', { id: node.id, name: node.name, type: node.type, hasChildren: node.hasChildren });

  // 클릭된 노드가 폴더가 아니거나, 자식이 없다고 명시된 경우 확장을 시도하지 않습니다.
  if (node.type !== 'folder' || !node.hasChildren) {
    console.log('Node is not an expandable folder. Aborting toggle.');
    return;
  }
  // =================================================================

  if (node.isOpen) {
    node.isOpen = false;
  } else {
    node.isOpen = true;
    // 이전에 자식을 불러온 적이 없다면 API를 호출합니다.
    if (!node.children || node.children.length === 0) {
      console.log(`Fetching children for node ${node.id}...`);
      node.isLoading = true;
      node.children = await fetchNodes(node.id);
      node.isLoading = false;
      console.log(`Finished fetching children for node ${node.id}. Found ${node.children.length} items.`);
    }
  }
};

const updateNodeInTree = (nodes, id, updates) => {
  for (const node of nodes) {
    if (node.id === id) {
      Object.assign(node, updates);
      return true;
    }
    if (node.children && node.children.length > 0) {
      if (updateNodeInTree(node.children, id, updates)) return true;
    }
  }
  return false;
};

const { wsStatus } = useWebSocket((payload) => {
  payload.forEach(update => updateNodeInTree(treeData.value, update.id, { name: update.newName }));
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
} = useTreeSearch(treeData, handleToggleNode);

onMounted(async () => {
  isLoadingRoot.value = true;
  treeData.value = await fetchNodes();
  isLoadingRoot.value = false;
});
</script>

<style>
.vue-recycle-scroller__item-wrapper, .vue-recycle-scroller__item-view {
  box-sizing: border-box;
}
</style>

