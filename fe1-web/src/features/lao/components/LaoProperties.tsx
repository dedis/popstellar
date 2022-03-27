import { useNavigation } from '@react-navigation/core';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, StyleSheet, View } from 'react-native';
import { useStore } from 'react-redux';

import { CollapsibleContainer, ParagraphBlock, QRCode } from 'core/components';
import { getKeyPairState } from 'core/keypair';
import { PublicKey } from 'core/objects';
import { Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';

const laoPropertiesStyles = StyleSheet.create({
  default: {
    borderWidth: 1,
    borderRadius: 5,
    padding: Spacing.xs,
    marginHorizontal: Spacing.s,
    marginTop: Spacing.xs,
    marginBottom: Spacing.xs,
  },
});

const getUserRole = (isOrganizer: boolean, isWitness: boolean): string => {
  if (isOrganizer) {
    return STRINGS.user_role_organizer;
  }

  if (isWitness) {
    return STRINGS.user_role_witness;
  }

  return STRINGS.user_role_attendee;
};

const LaoProperties = ({ isInitiallyOpen }: IPropTypes) => {
  const lao = LaoHooks.useCurrentLao();
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  const store = useStore();

  const publicKeyRaw = getKeyPairState(store.getState()).keyPair?.publicKey;
  const publicKey = publicKeyRaw ? new PublicKey(publicKeyRaw) : undefined;

  const isOrganizer = !!(publicKey && publicKey.equals(lao.organizer));

  const isWitness = !!(publicKey && lao.witnesses.some((w) => publicKey.equals(w)));

  return (
    <View style={laoPropertiesStyles.default}>
      <CollapsibleContainer title="Lao Properties" isInitiallyOpen={isInitiallyOpen}>
        <ParagraphBlock text={`Lao name: ${lao.name}`} />
        <ParagraphBlock text={`Lao creation: ${lao.creation.toDateString()}`} />
        <ParagraphBlock text={`Your role: ${getUserRole(isOrganizer, isWitness)}`} />
        <QRCode
          value={encodeLaoConnection(lao.server_addresses[0] || '', lao.id.toString())}
          visibility
        />
        <Button
          title="Add connection"
          onPress={() =>
            navigation.navigate(STRINGS.navigation_tab_connect, {
              screen: STRINGS.connect_scanning_title,
            })
          }
        />
      </CollapsibleContainer>
    </View>
  );
};

const propTypes = {
  isInitiallyOpen: PropTypes.bool,
};

LaoProperties.propTypes = propTypes;
LaoProperties.defaultProps = {
  isInitiallyOpen: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaoProperties;
