import React from 'react'
import {StyleSheet, View, Text} from 'react-native'

class Organization extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text>Organization screen</Text>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignContent: 'center',
        alignItems: 'center'
    },
    text: {
    },
  });

export default Organization