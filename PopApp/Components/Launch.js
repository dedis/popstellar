import React from 'react'
import { StyleSheet, View, Text, Button, TextInput } from 'react-native'

import STRINGS from '../res/strings'
import { Spacing, Typography } from '../Styles';


/*
* The Launch component
*
* Manage the Launch screen
*/
class Connect extends React.Component {
    constructor(props) {
        super(props)
        this.LAOName = React.createRef()
    }

    _cancelAction() {
        this.LAOName.current.clear()
        this.props.navigation.navigate("Home")
    }

    render() {
        return(
            <View style={styles.container}>
                <View style={styles.viewTop}>
                    <Text style={styles.text}>{STRINGS.launch_description}</Text>
                    <View style={styles.button}>
                        <TextInput ref={this.LAOName} style={styles.textInput} placeholder={STRINGS.launch_organization_name}/>
                    </View>
                </View>
                <View style={styles.viewBottom}>
                    <View style={styles.button}>
                        <Button title={STRINGS.launch_button_launch}/>
                    </View>
                    <View style={styles.button}>
                        <Button title={STRINGS.general_button_cancel} onPress={() => this._cancelAction()}/>
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
        ...Typography.base
    },
    textInput: {
        ...Typography.base,
        borderBottomWidth: 2,
    },
    button: {
        paddingHorizontal: Spacing.xl,
        paddingVertical: Spacing.s,
    },
    viewBottom: {
        justifyContent: "flex-end"
    },
  });

export default Connect