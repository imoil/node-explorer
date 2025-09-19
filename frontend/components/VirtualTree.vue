<template>
  <div v-if="isLoading" class="d-flex flex-column align-center justify-center h-100">
    <v-progress-circular indeterminate size="64" color="primary"></v-progress-circular>
    <p class="mt-4 text-grey">Loading Tree Data...</p>
  </div>

  <RecycleScroller
    v-else-if="flattenedNodes.length > 0"
    ref="scrollerRef"
    class="scroller h-100"
    :items="flattenedNodes"
    :item-size="32"
    :key-field="'id'"
    role="tree"
    aria-label="System Explorer Tree"
    @keydown="handleKeydown"
    v-slot="{ item: node, index }"
  >
    <div
      :class="[
        'tree-node',
        { 'highlight': node.id === highlightId },
        { 'is-visual-focus': isSearchActive && index === focusedIndex }
      ]"
      :style="{ paddingLeft: `${node._depth * 20}px` }"
      :ref="el => { if (index === focusedIndex) focusedItemRef = el }"
      :tabindex="index === focusedIndex ? 0 : -1"
      @click="handleClick(node, index)"
      @focus="focusedIndex = index"
      role="treeitem"
      :aria-level="node._depth + 1"
      :aria-expanded="node.type === 'folder' && node.hasChildren ? node.isOpen : undefined"
      :aria-selected="node.id === highlightId"
      :aria-busy="node.isLoading"
    >
      <div class="node-content">
        <div class="node-icon">
          <v-progress-circular v-if="node.isLoading" indeterminate size="18" width="2" color="primary"></v-progress-circular>
          <template v-else-if="node.type === 'folder'">
            <v-icon v-if="node.hasChildren">{{ node.isOpen ? 'mdi-folder-open-outline' : 'mdi-folder-outline' }}</v-icon>
            <v-icon v-else color="grey-darken-1">mdi-folder-outline</v-icon>
          </template>
          <v-icon v-else-if="node.type === 'sensor'">mdi-access-point</v-icon>
        </div>
        <span class="node-name">{{ node.name }}</span>
        <v-tooltip activator="parent" location="top" open-delay="500">
          <div v-if="node.metadata && Object.keys(node.metadata).length > 0">
            <div v-for="(value, key) in node.metadata" :key="key"><strong>{{ key }}:</strong> {{ value }}</div>
          </div>
          <div v-else>No metadata</div>
        </v-tooltip>
      </div>
    </div>
  </RecycleScroller>
  
  <div v-else class="d-flex flex-column align-center justify-center h-100">
    <v-icon size="64" color="grey-darken-1">mdi-database-off-outline</v-icon>
    <p class="mt-4 text-grey">Failed to load data from the server.</p>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue';
import { RecycleScroller } from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';
import { useRuntimeConfig } from '#app';

// Props
const props = defineProps({
  highlightId: { type: String, default: null },
  isSearchActive: { type: Boolean, default: false }
});

// Emits
const emit = defineEmits(['sensor-selected']);

// State
const config = useRuntimeConfig();
const API_BASE_URL = config.public.apiBaseUrl;

const isLoading = ref(true);
const treeData = ref([]);
const flattenedNodes = ref([]);

const scrollerRef = ref(null);
const focusedIndex = ref(0);
const focusedItemRef = ref(null);

