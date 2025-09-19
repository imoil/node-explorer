<template>
  <v-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    max-width="800"
    @keydown.esc="$emit('update:modelValue', false)"
  >
    <v-card ref="cardRef">
      <v-toolbar flat density="compact">
        <v-toolbar-title class="text-body-1">Search Results ({{ results.length }})</v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon @click="$emit('update:modelValue', false)">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-toolbar>
      <v-divider></v-divider>
      <v-list class="py-0">
        <v-list-item
          v-for="(item, index) in results"
          :key="item.id"
          :class="{ 'v-list-item--active': index === activeIndex }"
          @click="selectAndClose(item)"
          @mouseenter="activeIndex = index"
          lines="two"
        >
          <template v-slot:prepend>
            <v-icon>{{ item.type === 'folder' ? 'mdi-folder-outline' : 'mdi-access-point' }}</v-icon>
          </template>
          <v-list-item-title>{{ item.name }}</v-list-item-title>
          <v-list-item-subtitle class="text-caption text-grey-lighten-1">
            Path: {{ getPathString(item.path) }}
          </v-list-item-subtitle>
        </v-list-item>
      </v-list>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  modelValue: Boolean,
  results: {
    type: Array,
    required: true,
  },
});

const emit = defineEmits(['update:modelValue', 'selectItem']);

const activeIndex = ref(0);
const cardRef = ref(null);

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    activeIndex.value = 0;
  }
});

const getPathString = (path) => {
  return path.map(p => p.name).join(' / ');
};

const selectAndClose = (item) => {
  emit('selectItem', item);
  emit('update:modelValue', false);
};

const handleKeydown = (event) => {
  if (!props.modelValue) return;

  if (event.key === 'ArrowDown') {
    event.preventDefault();
    activeIndex.value = (activeIndex.value + 1) % props.results.length;
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    activeIndex.value = (activeIndex.value - 1 + props.results.length) % props.results.length;
  } else if (event.key === 'Enter') {
    event.preventDefault();
    if (props.results[activeIndex.value]) {
      selectAndClose(props.results[activeIndex.value]);
    }
  }
};

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

