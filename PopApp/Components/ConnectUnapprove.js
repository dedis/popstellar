import React from 'react'
import { StyleSheet, View, Text, Button } from 'react-native'

import STRINGS from '../res/strings'
import { Spacing, Typography } from '../Styles';


/**
* The unapproved component
*
* In the future will ask for camera permission
*/

class ConnectUnapprove extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>{STRINGS.connect_description}</Text>
                <View style={styles.button}>
                    <Button title={STRINGS.connect_button_camera} onPress={ () => {this.props.navigation.navigate("Scanning");} }/>
                </View>
            </View>
        );
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'space-evenly'
    },
    text: {
        ...Typography.base
    },
    button:{
        marginHorizontal: Spacing.xl
    }
  });

export default ConnectUnapprove
