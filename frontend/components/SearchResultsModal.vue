<template>
  <v-dialog v-model="dialog" max-width="600px" @keydown="handleKeydown">
    <v-card ref="cardRef" tabindex="-1">
      <v-card-title class="d-flex justify-space-between align-center">
        <span>Search Results for "{{ searchQuery }}"</span>
        <v-chip color="primary" size="small" label>{{ results.length }} items found</v-chip>
      </v-card-title>
      <v-divider></v-divider>

      <v-list class="py-0" ref="listRef">
        <v-list-item
          v-for="(item, index) in results"
          :key="item.id"
          @click="selectItem(item)"
          :active="index === highlightedIndex"
          lines="two"
        >
          <template v-slot:prepend>
            <v-icon :icon="item.type === 'folder' ? 'mdi-folder-outline' : 'mdi-access-point'"></v-icon>
          </template>

          <v-list-item-title>{{ item.name }}</v-list-item-title>
          <v-list-item-subtitle class="path-text">
            {{ item.path.map(p => p.name).join(' / ') }}
          </v-list-item-subtitle>
        </v-list-item>
      </v-list>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn text @click="dialog = false">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue';

const props = defineProps({
  modelValue: Boolean,
  results: {
    type: Array,
    default: () => [],
  },
  searchQuery: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:modelValue', 'selectItem', 'cancel']);

const cardRef = ref(null);
const highlightedIndex = ref(0);
let itemWasSelected = false;

const dialog = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

const selectItem = (item) => {
  itemWasSelected = true;
  emit('selectItem', item);
  dialog.value = false;
};

const handleKeydown = (event) => {
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    if (highlightedIndex.value > 0) {
      highlightedIndex.value--;
    }
  } else if (event.key === 'ArrowDown') {
    event.preventDefault();
    if (highlightedIndex.value < props.results.length - 1) {
      highlightedIndex.value++;
    }
  } else if (event.key === 'Enter') {
    event.preventDefault();
    const selected = props.results[highlightedIndex.value];
    if (selected) {
      selectItem(selected);
    }
  }
};

// 모달이 열릴 때 상태를 초기화하고 카드에 포커스를 줍니다.
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    highlightedIndex.value = 0;
    itemWasSelected = false;
    nextTick(() => {
      // 카드가 포커스를 받아야 키보드 이벤트를 감지할 수 있습니다.
      cardRef.value?.$el.focus();
    });
  }
});

// 모달이 닫힐 때, 아이템 선택으로 닫힌게 아니라면 cancel 이벤트를 발생시킵니다.
watch(dialog, (isOpen, wasOpen) => {
    if (wasOpen && !isOpen && !itemWasSelected) {
        emit('cancel');
    }
});

</script>

<style scoped>
.path-text {
  font-size: 0.75rem;
  color: #a0a0a0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.v-card:focus {
  outline: none;
}
</style>