<template>
  <div class="bg-gray-900 text-white min-h-screen font-sans p-4 sm:p-6 lg:p-8">
    <div class="max-w-4xl mx-auto">
      <header class="mb-8">
        <h1 class="text-3xl font-bold tracking-tight text-gray-100">Intelligent Tree Component</h1>
        <p class="text-gray-400 mt-2">
          Featuring lazy loading, real-time updates, and interactive search with auto-expand.
        </p>
      </header>
      
      <!-- Search Form -->
      <div class="mb-6">
        <form @submit.prevent="handleSearch" class="flex items-center gap-3 bg-gray-800 border border-gray-700 rounded-lg p-3">
          <svg class="w-5 h-5 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
          </svg>
          <input 
            type="text" 
            v-model="searchQuery"
            placeholder="Search and press Enter..."
            class="w-full bg-transparent focus:outline-none text-white placeholder-gray-500"
          />
          <button 
            type="submit"
            :disabled="isSearching"
            class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 rounded-md text-sm font-medium transition-colors disabled:bg-indigo-800 disabled:cursor-not-allowed flex items-center">
            <svg v-if="isSearching" class="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Search
          </button>
        </form>
      </div>

      <!-- Search Status Message -->
      <div v-if="searchStatus" class="mb-6 text-center text-gray-400 p-4 bg-gray-800 rounded-lg">{{ searchStatus }}</div>

      <!-- Tree View -->
      <div
        class="bg-gray-800 border border-gray-700 rounded-lg shadow-lg p-6 min-h-[500px] overflow-auto relative" id="tree-container">
        <!-- WebSocket Status -->
        <div class="absolute top-4 right-4 flex items-center space-x-2 text-sm">
          <span :class="['w-3 h-3 rounded-full', wsStatus === 'Connected' ? 'bg-green-500' : 'bg-red-500']"></span>
          <span class="text-gray-300">{{ wsStatus }}</span>
        </div>
        
        <div v-if="isLoadingRoot" class="flex items-center justify-center h-64">
           <svg class="animate-spin -ml-1 mr-3 h-8 w-8 text-gray-300" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span class="text-lg">Loading Root Nodes...</span>
        </div>
        <div v-else>
          <TreeNode v-for="node in treeData" :key="node.id" :node="node" :depth="0" />
        </div>
      </div>
    </div>

    <!-- Search Results Modal -->
    <div v-if="isModalOpen" 
         class="fixed inset-0 bg-black bg-opacity-70 z-50 flex items-center justify-center p-4"
         role="dialog"
         aria-modal="true"
         aria-labelledby="modal-title">
      <div class="bg-gray-800 border border-gray-700 rounded-lg shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col"
           @keydown.esc="closeModal">
        <header class="p-4 border-b border-gray-700 flex justify-between items-center">
          <h2 id="modal-title" class="text-xl font-semibold">Search Results ({{ modalResults.length }})</h2>
          <button @click="closeModal" class="text-gray-400 hover:text-white" aria-label="Close">
            <svg class="w-6 h-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
          </button>
        </header>
        <div class="overflow-y-auto p-4" ref="modalListRef" tabindex="-1" @keydown.prevent.down="moveSelection(1)" @keydown.prevent.up="moveSelection(-1)" @keydown.prevent.enter="selectActiveItem">
          <ul role="listbox" aria-labelledby="modal-title">
            <li v-for="(result, index) in modalResults" 
                :key="result.item.id + index"
                :id="`result-item-${index}`"
                role="option"
                :aria-selected="activeIndex === index"
                @click="selectItem(result)"
                @mouseenter="activeIndex = index"
                :class="['p-3 rounded-md cursor-pointer transition-colors mb-2', activeIndex === index ? 'bg-indigo-600' : 'bg-gray-700/50 hover:bg-gray-700']">
              <div class="flex items-center gap-3">
                <component :is="getIconForType(result.item.type)" />
                <div class="flex-1">
                  <p class="font-semibold text-white">{{ result.item.name }}</p>
                  <p class="text-xs text-gray-400 font-mono mt-1 break-all">{{ getPathString(result) }}</p>
                </div>
              </div>
            </li>
          </ul>
        </div>
        <footer class="p-3 text-xs text-gray-500 bg-gray-900/50 rounded-b-lg">
          Use <kbd class="font-sans bg-gray-600 px-1.5 py-0.5 rounded">↑</kbd> <kbd class="font-sans bg-gray-600 px-1.5 py-0.5 rounded">↓</kbd> to navigate, <kbd class="font-sans bg-gray-600 px-1.5 py-0.5 rounded">Enter</kbd> to select, <kbd class="font-sans bg-gray-600 px-1.5 py-0.5 rounded">Esc</kbd> to close.
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, h, defineComponent, nextTick } from 'vue';

// --- STATE MANAGEMENT ---
const treeData = ref([]);
const isLoadingRoot = ref(true);
const ws = ref(null);
const wsStatus = ref('Disconnected');
const searchQuery = ref('');
const isSearching = ref(false);
const searchStatus = ref('');

