import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  flex: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
  },
  anchoredCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-evenly',
  },
  topCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-start',
  },
});

export default styles;
