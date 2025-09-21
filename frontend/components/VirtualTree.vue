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
        { 'highlight': node.id === highlightedItemId },
        { 'is-focused': index === focusedIndex } // Simplified, reliable focus class
      ]"
      :style="{ paddingLeft: `${node._depth * 20}px` }"
      :ref="el => { if (index === focusedIndex) focusedItemRef = el }"
      :tabindex="index === focusedIndex ? 0 : -1"
      @click="handleClick(node, index)"
      @focus="focusedIndex = index"
      role="treeitem"
      :aria-level="node._depth + 1"
      :aria-expanded="node.type === 'folder' && node.hasChildren ? openNodes[node.id] : undefined"
      :aria-selected="node.id === highlightedItemId"
      :aria-busy="node.isLoading"
    >
      <div class="node-content">
        <div class="node-icon">
          <v-progress-circular v-if="node.isLoading" indeterminate size="18" width="2" color="primary"></v-progress-circular>
          <template v-else-if="node.type === 'folder'">
            <v-icon v-if="node.hasChildren">{{ openNodes[node.id] ? 'mdi-folder-open-outline' : 'mdi-folder-outline' }}</v-icon>
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

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { RecycleScroller } from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';
import { useTreeStore } from '~/stores/tree';

const emit = defineEmits(['sensor-selected']);

// --- Pinia Store ---
const store = useTreeStore();
const {
  rootNodeIds,
  highlightedItemId,
  isSearchActive,
  virtualTreeRef,
  openNodes,
  scrollToNodeId,
} = storeToRefs(store);
const { fetchNodes, getNode, getChildren } = store;

// --- Local State ---
const isLoading = ref(true);
const scrollerRef = ref<any>(null);
const focusedIndex = ref(0);
const focusedItemRef = ref<any>(null);

// --- Computed Properties ---
const flattenedNodes = computed(() => {
  const result: any[] = [];
  const flatten = (nodeIds: string[], depth: number) => {
    for (const nodeId of nodeIds) {
      const node = getNode(nodeId);
      if (!node) continue;

      result.push({ ...node, _depth: depth });

      if (openNodes.value[node.id] && node.hasChildren) {
        const children = getChildren(node.id);
        if (children.length > 0) {
          flatten(children.map(c => c.id), depth + 1);
        }
      }
    }
  };

  flatten(rootNodeIds.value, 0);
  return result;
});

// --- Data Fetching and Manipulation ---
const handleToggleNode = async (node: any) => {
  if (node.type === 'sensor') {
    emit('sensor-selected', node);
    return;
  }

  if (!node.hasChildren) return;

  const isOpen = !openNodes.value[node.id];
  openNodes.value[node.id] = isOpen;

  if (isOpen) {
    const children = getChildren(node.id);
    if (children.length === 0) {
      const nodeInTree = getNode(node.id);
      if (nodeInTree) nodeInTree.isLoading = true;
      await fetchNodes(node.id);
      if (nodeInTree) nodeInTree.isLoading = false;
    }
  }
};

// --- Lifecycle and Focus Management ---
onMounted(async () => {
  virtualTreeRef.value = {
    setFocusByIndex,
    getFocusedIndex: () => focusedIndex.value,
    focus: focus,
  };

  isLoading.value = true;
  await fetchNodes(null);
  isLoading.value = false;

  await nextTick();
  focus();
});

const applyFocus = (index: number) => {
  nextTick(() => {
    focusedItemRef.value?.focus();
  });
};

const setFocusByIndex = (index: number) => {
  if (index >= 0 && index < flattenedNodes.value.length) {
    focusedIndex.value = index;
  }
};

watch(focusedIndex, (index) => {
  applyFocus(index);
  nextTick(() => {
      focusedItemRef.value?.scrollIntoView({ block: 'nearest', behavior: 'auto' });
  });
});

watch(openNodes, () => {
  nextTick(() => {
    focusedItemRef.value?.scrollIntoView({ block: 'nearest', behavior: 'auto' });
  });
}, { deep: true });


watch(scrollToNodeId, (targetId) => {
  if (targetId) {
    const index = flattenedNodes.value.findIndex(n => n.id === targetId);
    if (index !== -1) {
      setTimeout(() => {
        scrollerRef.value.scrollToItem(index);
        setFocusByIndex(index);
        scrollToNodeId.value = null; // Reset the signal
      }, 50);
    }
  }
});

// --- Keyboard Navigation ---
const handleClick = (node: any, index: number) => {
  focusedIndex.value = index;
  handleToggleNode(node);
};

const handleKeydown = (event: KeyboardEvent) => {
  const { key } = event;
  const currentIndex = focusedIndex.value;
  const currentNode = flattenedNodes.value[currentIndex];
  if (!currentNode) return;

  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', ' ', 'Enter'].includes(key)) {
    event.preventDefault();
  }

  switch (key) {
    case 'ArrowUp':
      if (currentIndex > 0) focusedIndex.value--;
      break;
    case 'ArrowDown':
      if (currentIndex < flattenedNodes.value.length - 1) focusedIndex.value++;
      break;
    case 'ArrowRight':
      if (currentNode.type === 'folder' && currentNode.hasChildren) {
        if (!openNodes.value[currentNode.id]) {
          handleToggleNode(currentNode);
          nextTick(() => {
            if ((currentIndex + 1) < flattenedNodes.value.length && flattenedNodes.value[currentIndex + 1]._depth > currentNode._depth) {
              focusedIndex.value++;
            }
          });
        } else {
          if ((currentIndex + 1) < flattenedNodes.value.length && flattenedNodes.value[currentIndex + 1]._depth > currentNode._depth) {
            focusedIndex.value++;
          }
        }
      }
      break;
    case 'ArrowLeft':
      if (currentNode.type === 'folder' && openNodes.value[currentNode.id]) {
        handleToggleNode(currentNode);
      } else if (currentNode._depth > 0) {
        const parentIndex = flattenedNodes.value.findLastIndex((n, i) => i < currentIndex && n._depth < currentNode._depth);
        if (parentIndex !== -1) {
          focusedIndex.value = parentIndex;
        }
      }
      break;
    case ' ':
    case 'Enter':
      handleToggleNode(currentNode);
      break;
  }
};

const focus = () => {
  applyFocus(focusedIndex.value);
};

defineExpose({ focus });

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
.tree-node:hover {
  background-color: rgba(255, 255, 255, 0.05);
}
.tree-node.is-focused {
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