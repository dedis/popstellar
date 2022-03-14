import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducers } from 'core/redux';
import STRINGS from '../resources/strings';

import * as connect from './connect';
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
  const connectConfiguration = connect.configure();
  const laoConfiguration = lao.configure({ registry: messageRegistry });
  const eventsConfiguration = events.configure({
    useIsLaoOrganizer: laoConfiguration.hooks.useIsLaoOrganizer,
  });

  const evotingConfiguration = evoting.configure({
    /* LAO FEATURE */
    /* lao: functions */
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    useCurrentLao: laoConfiguration.hooks.useCurrentLao,
    /* lao: hooks */
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    /* EVENTS FEATURE */
    /* events: action creators */
    addEvent: eventsConfiguration.actionCreators.addEvent,
    updateEvent: eventsConfiguration.actionCreators.updateEvent,
    /* events: functions */
    getEventById: eventsConfiguration.functions.getEventById,
    onConfirmEventCreation: eventsConfiguration.functions.onConfirmPress,
    /* other dependencies */
    messageRegistry,
  });
  meeting.configure(messageRegistry);
  rollCall.configure(messageRegistry);
  const socialConfiguration = social.configure(messageRegistry);
  witness.configure(messageRegistry);
  const walletConfiguration = wallet.configure(keyPairRegistry);

  // compose features

  const homeComposition = home.compose({
    /* functions */
    connectToTestLao: laoConfiguration.functions.openLaoTestConnection,
    createLao: laoConfiguration.functions.createLao,
    /* hoosk */
    useLaoList: laoConfiguration.hooks.useLaoList,
    LaoList: laoConfiguration.components.LaoList,
    mainNavigationScreens: [
      {
        name: STRINGS.navigation_tab_connect,
        Component: connectConfiguration.navigation.ConnectNavigation,
        order: 0,
      },
      {
        name: STRINGS.navigation_tab_wallet,
        Component: walletConfiguration.navigation.WalletNavigation,
        order: 3,
      },
    ],
  });

  const laoComposition = lao.compose({
    encodeLaoConnectionForQRCode: connectConfiguration.functions.encodeLaoConnectionInQRCode,
    laoNavigationScreens: [],
  });

  // verify configuration
  messageRegistry.verifyEntries();
  keyPairRegistry.verifyEntries();

  // setup all reducers
  addReducers({
    ...laoConfiguration.reducers,
    ...socialConfiguration.reducers,
    ...eventsConfiguration.reducers,
    ...walletConfiguration.reducers,
  });

  return {
    messageRegistry,
    keyPairRegistry,

    navigationOpts: {
      screens: [
        {
          name: STRINGS.app_navigation_tab_home,
          component: homeComposition.navigation.MainNavigation,
        },
        {
          name: STRINGS.app_navigation_tab_organizer,
          component: laoComposition.navigation.LaoNavigation,
        },
      ],
    },
    context: {
      [eventsConfiguration.identifier]: eventsConfiguration.context,
      [laoComposition.identifier]: laoComposition.context,
      [homeComposition.identifier]: homeComposition.context,
      [evotingConfiguration.identifier]: evotingConfiguration.context,
    },
  };
}
