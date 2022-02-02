import query from 'protocol/query/query.json';

import broadcast from 'protocol/query/method/broadcast.json';
import catchup from 'protocol/query/method/catchup.json';
import publish from 'protocol/query/method/publish.json';
import subscribe from 'protocol/query/method/subscribe.json';
import unsubscribe from 'protocol/query/method/unsubscribe.json';

const querySchemas = [
  query,
  broadcast,
  catchup,
  publish,
  subscribe,
  unsubscribe,
];

export default querySchemas;
