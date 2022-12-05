import { KeyPairStore } from 'core/keypair';
import { publish } from 'core/network';
import { Channel, channelFromIds, Hash, PublicKey, ROOT_CHANNEL, Timestamp } from 'core/objects';

import { Lao } from '../objects';
import { OpenedLaoStore } from '../store';
import { CreateLao, StateLao, UpdateLao } from './messages';

/**
 * Contains all functions to send lao related messages.
 */

/** Send a server query asking for the creation of a LAO with a given name (String) */
export async function requestCreateLao(laoName: string): Promise<Channel> {
  const time = Timestamp.EpochNow();
  const pubKey = KeyPairStore.getPublicKey();

  const message = new CreateLao({
    id: Hash.fromStringArray(pubKey, time.toString(), laoName),
    name: laoName,
    creation: time,
    organizer: pubKey,
    witnesses: [],
  });

  await publish(ROOT_CHANNEL, message);

  return channelFromIds(message.id);
}

/** Send a server query asking for a LAO update providing a new name (String) */
export function requestUpdateLao(name: string, witnesses?: PublicKey[]): Promise<void> {
  const time: Timestamp = Timestamp.EpochNow();
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new UpdateLao({
    id: Hash.fromStringArray(currentLao.organizer, currentLao.creation.toString(), name),
    name,
    last_modified: time,
    witnesses: witnesses === undefined ? currentLao.witnesses : witnesses,
  });

  return publish(channelFromIds(currentLao.id), message);
}

/** Send a server query asking for the current state of a LAO */
export function requestStateLao(): Promise<void> {
  const currentLao: Lao = OpenedLaoStore.get();

  const message = new StateLao({
    id: Hash.fromStringArray(currentLao.organizer, currentLao.creation.toString(), currentLao.name),
    name: currentLao.name,
    creation: currentLao.creation,
    last_modified: Timestamp.EpochNow(),
    organizer: currentLao.organizer,
    witnesses: currentLao.witnesses,
    modification_id: Hash.fromStringArray(), // FIXME need modification_id from storage
    modification_signatures: [], // FIXME need modification_signatures from storage
  });

  return publish(channelFromIds(currentLao.id), message);
}
