import { ConnectToLao } from '../objects';

/**
 * Given the lao server address and the lao id, this computes the data
 * that is encoded in a QR code that can be used to connect to a LAO
 * @returns Encoded LAO connection
 */
export const encodeLaoConnectionForQRCode = (servers: string[], laoId: string): string =>
  new ConnectToLao({ servers, lao: laoId }).toJson();
