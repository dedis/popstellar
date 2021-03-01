import { StyleSheet } from 'react-native';
import { Colors } from 'styles';

const stylesCircularButton = StyleSheet.create({
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    height: 80,
    backgroundColor: Colors.blue,
    borderRadius: 80,
  },
});

export default stylesCircularButton;
