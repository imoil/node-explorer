import { defineStore } from 'pinia';
import { useRuntimeConfig } from '#app';

// Helper function to create a delay
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export const useTreeStore = defineStore('tree', () => {
  // --- STATE ---
  const nodes = ref<Map<string | null, any[]>>(new Map());
  const rootNodeIds = ref<string[]>([]);
  const openNodes = ref<Record<string, boolean>>({});

  // Search
  const searchQuery = ref('');
  const searchedQuery = ref('');
  const isSearching = ref(false);
  const searchStatus = ref('');
  const highlightedItemId = ref<string | null>(null);
  const isModalOpen = ref(false);
  const modalResults = ref<any[]>([]);
  const searchStatusOverlay = ref(false);
  const noResultsFound = ref(false);
  const singleResultFound = ref(false);
  let overlayCloseTimer: NodeJS.Timeout | null = null;

  // Refresh
  const isRefreshing = ref(false);

  // UI State
  const isSearchActive = ref(false);
  const lastFocusedIndex = ref(0);
  const virtualTreeRef = ref<any>(null);
  const scrollToNodeId = ref<string | null>(null);

  // --- GETTERS ---
  const getNode = (id: string) => {
    for (const children of nodes.value.values()) {
      const node = children.find(c => c.id === id);
      if (node) return node;
    }
    return null;
  };

  const getChildren = (parentId: string | null) => {
    return nodes.value.get(parentId) || [];
  };

  // --- ACTIONS ---

  // --- Tree Data Actions ---
  async function fetchNodes(parentId: string | null = null, force = false) {
    if (nodes.value.has(parentId) && !force) return;

    const config = useRuntimeConfig();
    const url = parentId
      ? `${config.public.apiBaseUrl}/api/nodes/${parentId}/children`
      : `${config.public.apiBaseUrl}/api/nodes/root`;
    
    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch nodes');
      const data = await response.json();
      
      nodes.value.set(parentId, data);
      if (parentId === null) {
        rootNodeIds.value = data.map(n => n.id);
      }
    } catch (error) {
      console.error(`Error fetching nodes for parent ${parentId}:`, error);
      nodes.value.set(parentId, []);
    }
  }

  async function revealPath(nodeId: string) {
    console.log(`[STORE] revealPath: Starting for nodeId: ${nodeId}`);
    const config = useRuntimeConfig();
    try {
      const response = await fetch(`${config.public.apiBaseUrl}/api/nodes/reveal-path/${nodeId}`);
      if (!response.ok) throw new Error('Failed to fetch reveal path');
      const pathDto = await response.json();
      console.log('[STORE] revealPath: Received DTO', JSON.parse(JSON.stringify(pathDto)));

      // Using `for...in` loop to ensure reactivity on object property changes
      for (const parentId in pathDto.childrenMap) {
        const key = parentId === 'null' ? null : parentId;
        nodes.value.set(key, pathDto.childrenMap[parentId]);
      }
      console.log('[STORE] revealPath: nodes map updated.');

      for (const nodeInPath of pathDto.path) {
        if (nodeInPath.hasChildren) {
          openNodes.value[nodeInPath.id] = true;
        }
      }
      console.log('[STORE] revealPath: openNodes updated.');

      // Signal the component to scroll
      scrollToNodeId.value = nodeId;
      highlightedItemId.value = nodeId;
      console.log(`[STORE] revealPath: Set scrollToNodeId to ${nodeId}`);

    } catch (error) {
      console.error('Reveal path error:', error);
    }
  }

  async function refreshExpandedNodes() {
    isRefreshing.value = true;
    searchStatus.value = 'Refreshing tree...';
    searchStatusOverlay.value = true;

    try {
      const openNodeIds = Object.keys(openNodes.value).filter(id => openNodes.value[id]);
      const refreshPromises = openNodeIds.map(id => fetchNodes(id, true));
      refreshPromises.push(fetchNodes(null, true));
      
      await Promise.all(refreshPromises);
      searchStatus.value = 'Tree refreshed successfully!';
    } catch (error) {
      console.error('Error refreshing nodes:', error);
      searchStatus.value = 'An error occurred during refresh.';
    } finally {
      isRefreshing.value = false;
      closeOverlayAfterDelay();
    }
  }

  // --- Search Actions ---
  function closeOverlayAfterDelay(delayMs = 1000) {
    if (overlayCloseTimer) clearTimeout(overlayCloseTimer);
    overlayCloseTimer = setTimeout(() => {
      searchStatusOverlay.value = false;
    }, delayMs);
  }

  async function handleSearch() {
    if (!searchQuery.value.trim()) return;

    if (overlayCloseTimer) clearTimeout(overlayCloseTimer);
    searchedQuery.value = searchQuery.value;
    isSearching.value = true;
    noResultsFound.value = false;
    singleResultFound.value = false;
    searchStatus.value = `Searching for "${searchedQuery.value}"...`;
    searchStatusOverlay.value = true;
    modalResults.value = [];

    try {
      const config = useRuntimeConfig();
      const response = await fetch(`${config.public.apiBaseUrl}/api/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: searchedQuery.value }),
      });

      if (!response.ok) throw new Error('Search request failed');
      const results = await response.json();

      if (results.length === 0) {
        searchStatus.value = `No results found for "${searchedQuery.value}".`;
        noResultsFound.value = true;
        closeOverlayAfterDelay();
      } else if (results.length === 1) {
        searchStatus.value = `Found 1 result. Revealing...`;
        await selectItem(results[0]);
      } else {
        modalResults.value = results;
        isModalOpen.value = true;
        searchStatusOverlay.value = false;
      }
    } catch (error) {
      console.error('Search error:', error);
      searchStatus.value = 'An error occurred during the search.';
      closeOverlayAfterDelay();
    } finally {
      isSearching.value = false;
    }
  }

  async function selectItem(item: { id: string; name: string }) {
    console.log('[APP] selectItem: Item selected from modal', item);
    isModalOpen.value = false;
    singleResultFound.value = true;
    searchStatus.value = `Revealing path for "${item.name}"...`;
    searchStatusOverlay.value = true;

    try {
      await revealPath(item.id);
      closeOverlayAfterDelay();
    } catch (error) {
      console.error('Reveal path error:', error);
      searchStatus.value = 'An error occurred during the search.';
      closeOverlayAfterDelay();
    }
  }
  
  watch(searchStatusOverlay, (isShowing) => {
    if (!isShowing) {
      if (overlayCloseTimer) clearTimeout(overlayCloseTimer);
      noResultsFound.value = false;
      singleResultFound.value = false;
      setTimeout(() => {
        highlightedItemId.value = null;
      }, 300);
    }
  });

  return {
    // State
    nodes,
    rootNodeIds,
    openNodes,
    searchQuery,
    searchedQuery,
    isSearching,
    isRefreshing,
    searchStatus,
    highlightedItemId,
    isModalOpen,
    modalResults,
    searchStatusOverlay,
    noResultsFound,
    singleResultFound,
    isSearchActive,
    lastFocusedIndex,
    virtualTreeRef,
    scrollToNodeId,
    // Getters
    getNode,
    getChildren,
    // Actions
    fetchNodes,
    revealPath,
    refreshExpandedNodes,
    handleSearch,
    selectItem,
  };
});