<template>
  <RecycleScroller
    class="scroller h-100"
    :items="flattenedNodes"
    :item-size="32"
    :key-field="'id'"
    v-slot="{ item: node }"
  >
    <div
      :class="['tree-node', { 'highlight': node.id === highlightId }]"
      :style="{ paddingLeft: `${node._depth * 20}px` }"
      @click="emit('toggleNode', node)"
    >
      <div class="node-content">
        <!-- Icon and Loading Indicator -->
        <div class="node-icon">
          <v-progress-circular
            v-if="node.isLoading"
            indeterminate
            size="18"
            width="2"
            color="primary"
          ></v-progress-circular>
          <template v-else-if="node.type === 'folder'">
             <v-icon v-if="node.hasChildren">
              {{ node.isOpen ? 'mdi-folder-open-outline' : 'mdi-folder-outline' }}
            </v-icon>
            <v-icon v-else color="grey-darken-1">
              mdi-folder-outline
            </v-icon>
          </template>
          <v-icon v-else-if="node.type === 'sensor'">
            mdi-access-point
          </v-icon>
        </div>

        <!-- Node Name -->
        <span class="node-name">{{ node.name }}</span>

        <!-- Metadata Tooltip -->
        <v-tooltip activator="parent" location="top" open-delay="500">
          <div v-if="node.metadata && Object.keys(node.metadata).length > 0">
            <div v-for="(value, key) in node.metadata" :key="key">
              <strong>{{ key }}:</strong> {{ value }}
            </div>
          </div>
          <div v-else>No metadata</div>
        </v-tooltip>
      </div>
    </div>
  </RecycleScroller>
</template>

<script setup>
import { computed } from 'vue';
import { RecycleScroller } from 'vue-virtual-scroller';
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';

const props = defineProps({
  nodes: {
    type: Array,
    required: true,
  },
  highlightId: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(['toggleNode']);

const flattenTree = (nodes, depth = 0) => {
  let result = [];
  for (const node of nodes) {
    // 1. 현재 노드를 결과에 추가합니다.
    result.push({ ...node, _depth: depth });

    // 2. 만약 노드가 열려있다면(isOpen), 자식들과 센서들을 렌더링합니다.
    if (node.isOpen) {
      // 2a. 자식 '폴더'가 있다면 재귀적으로 처리하여 결과에 추가합니다.
      if (node.children && node.children.length > 0) {
        result = result.concat(flattenTree(node.children, depth + 1));
      }
      // 2b. '센서'가 있다면 다음 depth로 결과에 추가합니다.
      if (node.sensors && node.sensors.length > 0) {
        node.sensors.forEach(sensor => {
            result.push({ ...sensor, _depth: depth + 1 });
        });
      }
    }
  }
  return result;
};

const flattenedNodes = computed(() => flattenTree(props.nodes));
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

