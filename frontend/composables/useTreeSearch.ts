import { ref } from 'vue';
import { useRuntimeConfig } from '#app';

export function useTreeSearch(treeData, onToggleNode, virtualTreeRef, onRevealPath) {
  const searchQuery = ref('');
  const isSearching = ref(false);
  const searchStatus = ref('');
  const highlightedItemId = ref(null);
  const isModalOpen = ref(false);
  const modalResults = ref([]);
  const searchStatusOverlay = ref(false);

  const selectItem = async (item) => {
    isModalOpen.value = false;
    searchStatus.value = `Revealing path for "${item.name}"...`;
    searchStatusOverlay.value = true;

    try {
      const response = await fetch(`${useRuntimeConfig().public.apiBaseUrl}/api/reveal-path/${item.id}`);
      if (!response.ok) throw new Error('Reveal path request failed');
      const revealData = await response.json();

      await onRevealPath(revealData);

      highlightedItemId.value = item.id;

      setTimeout(() => {
        if (virtualTreeRef.value) {
          virtualTreeRef.value.scrollToNode(item.id);
        }
      }, 100);

    } catch (error) {
      console.error('Reveal path error:', error);
      searchStatus.value = 'An error occurred while revealing the path.';
    } finally {
        setTimeout(() => {
            highlightedItemId.value = null;
            searchStatusOverlay.value = false;
        }, 2500);
    }
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

