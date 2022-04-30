import { useNavigation } from '@react-navigation/core';
import React from 'react';
import { Button, StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import { CollapsibleContainer, ParagraphBlock, QRCode } from 'core/components';
import { Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { selectCurrentLao } from '../reducer';

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

const LaoProperties = () => {
  const lao = useSelector(selectCurrentLao);
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  return lao ? (
    <>
      <View style={laoPropertiesStyles.default}>
        <CollapsibleContainer title="Lao Properties">
          <ParagraphBlock text={`Lao name: ${lao.name}`} />
          <ParagraphBlock text={`Lao creation: ${lao.creation.toDateString()}`} />
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
    </>
  ) : null;
};

export default LaoProperties;
