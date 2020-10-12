import 'react-native-gesture-handler'
import React from 'react'
import { StyleSheet } from 'react-native'
import { SafeAreaProvider } from 'react-native-safe-area-context'

import Navigation from './Navigation/Navigation'

/*
* The starting point of the app
* It opens the navigation component in a safeAreaProvider in order to resolve issue with status bar
*/

export default function App() {
  return (
    <SafeAreaProvider>
      <Navigation/>
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