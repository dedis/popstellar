import React from 'react'
import {StyleSheet, View, Text, Button, ActivityIndicator} from 'react-native'

/*
    Connect to a LAO

    Currently, just simulate waiting for a response
*/

class ConnectConnecting extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <View>
                    <Text style={styles.text}>Connecting to URI</Text>
                </View>
                <View>
                    <View>
                        <ActivityIndicator size="large" color="#2196F3"/>
                    </View>
                </View>
                <View>
                    <View style={styles.button} >
                        <Button title="Cancel"
                                onPress={() => {
                                    this.props.navigation.navigate('Scanning');
                                    console.log(this.props.navigation)
                                }}/>
                    </View>
                    <View style={styles.button} >
                        <Button title="Simulate Validation"
                                onPress={() => {
                                    this.props.navigation.navigate('Confirm');
                                    console.log(this.props.navigation)
                                }}/>
                    </View>
                </View>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'space-evenly'
    },
    text: {
        fontSize: 25,
        textAlign: "center",
        marginHorizontal: 10
    },
    button:{
        marginHorizontal: 50,
        marginVertical: 10,
    },
  });

export default ConnectConnecting