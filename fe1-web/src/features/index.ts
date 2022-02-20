import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducers } from 'core/redux';
import STRINGS from '../resources/strings';

import * as events from './events';
import * as evoting from './evoting';
import * as home from './home';
import * as lao from './lao';
import * as meeting from './meeting';
import * as rollCall from './rollCall';
import * as social from './social';
import * as wallet from './wallet';
import * as witness from './witness';

export function configureFeatures() {
  const messageRegistry = new MessageRegistry();
  const keyPairRegistry = new KeyPairRegistry();

  // configure features
  const laoConfig = lao.configure(messageRegistry);
  const homeConfig = home.configure();
  evoting.configure(messageRegistry);
  meeting.configure(messageRegistry);
  rollCall.configure(messageRegistry);
  const socialConfig = social.configure(messageRegistry);
  witness.configure(messageRegistry);
  const eventsConfig = events.configure();
  const walletConfig = wallet.configure(keyPairRegistry);

  // verify configuration
  messageRegistry.verifyEntries();
  keyPairRegistry.verifyEntries();

  // setup all reducers
  addReducers({
    ...laoConfig.reducers,
    ...socialConfig.reducers,
    ...eventsConfig.reducers,
    ...walletConfig.reducers,
  });

  return {
    messageRegistry,
    keyPairRegistry,

    navigationOpts: {
      screens: [
        {
          name: STRINGS.app_navigation_tab_home,
          component: homeConfig.navigation.MainNavigation,
        },
        {
          name: STRINGS.app_navigation_tab_organizer,
          component: laoConfig.navigation.LaoNavigation,
        },
      ],
    },
  };
}
