import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar, Platform } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { useReduxDevToolsExtension } from '@react-navigation/devtools';
import { PersistGate } from 'redux-persist/integration/react';
import { ToastProvider } from 'react-native-toast-notifications';

import { Provider } from 'react-redux';
import { store, persist } from 'store/Storage';

import AppNavigation from 'navigation/AppNavigation';
import { navigationRef } from 'navigation/RootNavigation';

import { configureIngestion } from 'ingestion';
import { MessageRegistry } from 'model/network/method/message/data';

/*
* The starting point of the app
*
* It opens the navigation component in a safeAreaProvider to be able use
* SafeAreaView in order to resolve issue with status bar
*
*  The Platform.OS is to put the statusBar in IOS in black, otherwise it is not readable
*/

export default function App() {
  configureIngestion();

  useReduxDevToolsExtension(navigationRef);

  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persist}>
        <NavigationContainer ref={navigationRef}>
          <SafeAreaProvider>
            {Platform.OS === 'ios'
            && <StatusBar barStyle="dark-content" backgroundColor="white" />}
            <ToastProvider>
              <AppNavigation />
            </ToastProvider>
          </SafeAreaProvider>
        </NavigationContainer>
      </PersistGate>
    </Provider>
  );
}

// Initialize the message registry
export const messageRegistry = new MessageRegistry();
