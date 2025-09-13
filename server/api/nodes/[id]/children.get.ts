/**
 * @description 특정 ID를 가진 노드의 자식 노드 목록을 반환합니다.
 */
import { mockDatabase } from '~/server/utils/mockDatabase';

// 재귀적으로 특정 ID의 노드를 찾는 함수
const findNodeById = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node;
    if (node.children) {
      const found = findNodeById(node.children, id);
      if (found) return found;
    }
  }
  return null;
};

export default defineEventHandler(async (event) => {
  const nodeId = event.context.params?.id;

  if (!nodeId) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Node ID is required',
    });
  }

  // 네트워크 지연 시뮬레이션
  await new Promise(resolve => setTimeout(resolve, 800));

  const parentNode = findNodeById(mockDatabase, nodeId);

  if (!parentNode || !parentNode.children) {
    return [];
  }

  // 자식 노드 목록을 API 응답 형식에 맞게 변환
  const childNodes = parentNode.children.map(node => ({
    id: node.id,
    name: node.name,
    type: node.type,
    hasChildren: node.children && node.children.length > 0,
    metadata: node.metadata,
    sensors: node.sensors?.map(sensor => ({
        id: sensor.id,
        name: sensor.name,
        type: sensor.type,
        metadata: sensor.metadata,
    })) || [],
  }));

  return childNodes;
});

