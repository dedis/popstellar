import React from 'react'
import {StyleSheet, Image, View, Button} from 'react-native'
import { TouchableOpacity } from 'react-native-gesture-handler'

import { Colors } from '../Styles'

/**
* Camera button
* use action parameter to difine the onPress action
*/

class CameraButton extends React.Component {
    render() {

        return(
            <View>
                <Button title="test" onPress={() => console.log(this.props)} />
                <TouchableOpacity style={styles.button} onPress={() => {console.log(this)} }>
                    <Image style={styles.icon} source={require("../res/img/ic_camera.png")}/>
                </TouchableOpacity>
            </View>
        )
    }
}

const styles = StyleSheet.create({
    icon:{
        width: 64,
        height: 64,
    },
    button: {
        justifyContent: "center",
        alignItems: "center",
        width: 80,
        height: 80,
        backgroundColor: Colors.blue,
        borderRadius: 80,
    }
  });

export default CameraButton