const Ajv = require("ajv");
const main_schema = require("../jsonRPC.json");
const answer_schema = require("../answer/answer.json");
const error_schema = require("../answer/error.json");

const query_schema = require("../query/query.json");
const query_message_schema = require("../query/method/message/message.json");

const method_subscribe_schema = require("../query/method/subscribe.json");
const method_unsubscribe_schema = require("../query/method/unsubscribe.json");
const method_broadcast_schema = require("../query/method/broadcast.json");
const method_publish_schema = require("../query/method/publish.json");
const method_catchup_schema = require("../query/method/catchup.json");

const message_data_schema = require("../query/method/message/data/data.json")
const message_data_roll_call_close_schema = require("../query/method/message/data/dataCloseRollCall.json")
const message_data_roll_call_create_schema = require("../query/method/message/data/dataCreateRollCall.json")
const message_data_roll_call_open_schema = require("../query/method/message/data/dataOpenRollCall.json")
const message_data_lao_create_schema = require("../query/method/message/data/dataCreateLao.json")
const message_data_lao_update_schema = require("../query/method/message/data/dataUpdateLao.json")
const message_data_lao_state_schema = require("../query/method/message/data/dataStateLao.json")
const message_data_vote_cast_schema = require("../query/method/message/data/dataCastVote.json")
const message_data_election_end_schema = require("../query/method/message/data/dataEndElection.json")
const message_data_election_result_schema = require("../query/method/message/data/dataResultElection.json")
const message_data_election_setup_schema = require("../query/method/message/data/dataSetupElection.json")
const message_data_meeting_create_schema = require("../query/method/message/data/dataCreateMeeting.json")
const message_data_meeting_state_schema = require("../query/method/message/data/dataStateMeeting.json")
const message_data_message_witness_schema = require("../query/method/message/data/dataWitnessMessage.json")

const message_data_chirp_add_schema = require("../query/method/message/data/dataAddChirp.json")
const message_data_chirp_add_broadcast_schema = require("../query/method/message/data/dataAddChirpBroadcast.json")

const ajv = new Ajv({ allErrors: true, strict: false });

ajv.addSchema([
    main_schema,
    answer_schema,
    error_schema,
    query_schema,
    query_message_schema,
    method_subscribe_schema,
    method_unsubscribe_schema,
    method_broadcast_schema,
    method_publish_schema,
    method_catchup_schema,

    message_data_schema,

    message_data_roll_call_close_schema,
    message_data_roll_call_create_schema,
    message_data_roll_call_open_schema,

    message_data_lao_create_schema,
    message_data_lao_update_schema,
    message_data_lao_state_schema,

    message_data_vote_cast_schema,

    message_data_election_end_schema,
    message_data_election_result_schema,
    message_data_election_setup_schema,

    message_data_meeting_create_schema,
    message_data_meeting_state_schema,
    message_data_message_witness_schema,

    message_data_chirp_add_broadcast_schema,
    message_data_chirp_add_schema
]);

module.exports = ajv;
