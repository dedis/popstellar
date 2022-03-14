import {
  ConnectConfiguration,
  ConnectInterface,
  CONNECT_FEATURE_IDENTIFIER,
} from './interface/Configuration';
import * as navigation from './navigation';
import * as functions from './functions';

export const configure = (config: ConnectConfiguration): ConnectInterface => {
  return {
    identifier: CONNECT_FEATURE_IDENTIFIER,
    navigation,
    functions,
    context: {
      setLaoServerAddress: config.setLaoServerAddress,
    },
  };
};
