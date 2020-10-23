import React, { Fragment } from 'react'
import { Platform, StyleSheet, View, Text } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { createMaterialTopTabNavigator, MaterialTopTabBar } from '@react-navigation/material-top-tabs'
import { useTheme } from '@react-navigation/native';

import STRINGS from '../res/strings'

import Attendee from '../Components/Attendee'
import Identity from '../Components/Identity'
import { Spacing, Views } from '../Styles'


const OrganizerTopTabNavigator = createMaterialTopTabNavigator();

/**
* The organizer tab navigation component
*
* create a tab navigator between the Home, Attendee, Organizer, Witness and Identity component
*
* the SafeAreaView resolves problem with status bar overlap
*/

function HomeScreen({ navigation }) {
    React.useEffect(() => {
      const unsubscribe = navigation.addListener('tabPress', e => {
        // Prevent default behavior
        e.preventDefault();
        let parentNavigation = navigation.dangerouslyGetParent();
        if(parentNavigation != undefined){
            parentNavigation.navigate(STRINGS.app_navigation_tab_home)
        }
      });
  
      return unsubscribe;
    }, [navigation]);
  
    return (
      <View />
    );
}

function LAOName({ navigation }) {
    React.useEffect(() => {
      const unsubscribe = navigation.addListener('tabPress', e => {
        // Prevent default behavior
        e.preventDefault();
      });
  
      return unsubscribe;
    }, [navigation]);
  
    return (
      <View />
    );
}

/*
function MyTabBar(props) {
    return (
      <View style={ styles.fragment }>
          <MaterialTopTabBar {...props} style={ styles.materialTab }/>
          <Text style={[ {backgroundColor: useTheme().colors.card}, styles.laoName ]}>LAO Name</Text>
      </View>
    );
  }
*/


export default function TabNavigation() {
    return (
        <SafeAreaView style={ styles.view }>
            <OrganizerTopTabNavigator.Navigator style={ styles.navigator } 
                                                initialRouteName={STRINGS.organizer_navigation_tab_attendee}
                                                /*tabBar={ props => <MyTabBar {...props}/> }*/>
                <OrganizerTopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={HomeScreen} />
                <OrganizerTopTabNavigator.Screen name={STRINGS.organizer_navigation_tab_attendee} component={Attendee} />
                <OrganizerTopTabNavigator.Screen name={STRINGS.organizer_navigation_tab_identity} component={Identity} />
                <OrganizerTopTabNavigator.Screen name={'LAO name'} component={LAOName} />
            </OrganizerTopTabNavigator.Navigator>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    view: {
        flex:1,
    },
    navigator: {
        ...Platform.select({
            web: {
                width: '100vw',
            },
            default: {}
        })
    },
    /*
    fragment: {
        flexDirection: 'row',
    },
    materialTab: {
        flex: 3,
    },
    laoName: {
        flex: 1,
        textTransform: 'uppercase',
        fontSize: 13,
        textAlign: 'center',
        textAlignVertical: 'center',
    }
    */
});
