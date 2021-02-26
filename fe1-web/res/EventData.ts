// fake events to show how the app works
const data = [
  {
    title: 'Past',
    data: [
      {
        id: '1',
        object: 'meeting',
        action: 'state',
        name: 'Events 1',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616500,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '2',
        object: 'meeting',
        action: 'state',
        name: 'Events 2',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'i7NMjdW2tkBj4uMCM15rlckmQCdR0anffOTD+MQg3XQ=',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '3',
        object: 'meeting',
        action: 'state',
        name: 'Events 3',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['i7NMjdW2tkBj4uMCM15rlckmQCdR0anffOTD+MQg3XQ=', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
        children: [
          {
            id: '4',
            object: 'roll-call',
            action: 'create',
            name: 'Events 4',
            creation: 1607616483,
            last_modified: 1607616483,
            organizer: 'a organizer signature',
            witnesses: ['witness signature 1', 'witness signature 2'],
            modification_id: 'a modification id',
            modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
            children: [
              {
                id: '5',
                object: 'discussion',
                action: 'state',
                name: 'Events 5',
                creation: 1607616483,
                last_modified: 1607616483,
                organizer: 'a organizer signature',
                witnesses: ['witness signature 1', 'witness signature 2'],
                modification_id: 'a modification id',
                modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
              },
            ],
          },
          {
            id: '6',
            object: 'poll',
            action: 'state',
            name: 'Events 6',
            creation: 1607616483,
            last_modified: 1607616483,
            organizer: 'a organizer signature',
            witnesses: ['witness signature 1', 'witness signature 2'],
            modification_id: 'a modification id',
            modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
          },
        ],
      },
      {
        id: '7',
        object: 'meeting',
        action: 'state',
        name: 'Events 7',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '8',
        object: 'meeting',
        action: 'state',
        name: 'Events 8',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '9',
        object: 'meeting',
        action: 'state',
        name: 'Events 9',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
    ],
  },
  {
    title: 'Present',
    data: [
      {
        id: '10',
        object: 'meeting',
        action: 'state',
        name: 'Events 10',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '11',
        object: 'roll-call',
        action: 'create',
        name: 'Events 11',
        creation: 1607616483,
        last_modified: 1607616483,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '12',
        object: 'meeting',
        action: 'state',
        name: 'Events 12',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '13',
        object: 'meeting',
        action: 'state',
        name: 'Events 13',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '14',
        object: 'meeting',
        action: 'state',
        name: 'Events 14',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '15',
        object: 'meeting',
        action: 'state',
        name: 'Events 15',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '16',
        object: 'meeting',
        action: 'state',
        name: 'Events 16',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
    ],
  },
  {
    title: 'Future',
    data: [
      {
        id: '17',
        object: 'meeting',
        action: 'state',
        name: 'Events 17',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '18',
        object: 'meeting',
        action: 'state',
        name: 'Events 18',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '19',
        object: 'meeting',
        action: 'state',
        name: 'Events 19',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
      {
        id: '20',
        object: 'meeting',
        action: 'state',
        name: 'Events 20',
        creation: 1607616483,
        last_modified: 1607616483,
        location: 'A location, test.com',
        start: 1607616483,
        end: 1607618490,
        organizer: 'a organizer signature',
        witnesses: ['witness signature 1', 'witness signature 2'],
        modification_id: 'a modification id',
        modification_signatures: [{ witness: 'Witness 1', signature: 'witness signature 1' }, { witness: 'Witness 2', signature: 'witness signature 2' }],
      },
    ],
  },
];
export default data;
