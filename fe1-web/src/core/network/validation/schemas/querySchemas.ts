import broadcast from 'protocol/query/method/broadcast.json';
import catchup from 'protocol/query/method/catchup.json';
import get_messages_by_id from 'protocol/query/method/get_messages_by_id.json';
import greet_server from 'protocol/query/method/greet_server.json';
import heartbeat from 'protocol/query/method/heartbeat.json';
import publish from 'protocol/query/method/publish.json';
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
];

export default querySchemas;
