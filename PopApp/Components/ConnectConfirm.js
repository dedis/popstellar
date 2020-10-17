import React from 'react'
import {StyleSheet, View, Text, Button} from 'react-native'
import { ScrollView } from 'react-native-gesture-handler'

import STRINGS from '../res/strings'
import { Spacing, Typography } from '../Styles'

/**
* Ask for confirmation to connect to LAO
*
* Currently, just go to the Launch tab.
*/

class ConnectConfirm extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <View style={styles.view}>
                    <Text style={styles.text}>{STRINGS.connect_confirm_description}</Text>
                </View>
                <View style={styles.viewCenter}>
                    <ScrollView>
                        <Text style={styles.text}>{STRINGS.lorem_ipsum}</Text>
                    </ScrollView>
                </View>
                <View style={styles.buttonView}>
                    <View style={styles.button}>
                        <Button title={STRINGS.general_button_confirm} style={styles.button}
                                                onPress={() => {
                                                    let parentNavigation = this.props.navigation.dangerouslyGetParent();
                                                    if(parentNavigation != undefined){
                                                        parentNavigation.navigate('Launch')
                                                    }
                                                }}/>
                    </View>
                    <View style={styles.button}>
                        <Button title={STRINGS.general_button_cancel} style={styles.button} onPress={() => {this.props.navigation.navigate("Scanning");}}/>
                    </View>
                </View>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    text: {
        ...Typography.base,
    },
    buttonView:{
        flex: 2,
        flexDirection: 'column',
        marginBottom: Spacing.xl
    },
    button:{
        padding: Spacing.xs
    },
    viewCenter: {
        flex: 8,
        justifyContent: 'center',
        borderWidth: 1,
        margin: Spacing.xs,
    },
    view: {
        flex: 1,
        justifyContent: 'center'
    }
  });

export default ConnectConfirm