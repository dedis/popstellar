import { Hash } from 'core/objects';

import { ConnectToLao } from '../objects';

/**
 * Given a list of lao server addresses and the lao id, this computes the data
 * that is encoded in a QR code that can be used to connect to a LAO
 * @returns Encoded LAO connection
 */
export const encodeLaoConnectionForQRCode = (servers: string[], laoId: Hash): string =>
  new ConnectToLao({ servers, lao: laoId }).toJson();
