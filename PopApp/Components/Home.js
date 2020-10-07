import React from 'react'
import {StyleSheet, View, Text} from 'react-native'


/*
* The Home component
*
* Manage the Home screen
*/
class Home extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>Welcome to Personhood.Online! {'\n'}</Text>
                <Text style={styles.text}>To connect to a local organization (LAO), please tap to Connect above. {'\n'}</Text>
                <Text style={styles.text}>To launch a new LAO as on organizer, please tap Launch tab above. {'\n'}</Text>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
    },
    text: {
        textAlign: 'center',
        fontSize: 25,
        fontWeight: 'bold',
        marginHorizontal: 15
    },
  });

export default Home