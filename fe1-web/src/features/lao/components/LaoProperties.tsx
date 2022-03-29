import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { TouchableOpacity, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { ListCollapsibleIcon, ParagraphBlock, QRCode, TextBlock } from 'core/components';
import { Spacing } from 'core/styles';
import { ConnectToLao } from 'features/connect/objects';

import { Lao } from '../objects';
import { selectCurrentLao } from '../reducer';
import laoPropertiesStyles from '../styles/laoPropertiesStyles';

function renderProperties(lao: Lao, url: string) {
  const creationDateString = lao.creation.toDateString();
  const connectToLao = new ConnectToLao({
    server: url,
    lao: lao.id.toString(),
  });

  return (
    <>
      <ParagraphBlock text={`Lao name: ${lao.name}`} />
      <ParagraphBlock text={`Lao creation: ${creationDateString}`} />
      <QRCode value={JSON.stringify(connectToLao)} visibility />
    </>
  );
}

const LaoProperties = ({ url }: IPropTypes) => {
  const lao = useSelector(selectCurrentLao);

  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const toggleChildren = () => setToggleChildrenVisible(!toggleChildrenVisible);

  return (
    <>
      <TextBlock bold text="Lao Properties" />
      <View style={[laoPropertiesStyles.default, { marginTop: Spacing.s }]}>
        <TouchableOpacity onPress={toggleChildren} style={{ textAlign: 'right' } as ViewStyle}>
          <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
        </TouchableOpacity>

        {toggleChildrenVisible && lao && renderProperties(lao, url)}
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
