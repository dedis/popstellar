import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar, Platform } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import AppNavigation from './Navigation/AppNavigation';
import Navigation from './Navigation/Navigation';
import OrganizerNavigation from './Navigation/OrganizerNavigation';
import Attendee from './Components/Attendee';
import Identity from './Components/Identity';

/*
* The starting point of the app
*
* It opens the navigation component in a safeAreaProvider to be able use
* SafeAreaView in order to resolve issue with status bar
*
*  The Platform.OS is to put the statusBar in IOS in black, otherwise it is not readable
*/

export default function App() {
  return (
    <NavigationContainer>
      <SafeAreaProvider>
        {Platform.OS === 'ios'
          && <StatusBar barStyle="dark-content" backgroundColor="white" />}
        <AppNavigation />
      </SafeAreaProvider>
    </NavigationContainer>
  );
}
