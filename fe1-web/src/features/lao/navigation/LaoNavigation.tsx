import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import React, { useMemo } from 'react';
import { Platform, StyleSheet } from 'react-native';
import { useSelector, useStore } from 'react-redux';

import { getKeyPairState } from 'core/keypair';
import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import { selectCurrentLao } from '../reducer';
import { AttendeeScreen } from '../screens';
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

const getLaoTabName = (isOrganizer: boolean, isWitness: boolean): string => {
  if (isOrganizer) {
    return STRINGS.organization_navigation_tab_organizer;
  }

  if (isWitness) {
    return STRINGS.organization_navigation_tab_witness;
  }

  return STRINGS.organization_navigation_tab_attendee;
};

// Cannot omit the "component" attribute in Screen
// Moreover, cannot use a lambda in "component"
const DummyComponent = () => null;

const LaoNavigation: React.FC = () => {
  const lao = useSelector(selectCurrentLao);
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  const store = useStore();

  const publicKeyRaw = getKeyPairState(store.getState()).keyPair?.publicKey;
  const publicKey = publicKeyRaw ? new PublicKey(publicKeyRaw) : undefined;

  const isOrganizer = !!(lao && publicKey && publicKey.equals(lao.organizer));
  const isWitness = !!(lao && publicKey && lao.witnesses.some((w) => publicKey.equals(w)));

  const tabName: string = getLaoTabName(isOrganizer, isWitness);
  const laoName: string = lao ? lao.name : STRINGS.unused;

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.Screen[] = useMemo(() => {
    const screenName = getLaoTabName(isOrganizer, isWitness);

    let Component: React.ComponentType<any>;

    if (isOrganizer || isWitness) {
      Component = OrganizerNavigation;
    } else {
      Component = AttendeeScreen;
    }

    const s = [
      ...passedScreens,
      {
        id: STRINGS.organization_navigation_tab_user,
        title: screenName,
        Component,
        order: 2,
      } as LaoFeature.Screen,
    ];

    // sort screens by order before rendering them
    s.sort((a, b) => a.order - b.order);

    return s;
  }, [passedScreens, isOrganizer, isWitness]);

  return (
    <OrganizationTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={tabName}
      screenOptions={{
        swipeEnabled: false,
      }}>
      {screens.map(({ id, title, Component }) => (
        <OrganizationTopTabNavigator.Screen
          key={id}
          name={id}
          component={Component}
          options={{ title: title || id }}
        />
      ))}

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
