// fake LAOs to show how the app works
import { Hash } from 'model/objects/Hash';
import { Lao } from 'model/objects/Lao';
import { PublicKey } from 'model/objects/PublicKey';
import { Timestamp } from 'model/objects/Timestamp';

const TIMESTAMP = new Timestamp(1607616483);
const ORGANIZER = new PublicKey('DEADBEEF');
const WITNESSES = [new PublicKey('DEADC0DE'), new PublicKey('DEADBEA7')];

const laoData: Lao[] = [
  new Lao({
    id: Hash.fromString('31'),
    name: 'LAO 1',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('32'),
    name: 'LAO 2',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: new PublicKey('i7NMjdW2tkBj4uMCM15rlckmQCdR0anffOTD+MQg3XQ='),
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('33'),
    name: 'LAO 3',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: [
      new PublicKey('i7NMjdW2tkBj4uMCM15rlckmQCdR0anffOTD+MQg3XQ='),
      new PublicKey('DEADBEA7'),
    ],
  }),
  new Lao({
    id: Hash.fromString('34'),
    name: 'LAO 4',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('35'),
    name: 'LAO 5',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('36'),
    name: 'LAO 6',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('37'),
    name: 'LAO 7',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('38'),
    name: 'LAO 8',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('39'),
    name: 'LAO 9',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('310'),
    name: 'LAO 10',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('311'),
    name: 'LAO 11',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('312'),
    name: 'LAO 12',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('313'),
    name: 'LAO 13',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('314'),
    name: 'LAO 14',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('315'),
    name: 'LAO 15',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('316'),
    name: 'LAO 16',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('317'),
    name: 'LAO 17',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('318'),
    name: 'LAO 18',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('319'),
    name: 'LAO 19',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
  new Lao({
    id: Hash.fromString('320'),
    name: 'LAO 20',
    creation: TIMESTAMP,
    last_modified: TIMESTAMP,
    organizer: ORGANIZER,
    witnesses: WITNESSES,
  }),
];

export default laoData;
