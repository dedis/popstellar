import { shuffleArray } from 'core/functions/Array';

import { SendingStrategy } from './ClientMultipleServerStrategy';
import { sendToFirstAcceptingServerStrategy } from './SendToFirstAcceptingServerStrategy';

export const sendToFirstAcceptingRandomServerStrategy: SendingStrategy = async (
  payload,
  connections,
) => sendToFirstAcceptingServerStrategy(payload, shuffleArray(connections));