// Modal State
const isModalOpen = ref(false);
const modalResults = ref([]);
const activeIndex = ref(0);
const modalListRef = ref(null);

// --- API FETCHING ---
const fetchNodes = async (id = null) => {
  try {
    const url = id ? `/api/nodes/${id}/children` : '/api/nodes/root';
    const response = await fetch(url);
    if (!response.ok) throw new Error('Network response was not ok');
    const data = await response.json();
    return data.map(node => ({ ...node, children: [], sensors: node.sensors || [], isOpen: false, isLoading: false }));
  } catch (error) {
    console.error("Failed to fetch nodes:", error);
    return [];
  }
};

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return;
  
  isSearching.value = true;
  searchStatus.value = `Searching for "${searchQuery.value}"...`;
  isModalOpen.value = false;
  modalResults.value = [];

  try {
    const response = await fetch('/api/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: searchQuery.value })
    });
    if (!response.ok) throw new Error('Search request failed');
    
    const results = await response.json();

    if (results.length === 0) {
      searchStatus.value = `No results found for "${searchQuery.value}".`;
    } else if (results.length === 1) {
      searchStatus.value = 'One result found. Navigating...';
      await revealNodeInTree(results[0]);
      searchStatus.value = '';
    } else {
      modalResults.value = results;
      activeIndex.value = 0;
      isModalOpen.value = true;
      await nextTick();
      modalListRef.value?.focus();
      searchStatus.value = '';
    }

  } catch (error) {
    console.error("Search error:", error);
    searchStatus.value = "An error occurred during search.";
  } finally {
    isSearching.value = false;
  }
};

// --- MODAL & TREE NAVIGATION LOGIC ---
const closeModal = () => {
  isModalOpen.value = false;
};

const moveSelection = (step) => {
  activeIndex.value = (activeIndex.value + step + modalResults.value.length) % modalResults.value.length;
  document.getElementById(`result-item-${activeIndex.value}`)?.scrollIntoView({ block: 'nearest' });
};

const selectActiveItem = () => {
  if (modalResults.value[activeIndex.value]) {
    selectItem(modalResults.value[activeIndex.value]);
  }
};

const selectItem = async (result) => {
  closeModal();
  searchStatus.value = `Navigating to "${result.item.name}"...`;
  await revealNodeInTree(result);
  searchStatus.value = '';
};

const revealNodeInTree = async (result) => {
  let nodesInPath = result.path;
  const itemToReveal = result.item;

  // If the revealed item is a node, the path includes the node itself.
  // We only need to expand the parents, so we slice the last element off.
  if (itemToReveal.type === 'folder' || itemToReveal.type === 'file') {
      nodesInPath = result.path.slice(0, -1);
  }

  let currentChildrenRef = treeData;

  for (const pathNode of nodesInPath) {
      let treeNode = currentChildrenRef.value.find(n => n.id === pathNode.id);
      if (!treeNode) {
          console.error("Path node not found in UI tree", pathNode.id);
          searchStatus.value = "Error: Could not find node in the tree.";
          return;
      }

      if (treeNode.hasChildren && !treeNode.isOpen) {
          treeNode.isLoading = true;
          treeNode.isOpen = true;
          treeNode.children = await fetchNodes(treeNode.id);
          treeNode.isLoading = false;
      }
      currentChildrenRef = ref(treeNode.children);
  }
  
  await nextTick();

  const targetElement = document.querySelector(`[data-tree-id='${itemToReveal.id}']`);
  if (targetElement) {
      targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      targetElement.classList.add('highlight');
      setTimeout(() => {
          targetElement.classList.remove('highlight');
      }, 2000);
  } else {
      searchStatus.value = "Error: Could not find the element in the DOM to highlight.";
  }
};


// --- WEBSOCKET LOGIC ---
const setupWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const wsUrl = `${protocol}://${window.location.host}`;
  ws.value = new WebSocket(wsUrl);
  ws.value.onopen = () => { wsStatus.value = 'Connected'; };
  ws.value.onclose = () => { wsStatus.value = 'Disconnected'; };
  ws.value.onerror = () => { wsStatus.value = 'Error'; };
  ws.value.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data);
      if (message.type === 'NODE_UPDATES_BATCH') {
        console.log(`Received batch update with ${message.payload.length} items.`);
        message.payload.forEach(update => {
          updateNodeInTree(treeData.value, update.id, { name: update.newName });
        });
      }
    } catch (e) {
      console.error('Error parsing WebSocket message:', e);
    }
  };
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

// --- LIFECYCLE HOOKS ---
onMounted(async () => {
  isLoadingRoot.value = true;
  treeData.value = await fetchNodes();
  isLoadingRoot.value = false;
  setupWebSocket();
});

onUnmounted(() => {
  if (ws.value) ws.value.close();
});


