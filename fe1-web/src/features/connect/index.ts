import { ConnectInterface, CONNECT_FEATURE_IDENTIFIER } from './interface/Configuration';
import * as navigation from './navigation';
import * as functions from './functions';

export const configure = (): ConnectInterface => {
  return { identifier: CONNECT_FEATURE_IDENTIFIER, navigation, functions };
};
