import React from 'react'
import {StyleSheet, View, Text, Button, TextInput} from 'react-native'

/*
* The Launch component
*
* Manage the Launch screen
*/
class Connect extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <View style={styles.viewTop}>
                    <Text style={styles.text}>To launch a new organization please enter a name for the organization. (You can change it later.)</Text>
                    <View style={styles.button}>
                        <TextInput style={styles.textInput} placeholder="Organization name"/>
                    </View>
                </View>
                <View style={styles.viewBottom}>
                    <View style={styles.button}>
                        <Button title="Launch"/>
                    </View>
                    <View style={styles.button}>
                        <Button title="Cancel"/>
                    </View>
                </View>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: "space-around",
    },
    text: {
        textAlign: "center",
        fontSize: 25,
        marginHorizontal: 15,
    },
    textInput: {
        fontSize: 25,
        marginHorizontal: 15,
        borderBottomWidth: 2,
    },
    button: {
        paddingHorizontal: 50,
        paddingVertical: 20,
    },
    viewTop: {
    },
    viewBottom: {
        justifyContent: "flex-end"
    },
  });

export default Connect