// --- ICONS & HELPERS ---
const ICONS = {
  chevron: h('svg', { class: 'w-4 h-4 text-gray-400 shrink-0 transition-transform duration-200', xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('polyline', { points: '9 18 15 12 9 6' })]),
  folder: h('svg', { class: 'w-6 h-6 text-yellow-500 shrink-0', xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d: 'M4 20h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.93a2 2 0 0 1-1.66-.9l-.82-1.2A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13c0 1.1.9 2 2 2Z' })]),
  file: h('svg', { class: 'w-6 h-6 text-gray-400 shrink-0', xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z' }), h('polyline', { points: '14 2 14 8 20 8' })]),
  sensor: h('svg', { class: 'w-6 h-6 text-sky-400 shrink-0', xmlns: 'http://www.w3.org/2000/svg', width: '24', height: '24', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, [h('path', { d: 'M2 12.5a4.5 4.5 0 1 1 9 0 4.5 4.5 0 0 1-9 0Z' }), h('path', { d: 'M12 12.5a4.5 4.5 0 1 1 9 0 4.5 4.5 0 0 1-9 0Z' }), h('path', { d: 'M2 12.5h20' })]),
  loading: h('svg', { class: 'animate-spin h-5 w-5 text-gray-300', xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24' }, [h('circle', { class: 'opacity-25', cx: '12', cy: '12', r: '10', stroke: 'currentColor', 'stroke-width': '4' }), h('path', { class: 'opacity-75', fill: 'currentColor', d: 'M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z' })]),
};
const getIconForType = (type) => ICONS[type] || ICONS.file;
const getPathString = (result) => result.path.map(p => p.name).join(' > ');

// --- DYNAMIC TREE NODE COMPONENT ---
const TreeNode = defineComponent({
  name: 'TreeNode',
  props: { node: { type: Object, required: true }, depth: { type: Number, required: true } },
  setup(props) {
    const toggleNode = async () => {
      if (!props.node.hasChildren) return;
      if (props.node.isOpen) {
        props.node.isOpen = false;
      } else {
        props.node.isOpen = true;
        if (props.node.children.length === 0) {
          props.node.isLoading = true;
          props.node.children = await fetchNodes(props.node.id);
          props.node.isLoading = false;
        }
      }
    };

    const renderMetadataTooltip = (metadata) => {
      if (!metadata) return null;
      return h('div', {
        class: 'pointer-events-none absolute left-1/2 -translate-x-1/2 top-full mt-2 w-max max-w-xs z-10 opacity-0 group-hover:opacity-100 transition-opacity duration-200 bg-gray-900 border border-gray-600 text-white text-xs rounded-md p-2 shadow-lg',
        innerHTML: Object.entries(metadata)
          .map(([key, value]) => `<div class="flex justify-between gap-2"><span class="font-semibold text-gray-300">${key}:</span><span class="text-gray-400">${value}</span></div>`)
          .join('')
      });
    };
    
    return () => {
      const isFolder = props.node.type === 'folder';
      
      const nodeElement = h('div', { 'data-tree-id': props.node.id, class: 'relative group flex items-center p-1.5 rounded-md hover:bg-gray-700/60 cursor-pointer transition-colors duration-150', style: { paddingLeft: `${props.depth * 20}px` }, onClick: isFolder ? toggleNode : null }, [
          h('div', { class: 'flex items-center gap-2 w-full' }, [
            isFolder ? h('div', { class: ['transition-transform', props.node.isOpen ? 'rotate-90' : ''] }, ICONS.chevron) : h('div', { class: 'w-4 h-4' }),
            isFolder ? ICONS.folder : ICONS.file,
            h('span', { class: 'ml-2 flex-1 truncate' }, props.node.name),
            props.node.isLoading ? ICONS.loading : null,
          ]),
          renderMetadataTooltip(props.node.metadata)
        ]);

      const sensors = props.node.isOpen && props.node.sensors.map(sensor => 
        h('div', { 'data-tree-id': sensor.id, key: sensor.id, class: 'relative group flex items-center p-1.5 rounded-md hover:bg-gray-700/60', style: { paddingLeft: `${(props.depth + 1) * 20 + 4}px` } }, [ // 4px extra indent
          h('div', { class: 'flex items-center gap-2 w-full' }, [
            h('div', { class: 'w-4 h-4' }), // Indent spacer
            ICONS.sensor,
            h('span', { class: 'ml-2 flex-1 truncate' }, sensor.name),
          ]),
          renderMetadataTooltip(sensor.metadata)
        ])
      );

      const children = props.node.isOpen && props.node.children.map(child =>
        h(TreeNode, { key: child.id, node: child, depth: props.depth + 1 })
      );

      return h('div', [nodeElement, sensors, children]);
    };
  }
});
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&display=swap');
body { font-family: 'Inter', sans-serif; }
.highlight {
  background-color: #4f46e5; /* indigo-600 */
  transition: background-color 0.3s ease-in-out;
  animation: fadeOutHighlight 2s forwards;
}
@keyframes fadeOutHighlight {
  0% { background-color: #4f46e5; }
  70% { background-color: #4f46e5; }
  100% { background-color: transparent; }
}
</style>
