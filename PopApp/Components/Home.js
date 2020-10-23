import React from 'react'
import { StyleSheet, View, Text, FlatList } from 'react-native'


import STRINGS from '../res/strings'
import { Spacing, Typography } from '../Styles';
import LAOItem from './LAOItem'

import LAO from '../res/laoData'


/**
* The Home component
*
* Manage the Home screen
*/
class Home extends React.Component {
    _render() {
        if(!LAO || !LAO.length){
            return(
                <View style={styles.container}>
                    <Text style={styles.text}>{STRINGS.home_welcome}</Text>
                    <Text style={styles.text}>{STRINGS.home_connect_lao}</Text>
                    <Text style={styles.text}>{STRINGS.home_launch_lao}</Text>
                </View>
            )
        }else{
            return(
                <View style={styles.container}>
                    <FlatList
                        data={LAO}
                        keyExtractor={(item) => item.id.toString()}
                        renderItem={({item}) => <LAOItem LAO={item} navigation={this.props.navigation}/>}
                        style={styles.flatList}
                    />
                </View>
            )
        }
    }

    render() {
        return(
            this._render()
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
    },
    text: {
        ...Typography.important
    },
    flatList: {
        marginTop: Spacing.s
    }
  });

export default Home