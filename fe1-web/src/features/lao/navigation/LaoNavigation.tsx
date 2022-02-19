import React from 'react';
import { Platform, StyleSheet } from 'react-native';
import { useSelector } from 'react-redux';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';

import { getKeyPairState } from 'core/keypair';
import { getStore } from 'core/redux';
import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';
import { SocialMediaNavigation } from 'features/social/navigation';
import { WalletNavigation } from 'features/wallet/navigation';
import { WitnessNavigation } from 'features/witness/navigation';
import { Home } from 'features/home/screens';

import { makeCurrentLao } from '../reducer';
import { AttendeeScreen, Identity } from '../screens';
import OrganizerNavigation from './OrganizerNavigation';

const OrganizationTopTabNavigator = createMaterialTopTabNavigator();

/**
 * Navigation when connected to a lao
 *
 * Displays the following components:
 *  - Home
 *  - Social Media
 *  - Lao tab (corresponding to user role)
 *  - Identity
 *  - Wallet
 *  - Name of the connected lao (fake link)
 */
const styles = StyleSheet.create({
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

function getLaoTabName(isOrganizer: boolean, isWitness: boolean): string {
  if (isOrganizer) {
    return STRINGS.organization_navigation_tab_organizer;
  }

  if (isWitness) {
    return STRINGS.organization_navigation_tab_witness;
  }

  return STRINGS.organization_navigation_tab_attendee;
}

function buildTabComponent(isOrganizer: boolean, isWitness: boolean) {
  const tabName: string = getLaoTabName(isOrganizer, isWitness);
  let component;

  if (isOrganizer) {
    component = OrganizerNavigation;
  } else if (isWitness) {
    component = WitnessNavigation;
  } else {
    component = AttendeeScreen;
  }

  return <OrganizationTopTabNavigator.Screen name={tabName} component={component} />;
}

// Cannot omit the "component" attribute in Screen
// Moreover, cannot use a lambda in "component"
const DummyComponent = () => null;

const LaoNavigation: React.FC = () => {
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  const publicKeyRaw = getKeyPairState(getStore().getState()).keyPair?.publicKey;
  const publicKey = publicKeyRaw ? new PublicKey(publicKeyRaw) : undefined;

  const isOrganizer = !!(lao && publicKey && publicKey.equals(lao.organizer));
  const isWitness = !!(lao && publicKey && lao.witnesses.some((w) => publicKey.equals(w)));

  const tabName: string = getLaoTabName(isOrganizer, isWitness);
  const laoName: string = lao ? lao.name : STRINGS.unused;

  return (
    <OrganizationTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={tabName}
      screenOptions={{
        swipeEnabled: false,
      }}>
      <OrganizationTopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />

      <OrganizationTopTabNavigator.Screen
        name={STRINGS.navigation_tab_social_media}
        component={SocialMediaNavigation}
      />

      {buildTabComponent(isOrganizer, isWitness)}

      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_identity}
        component={Identity}
      />

      <OrganizationTopTabNavigator.Screen
        name={STRINGS.navigation_tab_wallet}
        component={WalletNavigation}
      />

      <OrganizationTopTabNavigator.Screen
        name={laoName}
        component={DummyComponent}
        listeners={{
          tabPress: (e) => {
            // => do nothing
            e.preventDefault();
          },
        }}
      />
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default LaoNavigation;
