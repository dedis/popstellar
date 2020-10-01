import React from 'react'
import {StyleSheet, View, Text} from 'react-native'

/*
* The attendee component
*/
class Attendee extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text>Attendee screen</Text>
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

export default Attendee