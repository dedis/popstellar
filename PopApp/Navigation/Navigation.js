import React from 'react'
import {createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'
import Launch from '../Components/Launch'
import Home from '../Components/Home'
import ConnectNavigation from '../Navigation/ConnectNavigation'
import { SafeAreaView } from 'react-native-safe-area-context'

import STRINGS from '../res/strings'
import { Dimensions, StyleSheet } from 'react-native'

const TopTabNavigator = createMaterialTopTabNavigator();

/**
* The main tab navigation component
*
* create a tab navigator between the Home, Connect and Launch component
*
* the SafeAreaView resolves problem with status bar overlap
*/

export default function TabNavigation() {
    return (
        <SafeAreaView style={ styles.view }>
            <TopTabNavigator.Navigator style={ styles.navigator }>
                <TopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
                <TopTabNavigator.Screen name={STRINGS.navigation_tab_connect} component={ConnectNavigation} />
                <TopTabNavigator.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
            </TopTabNavigator.Navigator>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    view: {
        flex:1,
    },
    navigator: {
        width: "100vw"
    }
})