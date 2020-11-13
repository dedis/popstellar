const data = [
  {
    title: '',
    data: [
      {
        id: 21,
        type: 'meeting',
        name: 'Property 1',
        date: '2020-10-28',
      },
      {
        id: 22,
        type: 'meeting',
        name: 'Property 2',
        date: '2020-10-28',
      },
      {
        id: 23,
        type: 'meeting',
        name: 'Property 3',
        date: '2020-10-28',
      },
      {
        id: 24,
        type: 'meeting',
        name: 'Property 4',
        date: '2021-10-28',
      },
    ],
  },
  {
    title: 'Past',
    data: [
      {
        id: 1,
        type: 'meeting',
        name: 'Events 1',
        date: '2020-09-28',
      },
      {
        id: 2,
        type: 'meeting',
        name: 'Events 2',
        date: '2020-10-28',
      },
      {
        id: 3,
        type: 'meeting',
        name: 'Events 3',
        date: '2020-10-28',
        childrens: [
          {
            id: 4,
            type: 'rollCall',
            name: 'Events 4',
            date: '2020-10-28',
            childrens: [
              {
                id: 5,
                type: 'discussion',
                name: 'Events 5',
                date: '2020-10-28',
              },
            ],
          },
          {
            id: 6,
            type: 'poll',
            name: 'Events 6',
            date: '2020-10-28',
          },
        ],
      },
      {
        id: 7,
        type: 'meeting',
        name: 'Events 7',
        date: '2020-10-28',
      },
      {
        id: 8,
        type: 'meeting',
        name: 'Events 8',
        date: '2020-10-28',
      },
      {
        id: 9,
        type: 'meeting',
        name: 'Events 9',
        date: '2020-10-28',
      },
    ],
  },
  {
    title: 'Present',
    data: [
      {
        id: 10,
        type: 'meeting',
        name: 'Events 10',
        date: '2020-10-28',
      },
      {
        id: 11,
        type: 'meeting',
        name: 'Events 11',
        date: '2020-10-28',
      },
      {
        id: 12,
        type: 'meeting',
        name: 'Events 12',
        date: '2020-10-28',
      },
      {
        id: 13,
        type: 'meeting',
        name: 'Events 13',
        date: '2020-10-28',
      },
      {
        id: 14,
        type: 'meeting',
        name: 'Events 14',
        date: '2020-10-28',
      },
      {
        id: 15,
        type: 'meeting',
        name: 'Events 15',
        date: '2020-10-28',
      },
      {
        id: 16,
        type: 'meeting',
        name: 'Events 16',
        date: '2020-10-28',
      },
    ],
  },
  {
    title: 'Future',
    data: [
      {
        id: 17,
        type: 'meeting',
        name: 'Events 17',
        date: '2020-10-28',
      },
      {
        id: 18,
        type: 'meeting',
        name: 'Events 18',
        date: '2020-10-28',
      },
      {
        id: 19,
        type: 'meeting',
        name: 'Events 19',
        date: '2020-10-28',
      },
      {
        id: 20,
        type: 'meeting',
        name: 'Events 20',
        date: '2021-10-28',
      },
    ],
  },
];

export default data;
