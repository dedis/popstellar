import dataAddChirp from 'protocol/query/method/message/data/dataAddChirp.json';
import dataAddReaction from 'protocol/query/method/message/data/dataAddReaction.json';
import dataCastVote from 'protocol/query/method/message/data/dataCastVote.json';
import dataCloseRollCall from 'protocol/query/method/message/data/dataCloseRollCall.json';
import dataCreateLao from 'protocol/query/method/message/data/dataCreateLao.json';
import dataCreateMeeting from 'protocol/query/method/message/data/dataCreateMeeting.json';
import dataCreateRollCall from 'protocol/query/method/message/data/dataCreateRollCall.json';
import dataDeleteChirp from 'protocol/query/method/message/data/dataDeleteChirp.json';
import dataEndElection from 'protocol/query/method/message/data/dataEndElection.json';
import dataNotifyAddChirp from 'protocol/query/method/message/data/dataNotifyAddChirp.json';
import dataNotifyDeleteChirp from 'protocol/query/method/message/data/dataNotifyDeleteChirp.json';
import dataOpenElection from 'protocol/query/method/message/data/dataOpenElection.json';
import dataOpenRollCall from 'protocol/query/method/message/data/dataOpenRollCall.json';
import dataResultElection from 'protocol/query/method/message/data/dataResultElection.json';
import dataSetupElection from 'protocol/query/method/message/data/dataSetupElection.json';
import dataStateLao from 'protocol/query/method/message/data/dataStateLao.json';
import dataStateMeeting from 'protocol/query/method/message/data/dataStateMeeting.json';
import dataUpdateLao from 'protocol/query/method/message/data/dataUpdateLao.json';
import dataWitnessMessage from 'protocol/query/method/message/data/dataWitnessMessage.json';

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
