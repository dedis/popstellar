import { useNavigation } from '@react-navigation/core';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, View } from 'react-native';
import { useSelector } from 'react-redux';

import { CollapsibleContainer, ParagraphBlock, QRCode } from 'core/components';
import { ConnectToLao } from 'features/connect/objects';
import STRINGS from 'resources/strings';

import { selectCurrentLao } from '../reducer';
import laoPropertiesStyles from '../styles/laoPropertiesStyles';

const LaoProperties = ({ url }: IPropTypes) => {
  const lao = useSelector(selectCurrentLao);
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  if (!lao) {
    return null;
  }

  const creationDateString = lao.creation.toDateString();
  const connectToLao = new ConnectToLao({
    server: url,
    lao: lao.id.toString(),
  });

  return (
    <>
      <View style={laoPropertiesStyles.default}>
        <CollapsibleContainer title="Lao Properties">
          <ParagraphBlock text={`Lao name: ${lao.name}`} />
          <ParagraphBlock text={`Lao creation: ${creationDateString}`} />
          <QRCode value={JSON.stringify(connectToLao)} visibility />
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
  );
};

const propTypes = {
  url: PropTypes.string.isRequired,
};
LaoProperties.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaoProperties;
