import React, { useState } from 'react';
import { TouchableOpacity, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import { Spacing } from 'styles';
import laoPropertiesStyles from 'styles/stylesheets/laoPropertiesStyles';
import { Lao } from 'model/objects';
import { makeCurrentLao } from 'store/reducers';
import { ConnectToLao } from 'model/objects/ConnectToLao';

import TextBlock from 'core/components/TextBlock';
import ListCollapsibleIcon from 'core/components/ListCollapsibleIcon';
import ParagraphBlock from 'core/components/ParagraphBlock';
import QRCode from 'core/components/QRCode';

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
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const toggleChildren = () => (setToggleChildrenVisible(!toggleChildrenVisible));

  return (
    <>
      <TextBlock bold text="Lao Properties" />
      <View style={[laoPropertiesStyles.default, { marginTop: Spacing.s }]}>
        <TouchableOpacity onPress={toggleChildren} style={{ textAlign: 'right' } as ViewStyle}>
          <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
        </TouchableOpacity>

        { toggleChildrenVisible && lao && renderProperties(lao, url) }
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
