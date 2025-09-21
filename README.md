# Node Explorer

현재 디렉트리의 코드를 분석하여 문제점이나 개선사항을 제안해 줘.

아래 예시 데이터의 구조대로 Example Data를 최상위 노드는 5개, 하위 노드는 각각의 상위 노드마다 최소 2개 이상, 센서는 노드 마다 최소 3개 이상 생성해 줘.
전체 노드는 최소 200개, 센서는 최소 1000개 수순으로 작성해 줘.
노드 이름과 센서 이름은 임의로 작성해 줘.

DB에서 Node Info 테이블에는 아래와 같은 구조로 되어 있어.

ID, NODE_PATH, NODE_NAME, PARENT_ID
101, ROOT1, ROOT1, 1
102, ROOT2, ROOT2, 1
103, ROOT3, ROOT3, 1
104, ROOT1|NODE1, NODE1, 101
105, ROOT1|NODE2, NODE2, 101
106, ROOT1|NODE3, NODE3, 101
107, ROOT1|NODE1|NODE1-1, NODE1-1, 104
108, ROOT1|NODE1|NODE1-2, NODE1-2, 104
109, ROOT1|NODE1|NODE2-1, NODE2-1, 105

DB의 Sensor Info 테이블에는 아래와 같은 구조로 되어 있어.

ID, SENSOR_NAME
101, SENSOR1
102, SENSOR2
103, SENSOR3
104, SENSOR4
105, SENSOR5
106, SENSOR6
107, SENSOR7
108, SENSOR8
109, SENSOR9
110, SENSOR10

그리고, Node 와 Sensor 를 연결하는 Map table 은 아래 구조로 되어 있어.

ID, NODE_ID, SENSOR_ID
101, 101, 101
102, 101, 102
103, 102, 103
104, 102, 104
105, 104, 105
106, 105, 106
107, 106, 107
108, 107, 108
109, 108, 109
110, 109, 110
