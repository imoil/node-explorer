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
    :key-field="'uniqueId'"
    role="tree"
    aria-label="System Explorer Tree"
    @keydown="handleKeydown"
    v-slot="{ item: node, index }"
  >
    <div
      :class="[
        'tree-node',
        { 'highlight': node.id === highlightedItemId },
        { 'is-focused': node.isFocused }
      ]"
      :style="{ paddingLeft: `${node._depth * 20}px` }"
      :ref="el => { if (node.isFocused) focusedItemRef = el; }"
      :tabindex="node.isFocused ? 0 : -1"
      @click="handleClick(node, index)"
      role="treeitem"
      :aria-level="node._depth + 1"
      :aria-expanded="node.type === 'folder' && node.hasChildren ? openNodes[node.id] : undefined"
      :aria-selected="node.id === highlightedItemId"
      :aria-busy="node.isLoading"
    >
      <div class="node-content">
        <div class="node-icon">
          <v-progress-circular v-if="loadingNodes[node.id]" indeterminate size="18" width="2" color="primary"></v-progress-circular>
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
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { storeToRefs } from 'pinia';
import { RecycleScroller } from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';
import { useTreeStore } from '~/stores/tree';

const emit = defineEmits(['sensor-selected']);

// --- Pinia Store ---
const store = useTreeStore();
const {
  highlightedItemId,
  openNodes,
  scrollToNodeId,
  virtualTreeRef,
} = storeToRefs(store);
const { fetchNodes, getNode, getRootNodeIds, getChildrenIds } = store;

// --- Local State ---
const isLoading = ref(true);
const loadingNodes = ref<Record<string, boolean>>({});
const scrollerRef = ref<any>(null);
const focusedItemRef = ref<any>(null);

// --- Computed Properties ---
const flattenedNodes = ref<any[]>([]);

const updateFlattenedNodes = (focusedNodeId: string | null = null) => {
  const result: any[] = [];
  let focusedIndex = -1;

  const flatten = (nodeIdentifiers: {id: string, type: string}[], depth: number) => {
    for (const { id, type } of nodeIdentifiers) {
      const node = getNode(id, type);
      if (!node) continue;

      const isFocused = focusedNodeId ? (node.uniqueId === focusedNodeId) : false;
      if (isFocused) {
        focusedIndex = result.length;
      }

      result.push({
        ...node,
        uniqueId: `${type}-${id}`,
        _depth: depth,
        isFocused: isFocused,
      });

      if (openNodes.value[id] && node.hasChildren) {
        const childrenIds = getChildrenIds(id);
        if (childrenIds.length > 0) {
          flatten(childrenIds, depth + 1);
        }
      }
    }
  };

  flatten(getRootNodeIds(), 0);
  
  if (focusedIndex === -1 && result.length > 0) {
    result[0].isFocused = true;
  }

  flattenedNodes.value = result;
  applyFocus();
};

watch(openNodes, () => {
  const focusedNode = flattenedNodes.value.find(n => n.isFocused);
  updateFlattenedNodes(focusedNode?.uniqueId || null);
}, { deep: true });

// --- Data Fetching and Manipulation ---
const handleToggleNode = async (node: any) => {
  if (node.type === 'sensor') {
    emit('sensor-selected', node);
    return;
  }
  if (!node.hasChildren) return;

  const isOpen = !openNodes.value[node.id];

  if (isOpen && getChildrenIds(node.id).length === 0) {
    loadingNodes.value[node.id] = true;
    await fetchNodes(node.id);
    loadingNodes.value[node.id] = false;
  }

  openNodes.value[node.id] = isOpen;
};

// --- Lifecycle and Focus Management ---
onMounted(async () => {
  virtualTreeRef.value = {
    focus: focus,
  };

  isLoading.value = true;
  await fetchNodes(null);
  updateFlattenedNodes();
  isLoading.value = false;

  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});

const handleGlobalKeydown = (event: KeyboardEvent) => {
  const target = event.target as HTMLElement;
  if (['INPUT', 'TEXTAREA'].includes(target.tagName)) {
    return;
  }
  if (event.key === 't') {
    event.preventDefault();
    focus();
  }
};

const applyFocus = () => {
  setTimeout(() => {
    focusedItemRef.value?.focus();
    focusedItemRef.value?.scrollIntoView({ block: 'nearest', behavior: 'auto' });
  }, 0);
};

const setFocusByIndex = (index: number) => {
  if (index >= 0 && index < flattenedNodes.value.length) {
    const currentFocused = flattenedNodes.value.find(n => n.isFocused);
    if (currentFocused) {
      currentFocused.isFocused = false;
    }
    flattenedNodes.value[index].isFocused = true;
    applyFocus();
  }
};

watch(scrollToNodeId, (targetId) => {
  if (targetId) {
    nextTick(async () => {
      const path = await store.revealPath(targetId);
      if (path) {
        const targetNode = path.find(p => p.id === targetId);
        if (targetNode) {
          updateFlattenedNodes(`${targetNode.type}-${targetNode.id}`);
        }
      }
      scrollToNodeId.value = null;
    });
  }
});

// --- Keyboard Navigation ---
const handleClick = (node: any, index: number) => {
  setFocusByIndex(index);
  handleToggleNode(node);
};

const handleKeydown = async (event: KeyboardEvent) => {
  const { key } = event;
  const currentIndex = flattenedNodes.value.findIndex(n => n.isFocused);
  if (currentIndex === -1) return;

  event.preventDefault();

  const currentNode = flattenedNodes.value[currentIndex];
  let newIndex = currentIndex;

  switch (key) {
    case 'ArrowUp':
      if (currentIndex > 0) newIndex = currentIndex - 1;
      break;
    case 'ArrowDown':
      if (currentIndex < flattenedNodes.value.length - 1) newIndex = currentIndex + 1;
      break;
    case 'ArrowRight':
      if (currentNode.type === 'folder' && currentNode.hasChildren) {
        if (!openNodes.value[currentNode.id]) {
          await handleToggleNode(currentNode);
        } else {
          if ((currentIndex + 1) < flattenedNodes.value.length) {
            newIndex = currentIndex + 1;
          }
        }
      }
      break;
    case 'ArrowLeft':
      if (currentNode.type === 'folder' && openNodes.value[currentNode.id]) {
        await handleToggleNode(currentNode);
      } else if (currentNode._depth > 0) {
        const parentIndex = flattenedNodes.value.findLastIndex((n, i) => i < currentIndex && n._depth < currentNode._depth);
        if (parentIndex !== -1) {
          newIndex = parentIndex;
        }
      }
      break;
    case ' ':
    case 'Enter':
      await handleToggleNode(currentNode);
      break;
  }

  if (newIndex !== currentIndex) {
    setFocusByIndex(newIndex);
  } else {
    applyFocus();
  }
};

const focus = () => {
  const focused = flattenedNodes.value.find(n => n.isFocused);
  if (!focused && flattenedNodes.value.length > 0) {
    flattenedNodes.value[0].isFocused = true;
  }
  applyFocus();
};

defineExpose({ focus });

</script>

<style scoped>
.scroller {
  height: 100%;
}
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
