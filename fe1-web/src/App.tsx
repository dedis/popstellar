import 'react-native-gesture-handler';

import { ActionSheetProvider } from '@expo/react-native-action-sheet';
import { NavigationContainer } from '@react-navigation/native';
import { registerRootComponent } from 'expo';
import React from 'react';
import { Platform, StatusBar } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import Toast, { ToastProvider } from 'react-native-toast-notifications';
import { Provider } from 'react-redux';
import { PersistGate } from 'redux-persist/integration/react';

import { BuildInfo } from 'core/components';
import FeatureContext from 'core/contexts/FeatureContext';
import { configureKeyPair } from 'core/keypair';
import AppNavigation, { navigationRef } from 'core/navigation/AppNavigation';
import { configureNetwork } from 'core/network';
import { persist, store } from 'core/redux';
import { Color } from 'core/styles';
import { configureFeatures } from 'features';

import cameraPolyfill from './core/platform/camera/web-polyfill';
import wordBreakPolyfill from './core/platform/cssStyle/WordBreakPolyfill';

// load polyfill when the app loads
cameraPolyfill();

// This ensures that long words are broken correctly
wordBreakPolyfill();

const { messageRegistry, keyPairRegistry, navigationOpts, context } = configureFeatures();
configureKeyPair();
configureNetwork(messageRegistry, keyPairRegistry);
// start persisting the redux state after all reducers have been registered
persist.persist();

/*
 * The starting point of the app.
 *
 * It opens the navigation component in a safeAreaProvider to be able to use
 * SafeAreaView in order to resolve issue with status bar.
 * It initializes the message registry, configures the ingestion and message signatures.
 *
 * The Platform.OS is to put the statusBar in IOS in black, otherwise it is not readable
 */
function App() {
  return (
    <Provider store={store}>
      <PersistGate loading={null} persistor={persist}>
        <FeatureContext.Provider value={context}>
          <NavigationContainer ref={navigationRef}>
            <ActionSheetProvider>
              <SafeAreaProvider>
                {Platform.OS === 'ios' && (
                  <StatusBar barStyle="dark-content" backgroundColor="white" />
                )}
                <BuildInfo />
                <ToastProvider
                  normalColor={Color.primary}
                  successColor={Color.success}
                  warningColor={Color.warning}
                  dangerColor={Color.error}>
                  <AppNavigation screens={navigationOpts.screens} />
                  <Toast
                    ref={(ref) => {
                      globalThis.toast = ref;
                    }}
                  />
                </ToastProvider>
              </SafeAreaProvider>
            </ActionSheetProvider>
          </NavigationContainer>
        </FeatureContext.Provider>
      </PersistGate>
    </Provider>
  );
}

export default registerRootComponent(App);
