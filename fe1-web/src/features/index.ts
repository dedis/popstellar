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
import { NotificationCompositionConfiguration } from './notification/interface/Configuration';
import * as rollCall from './rollCall';
import * as social from './social';
import * as wallet from './wallet';
import * as witness from './witness';

export function configureFeatures() {
  const messageRegistry = new MessageRegistry();
  const keyPairRegistry = new KeyPairRegistry();

  // configure features
  const eventConfiguration = events.configure();
  const walletConfiguration = wallet.configure();

  const notificationConfiguration = notification.configure();
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
    addEvent: eventConfiguration.actionCreators.addEvent,
    updateEvent: eventConfiguration.actionCreators.updateEvent,
    /* events: functions */
    getEventById: eventConfiguration.functions.getEventById,
    /* other dependencies */
    messageRegistry,
  });

  const meetingConfiguration = meeting.configure({
    messageRegistry,
    addEvent: eventConfiguration.actionCreators.addEvent,
    updateEvent: eventConfiguration.actionCreators.updateEvent,
    getEventById: eventConfiguration.functions.getEventById,
    getLaoById: laoConfiguration.functions.getLaoById,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
  });

  const rollCallConfiguration = rollCall.configure({
    messageRegistry,
    addEvent: eventConfiguration.actionCreators.addEvent,
    updateEvent: eventConfiguration.actionCreators.updateEvent,
    getEventById: eventConfiguration.functions.getEventById,
    makeEventByTypeSelector: eventConfiguration.functions.makeEventByTypeSelector,
    getLaoById: laoConfiguration.functions.getLaoById,
    setLaoLastRollCall: laoConfiguration.actionCreators.setLaoLastRollCall,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    generateToken: walletConfiguration.functions.generateToken,
    hasSeed: walletConfiguration.functions.hasSeed,
  });

  const walletComposition = wallet.compose({
    keyPairRegistry,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    getEventById: eventConfiguration.functions.getEventById,
    getRollCallById: rollCallConfiguration.functions.getRollCallById,
    useRollCallsByLaoId: rollCallConfiguration.hooks.useRollCallsByLaoId,
  });

  const socialConfiguration = social.configure(messageRegistry);
  const witnessConfiguration = witness.configure({
    enabled: false,
    messageRegistry,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    isLaoWitness: laoConfiguration.functions.isLaoWitness,
    addNotification: notificationConfiguration.actionCreators.addNotification,
    markNotificationAsRead: notificationConfiguration.actionCreators.markNotificationAsRead,
    discardNotifications: notificationConfiguration.actionCreators.discardNotifications,
  });

  // compose features
  const notificationComposition = notification.compose({
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    notificationTypes: [
      ...witnessConfiguration.notificationTypes,
    ] as NotificationCompositionConfiguration['notificationTypes'],
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
        Component: walletComposition.navigation.WalletNavigation,
        order: 99999999,
      },
    ],
  });

  const eventsComposition = events.compose({
    eventTypes: [
      ...meetingConfiguration.eventTypes,
      ...rollCallConfiguration.eventTypes,
      ...evotingConfiguration.eventTypes,
    ],
    useIsLaoOrganizer: laoConfiguration.hooks.useIsLaoOrganizer,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
  });

  const laoComposition = lao.compose({
    /* events */
    EventList: eventConfiguration.components.EventList,
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
        Component: walletComposition.navigation.WalletNavigation,
        order: 99999999,
      },
    ],
    organizerNavigationScreens: [
      {
        id: STRINGS.organizer_navigation_tab_create_event,
        Component: eventConfiguration.screens.CreateEvent,
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
    ...eventConfiguration.reducers,
    ...walletComposition.reducers,
    ...meetingConfiguration.reducers,
    ...rollCallConfiguration.reducers,
    ...evotingConfiguration.reducers,
    ...walletComposition.reducers,
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
      [walletConfiguration.identifier]: walletComposition.context,
      [rollCallConfiguration.identifier]: rollCallConfiguration.context,
      [witnessConfiguration.identifier]: witnessConfiguration.context,
      [meetingConfiguration.identifier]: meetingConfiguration.context,
    },
  };
}
