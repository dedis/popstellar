import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { addReducers } from 'core/redux';

import * as digitalCash from './digital-cash';
import * as events from './events';
import * as evoting from './evoting';
import * as home from './home';
import * as lao from './lao';
import * as linkedOrganizations from './linked-organizations';
import * as meeting from './meeting';
import * as notification from './notification';
import { NotificationCompositionConfiguration } from './notification/interface/Configuration';
import * as popcha from './popcha';
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
  const digitalCashConfiguration = digitalCash.configure();

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
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
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
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
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
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    generateToken: walletConfiguration.functions.generateToken,
    hasSeed: walletConfiguration.functions.hasSeed,
  });

  const walletComposition = wallet.compose({
    keyPairRegistry,
    messageRegistry,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    useCurrentLao: laoConfiguration.hooks.useCurrentLao,
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    getEventById: eventConfiguration.functions.getEventById,
    getRollCallById: rollCallConfiguration.functions.getRollCallById,
    useRollCallsByLaoId: rollCallConfiguration.hooks.useRollCallsByLaoId,
    useRollCallTokensByLaoId: rollCallConfiguration.hooks.useRollCallTokensByLaoId,
  });

  const digitalCashComposition = digitalCash.compose({
    messageRegistry: messageRegistry,
    keyPairRegistry: keyPairRegistry,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    useIsLaoOrganizer: laoConfiguration.hooks.useIsLaoOrganizer,
    getLaoOrganizer: laoConfiguration.functions.getLaoOrganizer,
    useRollCallById: rollCallConfiguration.hooks.useRollCallById,
    useRollCallsByLaoId: rollCallConfiguration.hooks.useRollCallsByLaoId,
    useRollCallTokensByLaoId: rollCallConfiguration.hooks.useRollCallTokensByLaoId,
    useRollCallTokenByRollCallId: rollCallConfiguration.hooks.useRollCallTokenByRollCallId,
  });

  const socialConfiguration = social.configure({
    messageRegistry,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    useCurrentLao: laoConfiguration.hooks.useCurrentLao,
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    useRollCallById: rollCallConfiguration.hooks.useRollCallById,
    useRollCallAttendeesById: rollCallConfiguration.hooks.useRollCallAttendeesById,
    generateToken: walletConfiguration.functions.generateToken,
  });

  const witnessConfiguration = witness.configure({
    enabled: false,
    messageRegistry,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    getCurrentLao: laoConfiguration.functions.getCurrentLao,
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    isLaoWitness: laoConfiguration.functions.isLaoWitness,
    addNotification: notificationConfiguration.actionCreators.addNotification,
    markNotificationAsRead: notificationConfiguration.actionCreators.markNotificationAsRead,
    discardNotifications: notificationConfiguration.actionCreators.discardNotifications,
  });

  const popchaConfiguration = popcha.configure({
    messageRegistry: messageRegistry,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    generateToken: walletConfiguration.functions.generateToken,
  });

  const linkedOrganizationsConfiguration = linkedOrganizations.configure({
    messageRegistry: messageRegistry,
    keyPairRegistry: keyPairRegistry,
    useCurrentLaoId: laoConfiguration.hooks.useCurrentLaoId,
    useIsLaoOrganizer: laoConfiguration.hooks.useIsLaoOrganizer,
    useCurrentLao: laoConfiguration.hooks.useCurrentLao,
    getCurrentLaoId: laoConfiguration.functions.getCurrentLaoId,
    getLaoOrganizerBackendPublicKey: laoConfiguration.functions.getLaoOrganizerBackendPublicKey,
    getLaoById: laoConfiguration.functions.getLaoById,
    getRollCallById: rollCallConfiguration.functions.getRollCallById,
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
    getLaoById: laoConfiguration.functions.getLaoById,
    resubscribeToLao: laoConfiguration.functions.resubscribeToLao,
    /* action creators */
    addLaoServerAddress: laoConfiguration.actionCreators.addLaoServerAddress,
    /* hooks */
    useLaoList: laoConfiguration.hooks.useLaoList,
    useConnectedToLao: laoConfiguration.hooks.useConnectedToLao,
    useDisconnectFromLao: laoConfiguration.hooks.useDisconnectFromLao,
    /* components */
    LaoList: laoConfiguration.components.LaoList,
    /* screens */
    homeNavigationScreens: [],
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
      ...socialConfiguration.laoScreens,
      ...notificationConfiguration.laoScreens,
      ...walletComposition.laoScreens,
      ...digitalCashConfiguration.laoScreens,
      ...popchaConfiguration.laoScreens,
      ...linkedOrganizationsConfiguration.laoScreens,
    ],
    eventsNavigationScreens: [
      ...eventConfiguration.laoEventScreens,
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
    ...digitalCashComposition.reducers,
    ...socialConfiguration.reducers,
    ...eventConfiguration.reducers,
    ...walletComposition.reducers,
    ...meetingConfiguration.reducers,
    ...rollCallConfiguration.reducers,
    ...evotingConfiguration.reducers,
    ...walletComposition.reducers,
    ...witnessConfiguration.reducers,
    ...linkedOrganizationsConfiguration.reducers,
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
      [digitalCashComposition.identifier]: digitalCashComposition.context,
      [socialConfiguration.identifier]: socialConfiguration.context,
      [popchaConfiguration.identifier]: popchaConfiguration.context,
      [linkedOrganizationsConfiguration.identifier]: linkedOrganizationsConfiguration.context,
    },
  };
}
