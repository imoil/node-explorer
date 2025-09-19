<template>
  <v-app theme="dark">
    <v-main>
      <v-container>
        <div class="max-w-4xl mx-auto">
          <TheHeader />

          <form @submit.prevent="handleSearch" class="mb-6">
            <v-text-field
              ref="searchInputRef"
              v-model="searchQuery"
              variant="solo"
              prepend-inner-icon="mdi-magnify"
              hide-details
              :loading="isSearching"
              :disabled="isSearching"
              @focus="onSearchFocus"
              @blur="handleSearchBlur"
              @keydown.esc="deactivateSearchInput"
              clearable
            >
              <template v-slot:label>
                Search nodes or sensors
                <span class="kbd-shortcut">f</span>
              </template>
            </v-text-field>
          </form>

          <v-card flat border>
            <v-toolbar flat density="compact">
               <v-toolbar-title class="text-body-2 d-flex align-center">
                 <span>System Explorer</span>
                 <span class="kbd-shortcut">t</span>
               </v-toolbar-title>
               <v-spacer></v-spacer>
               <div class="d-flex align-center text-body-2 mr-4">
                 <v-icon :color="wsStatus === 'Connected' ? 'green' : 'red'" icon="mdi-circle" size="x-small" class="mr-2"></v-icon>
                 WebSocket: {{ wsStatus }}
               </div>
            </v-toolbar>
            <v-divider></v-divider>
            
            <v-card-text class="pa-0" style="height: 70vh; overflow-y: auto;">
              <VirtualTree
                ref="virtualTreeRef"
                :highlight-id="highlightedItemId"
                :is-search-active="isSearchActive"
                @sensor-selected="handleSensorSelect"
              />
            </v-card-text>

            <v-overlay v-model="searchStatusOverlay" scrim="#000" class="d-flex align-center justify-center">
               <div class="d-flex flex-column align-center text-center pa-4 bg-grey-darken-4 rounded-lg pa-8">
                 <div v-if="isSearching">
                   <v-progress-circular indeterminate size="48" class="mb-4"></v-progress-circular>
                   <p>{{ searchStatus }}</p>
                 </div>
                 <div v-else-if="noResultsFound">
                    <v-icon size="64" class="mb-4" color="grey-lighten-1">mdi-magnify-close</v-icon>
                    <p class="text-h6">{{ searchStatus }}</p>
                 </div>
                 <div v-else-if="singleResultFound">
                    <v-icon size="64" class="mb-4" color="success">mdi-check-circle-outline</v-icon>
                    <p class="text-h6">{{ searchStatus }}</p>
                 </div>
                 <p v-else>{{ searchStatus }}</p>
               </div>
            </v-overlay>

            <v-overlay v-model="isSensorMessageVisible" scrim="#000" class="d-flex align-center justify-center">
              <div class="d-flex flex-column align-center text-center pa-4 bg-grey-darken-4 rounded-lg pa-8">
                <v-icon size="64" class="mb-4" color="info">mdi-information-outline</v-icon>
                <p class="text-h6">{{ sensorMessage }}</p>
              </div>
            </v-overlay>

          </v-card>
        </div>
      </v-container>
    </v-main>

    <SearchResultsModal
      v-model="isModalOpen"
      :results="modalResults"
      :search-query="searchedQuery"
      @select-item="selectItem"
      @cancel="restoreTreeFocus"
    />
  </v-app>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import TheHeader from '~/components/TheHeader.vue';
import VirtualTree from '~/components/VirtualTree.vue';
import SearchResultsModal from '~/components/SearchResultsModal.vue';
import { useWebSocket } from '~/composables/useWebSocket';
import { useTreeSearch } from '~/composables/useTreeSearch';

const MESSAGE_DISPLAY_TIME = 1000;

const virtualTreeRef = ref(null);
const searchInputRef = ref(null);
const lastFocusedIndex = ref(0);
const isSearchActive = ref(false);

const isSensorMessageVisible = ref(false);
const sensorMessage = ref('');
let sensorMessageTimer = null;

// --- Search ---
const {
  searchQuery, searchedQuery, isSearching, searchStatus, highlightedItemId,
  isModalOpen, modalResults, searchStatusOverlay, noResultsFound,
  singleResultFound, handleSearch, selectItem,
} = useTreeSearch(virtualTreeRef);

// --- Focus Management ---
const onSearchFocus = () => {
  isSearchActive.value = true;
  if (virtualTreeRef.value) {
    lastFocusedIndex.value = virtualTreeRef.value.getFocusedIndex();
  }
};

const handleSearchBlur = () => {
  if (searchStatusOverlay.value || isModalOpen.value) return;
  isSearchActive.value = false;
};

const deactivateSearchInput = () => {
  searchInputRef.value?.$el.querySelector('input')?.blur();
};

const restoreTreeFocus = () => {
  isSearchActive.value = false;
  if (virtualTreeRef.value) {
    virtualTreeRef.value.setFocusByIndex(lastFocusedIndex.value);
  }
};

watch(searchStatusOverlay, (isShowing, wasShowing) => {
  if (wasShowing && !isShowing && noResultsFound.value) {
    restoreTreeFocus();
  }
});

// --- WebSocket ---
const { wsStatus } = useWebSocket((payload) => {
  if (virtualTreeRef.value) {
    payload.forEach(update => {
      virtualTreeRef.value.updateNodeName(update);
    });
  }
});

// --- Sensor Selection ---
const handleSensorSelect = (sensorNode) => {
  sensorMessage.value = `Sensor '${sensorNode.name}' selected.`;
  isSensorMessageVisible.value = true;
  clearTimeout(sensorMessageTimer);
  sensorMessageTimer = setTimeout(() => {
    isSensorMessageVisible.value = false;
  }, MESSAGE_DISPLAY_TIME);
};

// --- Global Hotkeys ---
const handleGlobalKeydown = (event: KeyboardEvent) => {
  const target = event.target as HTMLElement;
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) {
    return;
  }

  if (event.key === 'f') {
    event.preventDefault();
    searchInputRef.value?.$el.querySelector('input')?.focus();
  }

  if (event.key === 't') {
    event.preventDefault();
    if (virtualTreeRef.value) {
      isSearchActive.value = false;
      // 부모는 자식에게 포커스하라고 명령만 내리면 됩니다.
      // 자식이 자신의 현재 포커스 위치를 알고 있으므로 그 위치에 다시 포커스합니다.
      virtualTreeRef.value.focus();
    }
  }
};

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});

</script>

<style>
.vue-recycle-scroller__item-wrapper, .vue-recycle-scroller__item-view {
  box-sizing: border-box;
}

.kbd-shortcut {
  background-color: #4f4f4f;
  border: 1px solid #666;
  border-bottom-width: 2px;
  border-radius: 4px;
  padding: 2px 6px;
  font-family: monospace;
  font-size: 0.8em;
  font-weight: bold;
  color: #e0e0e0;
  box-shadow: 0px 1px 1px #222;
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
</style>