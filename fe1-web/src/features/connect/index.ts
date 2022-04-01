import * as functions from './functions';
import {
  ConnectConfiguration,
  ConnectInterface,
  CONNECT_FEATURE_IDENTIFIER,
} from './interface/Configuration';
import * as navigation from './navigation';

export const configure = (config: ConnectConfiguration): ConnectInterface => {
  return {
    identifier: CONNECT_FEATURE_IDENTIFIER,
    navigation,
    functions,
    context: {
      addLaoServerAddress: config.addLaoServerAddress,
      getLaoChannel: config.getLaoChannel,
      useCurrentLaoId: config.useCurrentLaoId,
    },
  };
};
