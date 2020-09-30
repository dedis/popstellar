import React from 'react'
import {StyleSheet, View, Text, Button} from 'react-native'

class Connect extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>The easiest way to connect to a local organization is to scan its QR code</Text>
                <Button style={styles.object} title="Enable Camera Access"/>
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