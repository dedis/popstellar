/* istanbul ignore file */

import { KeyPairStore } from 'core/keypair';
import { getNetworkManager } from 'core/network';
import { Hash, Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';

import { Lao } from '../objects';
import { setCurrentLao } from '../reducer';

export const openLaoTestConnection = async () => {
  const nc = await getNetworkManager().connect('ws://127.0.0.1:9000/organizer/client');
  nc.setRpcHandler(() => {
    console.info('Using custom test rpc handler: does nothing');
  });

  const org = KeyPairStore.getPublicKey();
  const time = new Timestamp(1609455600);
  const sampleLao: Lao = new Lao({
    name: 'name de la Lao',
    id: new Hash('myLaoId'), // Hash.fromStringArray(org.toString(), time.toString(), 'name')
    creation: time,
    last_modified: time,
    organizer: org,
    witnesses: [],
  });

  dispatch(setCurrentLao(sampleLao.toState()));
  console.info('Stored test lao in storage : ', sampleLao);
};
