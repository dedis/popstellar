import dataCreateLao from 'protocol/query/method/message/data/dataCreateLao.json';
import dataStateLao from 'protocol/query/method/message/data/dataStateLao.json';
import dataUpdateLao from 'protocol/query/method/message/data/dataUpdateLao.json';
import dataCreateMeeting from 'protocol/query/method/message/data/dataCreateMeeting.json';
import dataStateMeeting from 'protocol/query/method/message/data/dataStateMeeting.json';
import dataCreateRollCall from 'protocol/query/method/message/data/dataCreateRollCall.json';
import dataOpenRollCall from 'protocol/query/method/message/data/dataOpenRollCall.json';
import dataCloseRollCall from 'protocol/query/method/message/data/dataCloseRollCall.json';
import dataWitnessMessage from 'protocol/query/method/message/data/dataWitnessMessage.json';
import dataSetupElection from 'protocol/query/method/message/data/dataSetupElection.json';
import dataCastVote from 'protocol/query/method/message/data/dataCastVote.json';
import dataEndElection from 'protocol/query/method/message/data/dataEndElection.json';
import dataResultElection from 'protocol/query/method/message/data/dataResultElection.json';
import dataAddChirp from 'protocol/query/method/message/data/dataAddChirp.json';
import dataAddChirpBroadcast from 'protocol/query/method/message/data/dataAddChirpBroadcast.json';
import dataAddReaction from 'protocol/query/method/message/data/dataAddReaction.json';

const dataSchemas = [
  dataCreateLao,
  dataStateLao,
  dataUpdateLao,

  dataCreateMeeting,
  dataStateMeeting,

  dataCreateRollCall,
  dataOpenRollCall,
  dataCloseRollCall,

  dataSetupElection,
  dataCastVote,
  dataEndElection,
  dataResultElection,

  dataWitnessMessage,

  dataAddChirp,
  dataAddChirpBroadcast,

  dataAddReaction,
];

export default dataSchemas;
