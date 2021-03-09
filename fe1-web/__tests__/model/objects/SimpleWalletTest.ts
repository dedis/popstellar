import { Hash } from 'model/objects/Hash';
import { SimpleWalletObject } from 'model/objects/SimpleWalletObject';

const wallet = new SimpleWalletObject(Hash.fromString('wallet'));

/* create a LAO */
const laoIdHash: Hash = Hash.fromString('TESTING LAO');
wallet.addLao(laoIdHash);

/* create a Roll Call */
const rollCallId: Hash = Hash.fromString('rollCallId');
wallet.addTokenForRollCallAttendance(laoIdHash, rollCallId);

//const keyPair = wallet.findKeyPair(laoIdHash, rollCallId); 

