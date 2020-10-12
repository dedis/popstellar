import React from 'react'
import { StyleSheet, View, Text, Button } from 'react-native'

import STRINGS from '../res/strings'

/*
* The connect component
*/
class Connect extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.connect_description}</Text>
                <Button style={styles.object} title={STRINGS.connect_button_camera}/>
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