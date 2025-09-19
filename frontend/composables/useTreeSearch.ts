import { ref } from 'vue';

const API_BASE_URL = 'http://localhost:8080';

export function useTreeSearch(treeData, onToggleNode) {
  const searchQuery = ref('');
  const isSearching = ref(false);
  const searchStatus = ref('');
  const highlightedItemId = ref(null);
  const isModalOpen = ref(false);
  const modalResults = ref([]);
  const searchStatusOverlay = ref(false);

  const revealNodeInTree = async (path) => {
    let currentLevelNodes = treeData.value;
    for (const pathNode of path.slice(0, -1)) {
      let nodeInTree = currentLevelNodes.find(n => n.id === pathNode.id);
      if (nodeInTree) {
        if (!nodeInTree.isOpen) {
          await onToggleNode(nodeInTree);
          await new Promise(resolve => {
            const interval = setInterval(() => {
              if (!nodeInTree.isLoading) {
                clearInterval(interval);
                resolve();
              }
            }, 50);
          });
        }
        currentLevelNodes = nodeInTree.children;
      }
    }
  };

  const selectItem = async (item) => {
    isModalOpen.value = false;
    searchStatus.value = `Revealing path for "${item.name}"...`;
    searchStatusOverlay.value = true;
    
    await revealNodeInTree(item.path);

    highlightedItemId.value = item.id;
    setTimeout(() => {
        const scroller = document.querySelector('.scroller');
        const allNodes = Array.from(scroller.querySelectorAll('.tree-node'));
        const highlightedNode = allNodes.find(el => {
          // This is a bit of a hack since vue-virtual-scroller reuses DOM nodes.
          // We find the node that currently represents our highlighted item.
          const itemWrapper = el.parentElement;
          const itemIndex = parseInt(itemWrapper.getAttribute('data-index'), 10);
          const vm = scroller.__vue__;
          if (vm) {
            const flattenedNodes = vm.items; // Access internal items
            return flattenedNodes[itemIndex]?.id === item.id;
          }
          return false;
        });

        if (highlightedNode) {
          highlightedNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }, 500); // Wait a bit longer for DOM to be fully ready

    setTimeout(() => {
      highlightedItemId.value = null;
      searchStatusOverlay.value = false;
    }, 2500);
  };

  const handleSearch = async () => {
    if (!searchQuery.value.trim()) return;

    isSearching.value = true;
    searchStatus.value = `Searching for "${searchQuery.value}"...`;
    searchStatusOverlay.value = true;
    modalResults.value = [];

    try {
      const response = await fetch(`${API_BASE_URL}/api/search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ query: searchQuery.value }),
      });

      if (!response.ok) throw new Error('Search request failed');
      const results = await response.json();

      if (results.length === 0) {
        searchStatus.value = `No results found for "${searchQuery.value}".`;
        setTimeout(() => searchStatusOverlay.value = false, 2000);
      } else if (results.length === 1) {
        searchStatus.value = `Found 1 result.`;
        await selectItem(results[0]);
      } else {
        modalResults.value = results;
        isModalOpen.value = true;
        searchStatusOverlay.value = false;
      }
    } catch (error) {
      console.error('Search error:', error);
      searchStatus.value = 'An error occurred during the search.';
      setTimeout(() => searchStatusOverlay.value = false, 2000);
    } finally {
      isSearching.value = false;
    }
  };

  return {
    searchQuery,
    isSearching,
    searchStatus,
    highlightedItemId,
    isModalOpen,
    modalResults,
    searchStatusOverlay,
    handleSearch,
    selectItem,
  };
}

