import React from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import { makeLaosList } from 'store';
import { Lao } from 'model/objects';

import { Spacing } from 'styles';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import { sign } from 'tweetnacl';
import LAOItem from 'components/LAOItem';
import TextBlock from 'components/TextBlock';
import { encodeBase64 } from 'tweetnacl-util';
import WideButtonView from '../components/WideButtonView';
import { WalletCryptographyHandler } from '../model/objects/WalletCryptographyHandler';

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

const token = sign.keyPair().secretKey;
let cypher: ArrayBuffer = new ArrayBuffer(0);
const cryptoManager = new WalletCryptographyHandler();

const onWalletCryptoHandlerButtonPressed = async () => {
  console.log('--------------------------------- Creation of the secret key database ---------------------------------');
  await cryptoManager.initWalletStorage();
  console.log('---------------------------------        Key database created         ---------------------------------');
  console.log('');
};

const onEncryptToken = async () => {
  console.log('---------------------------------      Encryption/Decryption test     ---------------------------------');
  console.log(`ed25519 test-key to encrypt : \n ${encodeBase64(token).toString()}`);
  cypher = await cryptoManager.encrypt(token);
  console.log('Encrypted Token');
  console.log(cypher);
};

const onDecryptToken = async () => {
  const plaintext = await cryptoManager.decrypt(cypher);
  console.log('Decrypted Token');
  console.log(encodeBase64(new Uint8Array(plaintext)));
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
        onPress={() => onWalletCryptoHandlerButtonPressed()}
      />
      <WideButtonView
        title={STRINGS.walletEncryptRandomToken}
        onPress={() => onEncryptToken()}
      />
      <WideButtonView
        title={STRINGS.walletDecryptRandomToken}
        onPress={() => onDecryptToken()}
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
