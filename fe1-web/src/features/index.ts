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
import * as notification from './notification';
import * as rollCall from './rollCall';
import * as social from './social';
import * as wallet from './wallet';
import * as witness from './witness';

export function configureFeatures() {
  const messageRegistry = new MessageRegistry();
  const keyPairRegistry = new KeyPairRegistry();

  // configure features
  const notificationConfiguration = notification.configure();
  const eventsConfiguration = events.configure();
  const laoConfiguration = lao.configure({ registry: messageRegistry });
  const connectConfiguration = connect.configure({
    addLaoServerAddress: laoConfiguration.actionCreators.addLaoServerAddress,
    getLaoChannel: laoConfiguration.functions.getLaoChannel,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
  });

  const evotingConfiguration = evoting.configure({
    /* LAO FEATURE */
    /* lao: functions */
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    /* lao: hooks */
    useCurrentLao: laoConfiguration.hooks.useCurrentLao,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    /* EVENTS FEATURE */
    /* events: action creators */
    addEvent: eventsConfiguration.actionCreators.addEvent,
    updateEvent: eventsConfiguration.actionCreators.updateEvent,
    /* events: functions */
    getEventById: eventsConfiguration.functions.getEventById,
    /* other dependencies */
    messageRegistry,
  });
  const meetingConfiguration = meeting.configure(messageRegistry);
  const rollCallConfiguration = rollCall.configure(messageRegistry);
  const socialConfiguration = social.configure(messageRegistry);
  const witnessConfiguration = witness.configure({
    enabled: false,
    messageRegistry,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    isLaoWitness: laoConfiguration.functions.isLaoWitness,
    addNotification: notificationConfiguration.actionCreators.addNotification,
    markNotificationAsRead: notificationConfiguration.actionCreators.markNotificationAsRead,
    discardNotifications: notificationConfiguration.actionCreators.discardNotifications,
  });
  const walletConfiguration = wallet.configure(keyPairRegistry);

  // compose features
  const notificationComposition = notification.compose({
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    notificationTypes: [...witnessConfiguration.notificationTypes],
  });
  const homeComposition = home.compose({
    /* functions */
    connectToTestLao: laoConfiguration.functions.openLaoTestConnection,
    requestCreateLao: laoConfiguration.functions.requestCreateLao,
    /* action creators */
    addLaoServerAddress: laoConfiguration.actionCreators.addLaoServerAddress,
    /* hooks */
    useLaoList: laoConfiguration.hooks.useLaoList,
    LaoList: laoConfiguration.components.LaoList,
    mainNavigationScreens: [
      {
        id: STRINGS.navigation_tab_connect,
        Component: connectConfiguration.navigation.ConnectNavigation,
        order: -10000,
      },
      {
        id: STRINGS.navigation_tab_wallet,
        Component: walletConfiguration.navigation.WalletNavigation,
        order: 99999999,
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
    encodeLaoConnectionForQRCode: connectConfiguration.functions.encodeLaoConnectionForQRCode,
    /* navigation */
    laoNavigationScreens: [
      {
        id: STRINGS.navigation_tab_home,
        Component: homeComposition.screens.Home,
        order: -99999999,
      },
      {
        id: STRINGS.navigation_tab_social_media,
        Component: socialConfiguration.navigation.SocialMediaNavigation,
        order: 0,
      },
      {
        id: STRINGS.organization_navigation_tab_notifications,
        Component: notificationConfiguration.navigation.NotificationNavigation,
        order: 70000,
        Badge: notificationConfiguration.components.NotificationBadge,
      },
      {
        id: STRINGS.navigation_tab_wallet,
        Component: walletConfiguration.navigation.WalletNavigation,
        order: 99999999,
      },
    ],
    organizerNavigationScreens: [
      {
        id: STRINGS.organizer_navigation_tab_create_event,
        Component: eventsConfiguration.screens.CreateEvent,
        order: 0,
      },
      {
        id: STRINGS.organizer_navigation_creation_meeting,
        Component: meetingConfiguration.screens.CreateMeeting,
        order: 10000,
      },
      {
        id: STRINGS.organizer_navigation_creation_roll_call,
        Component: rollCallConfiguration.screens.CreateRollCall,
        order: 20000,
      },
      {
        id: STRINGS.organizer_navigation_creation_election,
        Component: evotingConfiguration.screens.CreateElection,
        order: 30000,
      },
      {
        id: STRINGS.roll_call_open,
        Component: rollCallConfiguration.screens.RollCallOpened,
        order: 40000,
      },
    ],
  });

  // verify configuration
  messageRegistry.verifyEntries();
  keyPairRegistry.verifyEntries();

  // setup all reducers
  addReducers({
    ...notificationConfiguration.reducers,
    ...laoConfiguration.reducers,
    ...socialConfiguration.reducers,
    ...eventsConfiguration.reducers,
    ...walletConfiguration.reducers,
    ...witnessConfiguration.reducers,
  });

  return {
    messageRegistry,
    keyPairRegistry,

    navigationOpts: {
      screens: [
        {
          id: STRINGS.app_navigation_tab_home,
          component: homeComposition.navigation.MainNavigation,
        },
        {
          id: STRINGS.app_navigation_tab_lao,
          component: laoComposition.navigation.LaoNavigation,
        },
      ],
    },
    context: {
      [notificationComposition.identifier]: notificationComposition.context,
      [connectConfiguration.identifier]: connectConfiguration.context,
      [eventsComposition.identifier]: eventsComposition.context,
      [laoComposition.identifier]: laoComposition.context,
      [homeComposition.identifier]: homeComposition.context,
      [evotingConfiguration.identifier]: evotingConfiguration.context,
      [witnessConfiguration.identifier]: witnessConfiguration.context,
    },
  };
}
