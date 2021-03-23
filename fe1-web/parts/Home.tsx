import React from 'react';
import { StyleSheet, View, FlatList } from 'react-native';
import { useSelector } from 'react-redux';

import { makeLaosList } from 'store';
import { Lao } from 'model/objects';

import { Spacing } from 'styles';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';

import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';
import WideButtonView from '../components/WideButtonView';
import { Wallet } from '../model/objects/Wallet';

/**
 * Manage the Home screen component: if the user is not connected to any LAO, a welcome message
 * is displayed, otherwise a list available previously connected LAOs is displayed instead
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
*/
const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.s,
  },
});

let wallet: Wallet;

const onWalletButtonPressed = async () => {
  console.log('Creation of wallet object');
  wallet = new Wallet();
  console.log('Wallet created');
};

const onAddKeyPairButtonPressed = async () => {
  await wallet.addEncryptionKey();
};

// FIXME: define interface + types, requires availableLaosReducer to be migrated first
function getConnectedLaosDisplay(laos: Lao[]) {
  return (
    <View style={styleContainer.centered}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LAOItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
}

function getWelcomeMessageDisplay() {
  return (
    <View style={styleContainer.centered}>
      <TextBlock bold text={STRINGS.home_welcome} />
      <TextBlock bold text={STRINGS.home_connect_lao} />
      <TextBlock bold text={STRINGS.home_launch_lao} />
      <TextBlock text={' '} />
      <TextBlock text={' '} />
      <TextBlock text={' '} />
      <WideButtonView
        title={STRINGS.wallet}
        onPress={() => onWalletButtonPressed()}
      />
      <WideButtonView
        title={STRINGS.walletAdd}
        onPress={() => onAddKeyPairButtonPressed()}
      />
    </View>
  );
}

const Home = () => {
  const laosList = makeLaosList();
  const laos: Lao[] = useSelector(laosList);

  return (laos && !laos.length)
    ? getConnectedLaosDisplay(laos)
    : getWelcomeMessageDisplay();
};

export default Home;
