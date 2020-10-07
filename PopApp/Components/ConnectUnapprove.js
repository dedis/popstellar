import {StyleSheet, View, Text, Button} from 'react-native'

/*
* The unapprove component
*
* In the future will ask for camera permission
*/

class ConnectUnapprove extends React.Component {
    render() {
        return(
            <View style={styles.container}>
                <Text style={styles.text}>The easiest way to connect to a local organization is to scan its QR code</Text>
                <View style={styles.button}>
                    <Button title="Enable Camera Access" onPress={() => {this.props.navigation.navigate("Scanning");}}/>
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
        fontSize: 25,
        textAlign: "center",
        marginHorizontal: 10
    },
    button:{
        marginHorizontal: 50
    }
  });

export default ConnectUnapprove