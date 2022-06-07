import { makeIcon } from 'core/components/PoPIcon';
import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducers } from 'core/redux';

import STRINGS from '../resources/strings';
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

  const evotingConfiguration = evoting.configure({
    /* LAO FEATURE */
    /* lao: functions */
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    getLaoOrganizerBackendPublicKey: laoConfiguration.functions.getLaoOrganizerBackendPublicKey,
    useLaoOrganizerBackendPublicKey: laoConfiguration.hooks.useLaoOrganizerBackendPublicKey,
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
    getLaoChannel: laoConfiguration.functions.getLaoChannel,
    /* action creators */
    addLaoServerAddress: laoConfiguration.actionCreators.addLaoServerAddress,
    /* hooks */
    useLaoList: laoConfiguration.hooks.useLaoList,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    /* components */
    LaoList: laoConfiguration.components.LaoList,
    /* screens */
    homeNavigationScreens: [...walletComposition.homeScreens],
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
    CreateEventButton: eventConfiguration.components.CreateEventButton,
    /* connect */
    encodeLaoConnectionForQRCode: homeComposition.functions.encodeLaoConnectionForQRCode,
    /* navigation */
    laoNavigationScreens: [
      {
        id: STRINGS.navigation_social_media,
        Component: socialConfiguration.navigation.SocialMediaNavigation,
        headerShown: false,
        tabBarIcon: makeIcon('socialMedia'),
        order: 10000,
      },
      ...notificationConfiguration.laoScreens,
      ...walletComposition.laoScreens,
    ],
    eventsNavigationScreens: [
      ...meetingConfiguration.laoEventScreens,
      ...rollCallConfiguration.laoEventScreens,
      ...evotingConfiguration.laoEventScreens,
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
        ...homeComposition.appScreens,
        ...walletComposition.appScreens,
        ...laoComposition.appScreens,
      ],
    },
    context: {
      [notificationComposition.identifier]: notificationComposition.context,
      [eventsComposition.identifier]: eventsComposition.context,
      [laoComposition.identifier]: laoComposition.context,
      [homeComposition.identifier]: homeComposition.context,
      [meetingConfiguration.identifier]: meetingConfiguration.context,
      [evotingConfiguration.identifier]: evotingConfiguration.context,
      [walletConfiguration.identifier]: walletComposition.context,
      [rollCallConfiguration.identifier]: rollCallConfiguration.context,
      [witnessConfiguration.identifier]: witnessConfiguration.context,
    },
  };
}