// --- Data Fetching and Manipulation ---
const fetchNodes = async (id = null) => {
  try {
    const url = id ? `${API_BASE_URL}/api/nodes/${id}/children` : `${API_BASE_URL}/api/nodes/root`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Network response was not ok for url: ${url}`);
    const data = await response.json();
    return data.map(node => ({ ...node, children: [], sensors: [], isOpen: false, isLoading: false }));
  } catch (error) {
    console.error("Failed to fetch nodes:", error);
    return [];
  }
};

const findNodeById = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node;
    if (node.children?.length) {
      const found = findNodeById(node.children, id);
      if (found) return found;
    }
    if (node.sensors?.length) {
      const found = node.sensors.find(s => s.id === id);
      if (found) return found;
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
    }
  }
  return count;
};

const handleToggleNode = async (nodeFromEvent) => {
  if (nodeFromEvent.type === 'sensor') {
    emit('sensor-selected', nodeFromEvent);
    return;
  }

  const node = findNodeById(treeData.value, nodeFromEvent.id);
  if (!node || !node.hasChildren) return;

  const parentIndex = flattenedNodes.value.findIndex(n => n.id === node.id);
  if (parentIndex === -1) return;

  let newNodes = [...flattenedNodes.value];
  if (node.isOpen) {
    node.isOpen = false;
    const count = countVisibleDescendants(node);
    newNodes.splice(parentIndex + 1, count);
    newNodes[parentIndex] = { ...node, _depth: nodeFromEvent._depth };
  } else {
    node.isOpen = true;
    if (!node.children?.length && !node.sensors?.length) {
      node.isLoading = true;
      newNodes[parentIndex] = { ...node, _depth: nodeFromEvent._depth };
      flattenedNodes.value = newNodes;

      const items = await fetchNodes(node.id);
      node.children = items.filter(i => i.type === 'folder');
      node.sensors = items.filter(i => i.type === 'sensor');
    }
    node.isLoading = false;
    const itemsToInsert = [...(node.children || []), ...(node.sensors || [])].map(item => ({ ...item, _depth: nodeFromEvent._depth + 1 }));
    newNodes = [...flattenedNodes.value];
    newNodes.splice(parentIndex + 1, 0, ...itemsToInsert);
    newNodes[parentIndex] = { ...node, _depth: nodeFromEvent._depth };
  }
  flattenedNodes.value = newNodes;
};

// --- Lifecycle and Focus Management ---
onMounted(async () => {
  isLoading.value = true;
  const rootNodes = await fetchNodes();
  treeData.value = rootNodes.map(node => ({ ...node, _depth: 0 }));
  flattenedNodes.value = [...treeData.value];
  isLoading.value = false;

  nextTick(() => {
    if (focusedItemRef.value) {
      focusedItemRef.value.focus();
    }
  });
});

// ✨ 실제 포커스와 스크롤을 적용하는 함수
const applyFocus = (index) => {
  if (scrollerRef.value) scrollerRef.value.scrollToItem(index);
  nextTick(() => {
    if (focusedItemRef.value) {
      focusedItemRef.value.focus();
    }
  });
};

// ✨ 부모로부터 호출되는 포커스 설정 함수
const setFocusByIndex = (index) => {
  if (index >= 0 && index < flattenedNodes.value.length) {
    if (focusedIndex.value === index) {
      // 인덱스가 같으면 watch가 실행되지 않으므로, 직접 focus 함수를 호출
      applyFocus(index);
    } else {
      // 인덱스가 다르면, ref를 변경하여 watch가 실행되도록 함
      focusedIndex.value = index;
    }
  }
};

const scrollToNode = (nodeId) => {
  const index = flattenedNodes.value.findIndex(n => n.id === nodeId);
  if (index !== -1) {
    focusedIndex.value = index;
  }
};

// ✨ focusedIndex가 변경될 때마다 applyFocus를 호출
watch(focusedIndex, (index) => {
  applyFocus(index);
});

// --- Keyboard Navigation ---
const handleClick = (node, index) => {
  focusedIndex.value = index;
  handleToggleNode(node);
};

const handleKeydown = (event) => {
  const { key } = event;
  const currentNode = flattenedNodes.value[focusedIndex.value];
  if (!currentNode) return;
  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', ' ', 'Enter'].includes(key)) {
    event.preventDefault();
  }
  switch (key) {
    case 'ArrowUp': if (focusedIndex.value > 0) focusedIndex.value--; break;
    case 'ArrowDown': if (focusedIndex.value < flattenedNodes.value.length - 1) focusedIndex.value++; break;
    case 'ArrowRight': if (currentNode.type === 'folder' && !currentNode.isOpen) handleToggleNode(currentNode); break;
    case 'ArrowLeft': if (currentNode.type === 'folder' && currentNode.isOpen) handleToggleNode(currentNode); break;
    case ' ': case 'Enter': handleToggleNode(currentNode); break;
  }
};

// --- Exposed Methods for Parent ---
const flattenTreeForReveal = (nodes, depth = 0) => {
  let result = [];
  for (const node of nodes) {
    result.push({ ...node, _depth: depth });
    if (node.isOpen) {
      result = result.concat(flattenTreeForReveal(node.children || [], depth + 1));
      (node.sensors || []).forEach(sensor => result.push({ ...sensor, _depth: depth + 1 }));
    }
  }
  return result;
};

const revealPath = async (itemId) => {
  const response = await fetch(`${API_BASE_URL}/api/reveal-path/${itemId}`);
  if (!response.ok) throw new Error('Reveal path request failed');
  const revealData = await response.json();
  const { path, childrenMap } = revealData;

  for (const nodeInPath of path) {
    const originalNode = findNodeById(treeData.value, nodeInPath.id);
    if (originalNode) {
      originalNode.isOpen = true;
      if (childrenMap[originalNode.id]) {
        const items = childrenMap[originalNode.id];
        originalNode.children = items.filter(i => i.type === 'folder');
        originalNode.sensors = items.filter(i => i.type === 'sensor');
      }
    }
  }
  flattenedNodes.value = flattenTreeForReveal(treeData.value);
  await nextTick();
  scrollToNode(itemId);
};

const updateNodeName = (update) => {
    const nodeInTree = findNodeById(treeData.value, update.id);
    if (nodeInTree) {
        nodeInTree.name = update.newName;
    }
    const flatNodeIndex = flattenedNodes.value.findIndex(n => n.id === update.id);
    if (flatNodeIndex > -1) {
        const newNodes = [...flattenedNodes.value];
        newNodes[flatNodeIndex] = { ...newNodes[flatNodeIndex], name: update.newName };
        flattenedNodes.value = newNodes;
    }
};

const getFocusedIndex = () => {
  return focusedIndex.value;
};

const focus = () => {
  setFocusByIndex(focusedIndex.value);
};

defineExpose({ setFocusByIndex, getFocusedIndex, focus, revealPath, updateNodeName });

</script>

<style scoped>
.tree-node {
  height: 32px;
  display: flex;
  align-items: center;
  cursor: pointer;
  padding-right: 16px;
  transition: background-color 0.2s;
}
.tree-node:hover, .tree-node:focus {
  background-color: rgba(255, 255, 255, 0.05);
  outline: none;
}
.tree-node:focus, .tree-node.is-visual-focus {
  outline: 2px solid rgba(var(--v-theme-primary), 0.7);
  outline-offset: -2px;
}
.tree-node.highlight {
  background-color: rgba(var(--v-theme-primary), 0.3);
  animation: fadeOut 2s forwards;
}
@keyframes fadeOut {
  from { background-color: rgba(var(--v-theme-primary), 0.3); }
  to { background-color: transparent; }
}
.node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.node-icon {
  width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.node-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1;
}
</style>