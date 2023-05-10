import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';

import { PopchaConfiguration } from '../interface';
import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

export const configureNetwork = (configuration: PopchaConfiguration) => {
  configuration.messageRegistry.add(
    ObjectType.POPCHA,
    ActionType.AUTH,
    // TODO: for now we don't process such messages
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    (_: ProcessableMessage) => true,
    PopchaAuthMsg.fromJson,
  );
};
