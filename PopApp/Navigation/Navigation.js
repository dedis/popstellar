import React from 'react'
import {createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs'
import Launch from '../Components/Launch'
import Home from '../Components/Home'
import Connect from '../Components/Connect'
import {Dimensions} from 'react-native'
import { NavigationContainer } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context'


const TopTabNavigator = createMaterialTopTabNavigator()

/*
* The main tab navigation component
*/

export default function TabNavigation() {
    return (
        <NavigationContainer>
            <SafeAreaView style={{flex:1}}>
            <TopTabNavigator.Navigator
                initialLayout={ width= Dimensions.get('window').width }
                >
            <TopTabNavigator.Screen name="Home" component={Home} />
            <TopTabNavigator.Screen name="Connect" component={Connect} />
            <TopTabNavigator.Screen name="Launch" component={Launch} />
            </TopTabNavigator.Navigator>
            </SafeAreaView>
        </NavigationContainer>
    );
  }