import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { addReducers, getStore } from 'core/redux';

import STRINGS from '../resources/strings';
import * as events from './events';
import { getEventFromId as getEventFromIdUnbound } from './events/network/EventHandlerUtils';
import { addEvent, updateEvent } from './events/reducer';
import { onConfirmPress } from './events/screens/CreateEvent';
import * as evoting from './evoting';
import * as home from './home';
import * as lao from './lao';
import { getCurrentLaoId as getCurrentLaoIdUnbound, makeCurrentLao } from './lao/reducer';
import * as meeting from './meeting';
import * as rollCall from './rollCall';
import * as social from './social';
import * as wallet from './wallet';
import * as witness from './witness';

export function configureFeatures() {
  const messageRegistry = new MessageRegistry();
  const keyPairRegistry = new KeyPairRegistry();

  const getCurrentLaoUnboound = makeCurrentLao();

  /**
   * Returns the current lao and throws an error if there is none
   * @returns The current lao
   */
  const getCurrentLao = () => {
    const currentLao = getCurrentLaoUnboound(getStore().getState());

    if (!currentLao) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return currentLao;
  };

  /**
   * Returns the current lao id and throws an error if there is none
   * @returns The current lao id
   */
  const getCurrentLaoId = () => {
    const currentLaoId = getCurrentLaoIdUnbound(getStore().getState());

    if (!currentLaoId) {
      throw new Error('Error encountered while accessing storage : no currently opened LAO');
    }

    return currentLaoId;
  };
  const getEventFromId = (id: Hash) => getEventFromIdUnbound(getStore().getState(), id);

  // configure features
  const laoConfig = lao.configure(messageRegistry);
  const homeConfig = home.configure();
  const evotingInterface = evoting.configure({
    getCurrentLao,
    getCurrentLaoId,
    addEvent,
    getEventFromId,
    updateEvent,
    onConfirmEventCreation: onConfirmPress,
    messageRegistry,
  });
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
    context: {
      [evotingInterface.identifier]: evotingInterface.context,
    },
  };
}
