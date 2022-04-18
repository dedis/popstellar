import broadcast from 'protocol/query/method/broadcast.json';
import catchup from 'protocol/query/method/catchup.json';
import greeting from 'protocol/query/method/greeting.json';
import publish from 'protocol/query/method/publish.json';
import subscribe from 'protocol/query/method/subscribe.json';
import unsubscribe from 'protocol/query/method/unsubscribe.json';
import query from 'protocol/query/query.json';

const querySchemas = [broadcast, catchup, greeting, publish, subscribe, unsubscribe, query];

export default querySchemas;
