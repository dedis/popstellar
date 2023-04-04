import broadcast from 'protocol/query/method/broadcast.json';
import catchup from 'protocol/query/method/catchup.json';
import get_messages_by_id from 'protocol/query/method/get_messages_by_id.json';
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
  query,
];

export default querySchemas;
