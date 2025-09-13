/**
 * @description 메타데이터 및 이름으로 노드와 센서를 검색하는 API 엔드포인트입니다.
 * 결과에는 해당 항목까지의 전체 경로(노드 객체의 배열)가 포함됩니다.
 */
import { mockDatabase } from '~/server/utils/mockDatabase';

interface SearchResult {
  path: any[]; // 경로를 구성하는 노드 객체들의 배열
  item: any;   // 실제 검색된 노드 또는 센서 객체
}

// 재귀적으로 데이터베이스를 검색하는 함수
function searchRecursive(
  nodes: any[],
  query: string,
  currentPath: any[],
  results: SearchResult[]
): void {
  const lowerCaseQuery = query.toLowerCase();

  for (const node of nodes) {
    // 현재 노드를 포함한 새로운 경로 생성 (API 응답에 필요한 최소 정보만 포함)
    const nodePathInfo = { id: node.id, name: node.name, type: node.type };
    const newPath = [...currentPath, nodePathInfo];

    // 노드 이름 및 메타데이터 검색
    let isNodeMatch = node.name.toLowerCase().includes(lowerCaseQuery);
    if (!isNodeMatch && node.metadata) {
      isNodeMatch = Object.values(node.metadata).some(val =>
        String(val).toLowerCase().includes(lowerCaseQuery)
      );
    }
    if (isNodeMatch) {
      results.push({ path: newPath, item: node });
    }

    // 센서 검색
    if (node.sensors && node.sensors.length > 0) {
      for (const sensor of node.sensors) {
        let isSensorMatch = sensor.name.toLowerCase().includes(lowerCaseQuery);
        if (!isSensorMatch && sensor.metadata) {
          isSensorMatch = Object.values(sensor.metadata).some(val =>
            String(val).toLowerCase().includes(lowerCaseQuery)
          );
        }
        if (isSensorMatch) {
          // 센서가 일치하면, 부모 노드까지의 경로와 센서 아이템을 결과에 추가
          results.push({ path: newPath, item: sensor });
        }
      }
    }

    // 자식 노드 재귀 검색
    if (node.children && node.children.length > 0) {
      searchRecursive(node.children, query, newPath, results);
    }
  }
}

export default defineEventHandler(async (event) => {
  const body = await readBody(event);
  const query = body.query;

  if (!query) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Search query is required',
    });
  }

  // 검색 로직 시뮬레이션을 위한 지연
  await new Promise(resolve => setTimeout(resolve, 700));

  const searchResults: SearchResult[] = [];
  // 최상위에서 검색 시작
  searchRecursive(mockDatabase, query, [], searchResults);

  return searchResults;
});

