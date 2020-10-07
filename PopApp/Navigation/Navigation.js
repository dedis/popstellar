import React from 'react'
import {createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'
import Launch from '../Components/Launch'
import Home from '../Components/Home'
import ConnectNavigation from '../Navigation/ConnectNavigation'
import { SafeAreaView } from 'react-native-safe-area-context'


const TopTabNavigator = createMaterialTopTabNavigator()

/*
* The main tab navigation component
*
* create a tab navigator beetween the Home, Connect and Launch component
*
* the SafeAreaView resolve problem with staus bar overlap
*/

export default function TabNavigation() {
    return (
        <SafeAreaView style={{ flex: 1}}>
            <TopTabNavigator.Navigator>
            <TopTabNavigator.Screen name="Home" component={Home} />
            <TopTabNavigator.Screen name="Connect" component={ConnectNavigation} />
            <TopTabNavigator.Screen name="Launch" component={Launch} />
            </TopTabNavigator.Navigator>
        </SafeAreaView>
    );
  }