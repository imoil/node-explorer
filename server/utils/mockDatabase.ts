/**
 * @description 애플리케이션의 데이터 소스를 시뮬레이션하는 Mock 데이터입니다.
 * 실제 환경에서는 이 데이터가 Oracle DB에서 비롯됩니다.
 */
export const mockDatabase = [
  {
    id: 'node-1',
    name: 'Manufacturing EMEA',
    type: 'folder',
    hasChildren: true,
    metadata: { region: 'EMEA', owner: 'Alice', created: '2023-01-15' },
    sensors: [
        { id: 'sensor-1', name: 'Main Power Grid Sensor', type: 'sensor', metadata: { status: 'online', voltage: '240V' } },
        { id: 'sensor-2', name: 'Water Pressure Monitor', type: 'sensor', metadata: { status: 'warning', pressure: '2.5bar' } }
    ],
    children: [
      {
        id: 'node-1-1',
        name: 'Factory Germany',
        type: 'folder',
        hasChildren: true,
        metadata: { country: 'Germany', manager: 'Hans' },
        sensors: [
            { id: 'sensor-1-1', name: 'Assembly Line A Temp', type: 'sensor', metadata: { temp: '25C', humidity: '45%' } }
        ],
        children: [
          { id: 'node-1-1-1', name: 'Production Line Alpha', type: 'folder', hasChildren: false, metadata: { product: 'Widget A', shift: 'Day' }, sensors: [], children: [] },
          { id: 'node-1-1-2', name: 'Production Line Beta', type: 'folder', hasChildren: false, metadata: { product: 'Widget B', shift: 'Night' }, sensors: [], children: [] },
          { id: 'node-1-1-3', name: 'Quality Control Docs', type: 'file', hasChildren: false, metadata: { author: 'QC Dept', version: '2.1' }, sensors: [], children: [] },
        ],
      },
      {
        id: 'node-1-2',
        name: 'Factory France',
        type: 'folder',
        hasChildren: false,
        metadata: { country: 'France', manager: 'Pierre' },
        sensors: [],
        children: [],
      },
    ],
  },
  {
    id: 'node-2',
    name: 'Logistics APAC',
    type: 'folder',
    hasChildren: true,
    metadata: { region: 'APAC', owner: 'Bob', created: '2023-02-20' },
    sensors: [],
    children: [
        {
            id: 'node-2-1',
            name: 'Warehouse Korea',
            type: 'folder',
            hasChildren: true,
            metadata: { country: 'South Korea', stock: 'high' },
            sensors: [
                { id: 'sensor-2-1-1', name: 'Main Gate Access', type: 'sensor', metadata: { last_access: '2023-10-27 10:00' } }
            ],
            children: [
                 { id: 'node-2-1-1', name: 'Inventory-2023.csv', type: 'file', hasChildren: false, metadata: { size: '2.5MB' }, sensors: [], children: [] },
            ]
        }
    ],
  },
  {
    id: 'node-3',
    name: 'Finance NA',
    type: 'folder',
    hasChildren: false,
    metadata: { region: 'North America', owner: 'Charlie' },
    sensors: [],
    children: [],
  },
  {
    id: 'node-4',
    name: 'Empty Folder Test',
    type: 'folder',
    hasChildren: false,
    metadata: { purpose: 'testing' },
    sensors: [],
    children: [],
  }
];

