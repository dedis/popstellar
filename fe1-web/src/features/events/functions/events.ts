import { Hash } from 'core/objects';
import { getStore } from 'core/redux';

import { selectEventById } from '../network/EventHandlerUtils';

export const getEventById = (id: Hash) => selectEventById(getStore().getState(), id);
