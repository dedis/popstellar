import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';

import { PopchaConfiguration } from '../interface';
import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

export const configureNetwork = (configuration: PopchaConfiguration) => {
  configuration.messageRegistry.add(
    ObjectType.POPCHA,
    ActionType.AUTH,
    // We cannot subscribe on this channel, so we should not receive any message
    (_: ProcessableMessage) => {
      console.log(`Received unprocessed popcha auth message${JSON.stringify(_)}`);
      return true;
    },
    PopchaAuthMsg.fromJson,
  );
};
