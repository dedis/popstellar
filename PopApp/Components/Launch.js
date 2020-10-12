import React from 'react'
import {StyleSheet, View, Text, Button, TextInput} from 'react-native'
import STRINGS from "../res/strings";

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
                    <Text style={styles.text}>{STRINGS.launch_description}</Text>
                    <View style={styles.button}>
                        <TextInput style={styles.textInput} placeholder={STRINGS.launch_organization_name}/>
                    </View>
                </View>
                <View style={styles.viewBottom}>
                    <View style={styles.button}>
                        <Button title={STRINGS.launch_button_launch}/>
                    </View>
                    <View style={styles.button}>
                        <Button title={STRINGS.general_button_cancel}/>
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