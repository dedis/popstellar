import { Channel, channelFromIds, Hash } from 'core/objects';

/**
 * Get a LAOs channel by its id
 * @param laoId The id of the lao whose channel should be returned
 * @returns The channel related to the passed lao id or undefined it the lao id is invalid
 */
export const getLaoChannel = (laoId: string): Channel | undefined => {
  try {
    return channelFromIds(new Hash(laoId));
  } catch (error) {
    console.error(`Cannot connect to LAO '${laoId}' as it is an invalid LAO ID`, error);
  }

  return undefined;
};
