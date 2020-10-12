import React from 'react'
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'
import Launch from '../Components/Launch'
import Home from '../Components/Home'
import Connect from '../Components/Connect'
import { Dimensions } from 'react-native'
import { NavigationContainer } from '@react-navigation/native'
import { SafeAreaView } from 'react-native-safe-area-context'

import STRINGS from '../res/strings'

const TopTabNavigator = createMaterialTopTabNavigator();

/*
* The main tab navigation component
*/

export default function TabNavigation() {
    return (
        <NavigationContainer>
            <SafeAreaView style={{flex:1}}>
            <TopTabNavigator.Navigator
                initialLayout={Dimensions.get('window').width}
                >
            <TopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
            <TopTabNavigator.Screen name={STRINGS.navigation_tab_connect} component={Connect} />
            <TopTabNavigator.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
            </TopTabNavigator.Navigator>
            </SafeAreaView>
        </NavigationContainer>
    );
  }