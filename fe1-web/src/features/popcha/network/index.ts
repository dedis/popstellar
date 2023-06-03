import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { PopchaConfiguration } from '../interface';
import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

export const configureNetwork = (configuration: PopchaConfiguration) => {
  configuration.messageRegistry.add(
    ObjectType.POPCHA,
    ActionType.AUTH,
    // We cannot subscribe on this channel, so we should not receive any message
    () => {
      throw new Error(`Received unexpected message on popcha auth channel`);
    },
    PopchaAuthMsg.fromJson,
  );
};
