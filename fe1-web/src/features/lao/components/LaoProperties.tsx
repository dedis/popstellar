import React, { useState } from 'react';
import { TouchableOpacity, View, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';

import { Spacing } from 'core/styles';
import { ListCollapsibleIcon, ParagraphBlock, QRCode, TextBlock } from 'core/components';

import laoPropertiesStyles from '../styles/laoPropertiesStyles';
import { Lao } from '../objects';
import { selectCurrentLao } from '../reducer';
import { LaoHooks } from '../hooks';

const Properties = ({ lao, url }: { lao: Lao; url: string }) => {
  const creationDateString = lao.creation.toDateString();
  const encodedLaoConnection = LaoHooks.useEncodedLaoConnectionForQRCode(url, lao.id.toString());

  return (
    <>
      <ParagraphBlock text={`Lao name: ${lao.name}`} />
      <ParagraphBlock text={`Lao creation: ${creationDateString}`} />
      <QRCode value={encodedLaoConnection} visibility />
    </>
  );
};

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

        {toggleChildrenVisible && lao && <Properties lao={lao} url={url} />}
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
