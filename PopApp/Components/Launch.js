import React from 'react'
import {StyleSheet, View, Text, Button, TextInput} from 'react-native'

import STRINGS from '../res/strings'

/*
* The launch component
*/
class Connect extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.launch_description}</Text>
                <TextInput placeholder={STRINGS.launch_organization_name}/>
                <Button style={styles.object} title={STRINGS.launch_button_launch}/>
                <Button style={styles.object} title={STRINGS.launch_button_cancel}/>
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