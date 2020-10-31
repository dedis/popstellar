import 'react-native-gesture-handler'
import React, { Component } from 'react';
import { StyleSheet, StatusBar, Platform } from 'react-native'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { NavigationContainer } from '@react-navigation/native'

import Navigation from './Navigation/Navigation'

/*
* The starting point of the app
*
* It opens the navigation component in a safeAreaProvider to be able use
* SafeAreaView in order to resolve issue with status bar
*
*  The Platform.OS is to put the statusBar in IOS in black, otherwise it is not readable
*/


export default class App extends Component {

  /* GET WS FROM PERSISTENT STORAGE IF IT EXISTS */


  render() {
    return (
      <SafeAreaProvider>
        {Platform.OS === "ios" &&
        <StatusBar barStyle='dark-content' backgroundColor='white'/>
        }
        <NavigationContainer>
          <Navigation />
        </NavigationContainer>
      </SafeAreaProvider>
    );
  };
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
