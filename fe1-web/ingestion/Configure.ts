import { getNetworkManager } from 'network';
import { handleRpcRequests } from './Handler';

export function configureRpcHandler() {
  getNetworkManager().setRpcHandler(handleRpcRequests);
}
