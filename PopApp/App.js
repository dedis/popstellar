import 'react-native-gesture-handler'
import React from 'react';
import { StyleSheet, StatusBar, Platform } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import Navigation from './Navigation/Navigation'
import { NavigationContainer } from '@react-navigation/native';

/*
* The start point of the app
*
* It open the navigation component in a safeAreaProvider to be able use
* SafeAreaView in order to resolve issue with status bar
*
*  The Platform.OS is to put the statusBar in IOS in black, otherwise it is not readable
*/

export default function App() {
  return (
    <SafeAreaProvider>
      {Platform.OS === "ios" && 
        <StatusBar barStyle='dark-content' backgroundColor='white'/>
      }
      <NavigationContainer>
        <Navigation/>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});