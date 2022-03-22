import { shuffleArray } from 'core/functions/Array';

import { SendingStrategy } from './ClientMultipleServerStrategy';
import { sendToFirstAcceptingServerStrategy } from './SendToFirstAcceptingServerStrategy';

export const sendToFirstAcceptingRandomServerStrategy: SendingStrategy = async (
  payload,
  connections,
) =>
  // we have to create a copy of the array as shuffleArray shuffles in-place
  sendToFirstAcceptingServerStrategy(payload, shuffleArray([...connections]));
