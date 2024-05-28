import broadcast from 'protocol/query/method/broadcast.json';
import catchup from 'protocol/query/method/catchup.json';
import get_messages_by_id from 'protocol/query/method/get_messages_by_id.json';
import greet_server from 'protocol/query/method/greet_server.json';
import heartbeat from 'protocol/query/method/heartbeat.json';
import rumor_object from 'protocol/query/method/object/rumor.json';
import rumor_state_object from 'protocol/query/method/object/rumor_state.json';
import paged_catchup from 'protocol/query/method/paged_catchup.json';
import publish from 'protocol/query/method/publish.json';
import rumor from 'protocol/query/method/rumor.json';
import rumor_state from 'protocol/query/method/rumor_state.json';
import subscribe from 'protocol/query/method/subscribe.json';
import unsubscribe from 'protocol/query/method/unsubscribe.json';
import query from 'protocol/query/query.json';

const querySchemas = [
  broadcast,
  catchup,
  publish,
  subscribe,
  unsubscribe,
  heartbeat,
  get_messages_by_id,
  greet_server,
  query,
  rumor,
  rumor_object,
  rumor_state,
  rumor_state_object,
  paged_catchup,
];

export default querySchemas;
