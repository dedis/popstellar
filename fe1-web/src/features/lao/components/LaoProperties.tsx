import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import { CollapsibleContainer, ParagraphBlock, QRCode } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';

const laoPropertiesStyles = StyleSheet.create({
  default: {
    borderWidth: 1,
    borderRadius: 5,
    padding: Spacing.x1,
    marginHorizontal: Spacing.x2,
    marginTop: Spacing.x1,
    marginBottom: Spacing.x1,
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

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const LaoProperties = ({ isInitiallyOpen }: IPropTypes) => {
  const lao = LaoHooks.useCurrentLao();

  const navigation = useNavigation<NavigationProps['navigation']>();

  const encodeLaoConnection = LaoHooks.useEncodeLaoConnectionForQRCode();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  return (
    <View style={laoPropertiesStyles.default}>
      <CollapsibleContainer title="Lao Properties" isInitiallyOpen={isInitiallyOpen}>
        <ParagraphBlock text={`Lao name: ${lao.name}`} />
        <ParagraphBlock text={`Your role: ${getUserRole(isOrganizer, isWitness)}`} />
        <QRCode
          value={encodeLaoConnection(lao.server_addresses[0] || '', lao.id.toString())}
          visibility
        />
        <Button
          title="Add connection"
          onPress={() => navigation.navigate(STRINGS.navigation_app_connect)}
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
