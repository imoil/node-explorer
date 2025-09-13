/**
 * @description 최상위 노드 목록을 반환합니다.
 * 각 노드는 자식 노드를 가질 수 있는지(hasChildren) 여부를 포함합니다.
 */
import { mockDatabase } from '~/server/utils/mockDatabase';

export default defineEventHandler(async (event) => {
  // 네트워크 지연 시뮬레이션
  await new Promise(resolve => setTimeout(resolve, 500));

  // 최상위 노드만 필터링하여 반환
  const rootNodes = mockDatabase.map(node => ({
    id: node.id,
    name: node.name,
    type: node.type,
    hasChildren: node.children && node.children.length > 0,
    metadata: node.metadata,
    // 루트에서는 센서 목록도 함께 반환
    sensors: node.sensors?.map(sensor => ({
        id: sensor.id,
        name: sensor.name,
        type: sensor.type,
        metadata: sensor.metadata,
    })) || [],
  }));

  return rootNodes;
});

