import React from 'react'
import {StyleSheet, View, Text, Button, TextInput} from 'react-native'

/*
* The launch component
*/
class Connect extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>To launch a new organization please enter a name for the organization. (You can change it later.)</Text>
                <TextInput placeholder="Organization name"/>
                <Button style={styles.object} title="Launch"/>
                <Button style={styles.object} title="Cancel"/>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'center'
    },
    text: {
        textAlign: "center"
    }
  });

export default Connect