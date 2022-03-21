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
  const eventsConfiguration = events.configure();
  const laoConfiguration = lao.configure({ registry: messageRegistry });
  const connectConfiguration = connect.configure({
    setLaoServerAddress: laoConfiguration.actionCreators.setLaoServerAddress,
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
  const meetingConfiguration = meeting.configure(messageRegistry);
  const rollCallConfiguration = rollCall.configure(messageRegistry);
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

  const eventsComposition = events.compose({
    eventTypeComponents: [
      ...meetingConfiguration.eventTypeComponents,
      ...rollCallConfiguration.eventTypeComponents,
      ...evotingConfiguration.eventTypeComponents,
    ],
    useIsLaoOrganizer: laoConfiguration.hooks.useIsLaoOrganizer,
  });

  const laoComposition = lao.compose({
    /* events */
    EventList: eventsConfiguration.components.EventList,
    /* connect */
    encodeLaoConnectionForQRCode: connectConfiguration.functions.encodeLaoConnectionInQRCode,
    /* navigation */
    laoNavigationScreens: [
      { name: STRINGS.navigation_tab_home, Component: homeComposition.screens.Home, order: 0 },
      {
        name: STRINGS.navigation_tab_social_media,
        Component: socialConfiguration.navigation.SocialMediaNavigation,
        order: 1,
      },
      {
        name: STRINGS.navigation_tab_wallet,
        Component: walletConfiguration.navigation.WalletNavigation,
        order: 4,
      },
    ],
    organizerNavigationScreens: [
      {
        name: STRINGS.organizer_navigation_tab_create_event,
        Component: eventsConfiguration.screens.CreateEvent,
        order: 0,
      },
      {
        name: STRINGS.organizer_navigation_creation_meeting,
        Component: meetingConfiguration.screens.CreateMeeting,
        order: 1,
      },
      {
        name: STRINGS.organizer_navigation_creation_roll_call,
        Component: rollCallConfiguration.screens.CreateRollCall,
        order: 2,
      },
      {
        name: STRINGS.organizer_navigation_creation_election,
        Component: evotingConfiguration.screens.CreateElection,
        order: 3,
      },
      {
        name: STRINGS.roll_call_open,
        Component: rollCallConfiguration.screens.RollCallOpened,
        order: 4,
      },
    ],
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
      [connectConfiguration.identifier]: connectConfiguration.context,
      [eventsComposition.identifier]: eventsComposition.context,
      [laoComposition.identifier]: laoComposition.context,
      [homeComposition.identifier]: homeComposition.context,
      [evotingConfiguration.identifier]: evotingConfiguration.context,
    },
  };
}
