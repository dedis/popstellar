// disable import/order here as it makes it easier to spot missing imports
/* eslint-disable import/order */
import dataCreateLao from 'protocol/query/method/message/data/dataCreateLao.json';
import dataStateLao from 'protocol/query/method/message/data/dataStateLao.json';
import dataGreetLao from 'protocol/query/method/message/data/dataGreetLao.json';
import dataUpdateLao from 'protocol/query/method/message/data/dataUpdateLao.json';

import dataCreateMeeting from 'protocol/query/method/message/data/dataCreateMeeting.json';
import dataStateMeeting from 'protocol/query/method/message/data/dataStateMeeting.json';

import dataCreateRollCall from 'protocol/query/method/message/data/dataCreateRollCall.json';
import dataOpenRollCall from 'protocol/query/method/message/data/dataOpenRollCall.json';
import dataCloseRollCall from 'protocol/query/method/message/data/dataCloseRollCall.json';

import dataWitnessMessage from 'protocol/query/method/message/data/dataWitnessMessage.json';

import dataKeyElection from 'protocol/query/method/message/data/dataKeyElection.json';
import dataSetupElection from 'protocol/query/method/message/data/dataSetupElection.json';
import dataOpenElection from 'protocol/query/method/message/data/dataOpenElection.json';
import dataCastVote from 'protocol/query/method/message/data/dataCastVote.json';
import dataEndElection from 'protocol/query/method/message/data/dataEndElection.json';
import dataResultElection from 'protocol/query/method/message/data/dataResultElection.json';

import dataAddChirp from 'protocol/query/method/message/data/dataAddChirp.json';
import dataNotifyAddChirp from 'protocol/query/method/message/data/dataNotifyAddChirp.json';
import dataDeleteChirp from 'protocol/query/method/message/data/dataDeleteChirp.json';
import dataNotifyDeleteChirp from 'protocol/query/method/message/data/dataNotifyDeleteChirp.json';

import dataAddReaction from 'protocol/query/method/message/data/dataAddReaction.json';
/* eslint-enable import/order */

const dataSchemas = [
  dataCreateLao,
  dataStateLao,
  dataUpdateLao,
  dataGreetLao,

  dataCreateMeeting,
  dataStateMeeting,

  dataCreateRollCall,
  dataOpenRollCall,
  dataCloseRollCall,

  dataKeyElection,
  dataSetupElection,
  dataOpenElection,
  dataCastVote,
  dataEndElection,
  dataResultElection,

  dataWitnessMessage,

  dataAddChirp,
  dataNotifyAddChirp,
  dataDeleteChirp,
  dataNotifyDeleteChirp,

  dataAddReaction,
];

export default dataSchemas;
