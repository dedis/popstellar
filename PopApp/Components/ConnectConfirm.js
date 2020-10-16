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
                    <View>
                        <ScrollView>
                            <Text style={styles.text}>Scrollable box containing the critical information
                                LAO's name, and the orgnizers' and witnesses' names and hex fingerprint

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vitae egestas ex, et rhoncus nibh. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Aliquam iaculis elit libero, id lacinia quam vestibulum vitae. Integer tristique non est ac feugiat. Phasellus ac sapien eu ante sodales auctor et id ex. Etiam fringilla pulvinar dui ullamcorper fermentum. Sed luctus lacus vel hendrerit tempus. Vivamus vitae posuere nibh, eleifend semper risus. Mauris sit amet nunc nec risus volutpat semper et a tortor. Donec arcu nisi, pellentesque nec arcu vitae, efficitur molestie tellus. In in felis bibendum orci consectetur sagittis. Phasellus nec faucibus sem. Ut sagittis lorem non tellus luctus, ac lacinia lectus pretium.

Integer vitae aliquet lorem. Etiam non erat venenatis, venenatis ante et, efficitur ligula. Etiam et pellentesque erat, at fringilla elit. Aliquam facilisis tortor eget metus rhoncus mattis. Sed luctus velit quis enim scelerisque, quis elementum purus cursus. Proin venenatis commodo mi ac sodales. Cras in pretium tellus.

Duis sollicitudin, urna a tempor dapibus, dui nisl rhoncus dolor, et pretium quam dolor id velit. Donec vitae augue sollicitudin neque ultrices ultrices aliquam vel turpis. Sed quis risus luctus, volutpat libero vel, placerat neque. Nunc luctus malesuada eros, at accumsan lacus vehicula at. Duis laoreet placerat vehicula. Phasellus pulvinar eget orci eget ultrices. Cras in tincidunt libero, eget vulputate mi. Pellentesque hendrerit nibh massa, ac tincidunt lorem interdum a. Etiam a sodales justo. Ut ut ipsum eget lacus finibus tristique quis sit amet turpis. Nulla suscipit, nunc ut accumsan laoreet, felis tellus venenatis magna, a malesuada tortor risus et odio. Nulla vehicula libero ut elit lacinia pretium.

Nunc consectetur pharetra tortor, ut elementum quam dapibus a. Vestibulum vel tincidunt felis. Duis dapibus elit eu suscipit sodales. Integer nec ultricies orci, at porta odio. Etiam sed sem condimentum, feugiat ex nec, bibendum nulla. Donec venenatis magna vel odio molestie porttitor. Donec maximus placerat auctor. Fusce scelerisque condimentum molestie. Duis a lorem pretium, imperdiet massa a, iaculis dolor. Nullam a nisl elementum sapien facilisis scelerisque quis in sapien. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.

Integer sit amet quam vel turpis ultricies tristique ac at mauris. Vestibulum efficitur fringilla lacus non fringilla. Quisque venenatis dui tempor, aliquam nisi ut, cursus ante. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Vestibulum facilisis sem congue sem semper consectetur. Nunc a scelerisque diam, vulputate lobortis erat. Aenean posuere faucibus consectetur. Praesent feugiat nulla porta orci auctor, a vulputate felis suscipit. Aenean vulputate ligula ac commodo ornare. 
                            </Text>
                        </ScrollView>
                    </View>
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