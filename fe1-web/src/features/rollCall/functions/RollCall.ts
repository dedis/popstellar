import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { RollCall } from '../objects';
import { getRollCallById as getRollCallByIdFromStore } from '../reducer';

export const getRollCallById = (rollCallId: Hash | string): RollCall | undefined =>
  getRollCallByIdFromStore(rollCallId, getStore().getState());
