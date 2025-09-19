import { ref, nextTick, watch } from 'vue';
import { useRuntimeConfig } from '#app';

// virtualTreeRef를 인자로 받도록 수정
export function useTreeSearch(virtualTreeRef) {
  const searchQuery = ref('');
  const searchedQuery = ref('');

  const isSearching = ref(false);
  const searchStatus = ref('');
  const highlightedItemId = ref(null);
  const isModalOpen = ref(false);
  const modalResults = ref([]);
  const searchStatusOverlay = ref(false);
  const noResultsFound = ref(false);
  const singleResultFound = ref(false);

  const config = useRuntimeConfig();
  const API_BASE_URL = config.public.apiBaseUrl;
  const MESSAGE_DISPLAY_TIME = 1000;

  let overlayCloseTimer = null;

  const closeOverlayAfterDelay = (delay = MESSAGE_DISPLAY_TIME) => {
    clearTimeout(overlayCloseTimer);
    overlayCloseTimer = setTimeout(() => {
      searchStatusOverlay.value = false;
    }, delay);
  };

  const selectItem = async (item) => {
    isModalOpen.value = false;
    singleResultFound.value = true;
    searchStatus.value = `Revealing path for "${item.name}"...`;
    searchStatusOverlay.value = true;

    try {
      if (virtualTreeRef.value) {
        // VirtualTree의 revealPath 메소드를 직접 호출
        await virtualTreeRef.value.revealPath(item.id);
        highlightedItemId.value = item.id;
      }
      closeOverlayAfterDelay();

    } catch (error) {
      console.error('Reveal path error:', error);
      searchStatus.value = 'An error occurred during the search.';
      closeOverlayAfterDelay();
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.value.trim()) return;

    clearTimeout(overlayCloseTimer);
    searchedQuery.value = searchQuery.value;
    isSearching.value = true;
    noResultsFound.value = false;
    singleResultFound.value = false;
    searchStatus.value = `Searching for "${searchedQuery.value}"...`;
    searchStatusOverlay.value = true;
    modalResults.value = [];

    try {
      const response = await fetch(`${API_BASE_URL}/api/search`, {
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
  };

  watch(searchStatusOverlay, (isShowing) => {
    if (!isShowing) {
      clearTimeout(overlayCloseTimer);
      noResultsFound.value = false;
      singleResultFound.value = false;
      setTimeout(() => {
        highlightedItemId.value = null;
      }, 300);
    }
  });

  return {
    searchQuery,
    searchedQuery,
    isSearching,
    searchStatus,
    highlightedItemId,
    isModalOpen,
    modalResults,
    searchStatusOverlay,
    noResultsFound,
    singleResultFound,
    handleSearch,
    selectItem,
  };
}