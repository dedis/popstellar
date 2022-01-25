import React, { useState } from 'react';
import {
  TouchableOpacity, View, ViewStyle,
} from 'react-native';
import { Spacing } from 'styles';
import TextBlock from 'components/TextBlock';
import styleEventView from 'styles/stylesheets/eventView';
import ListCollapsibleIcon from 'components/eventList/ListCollapsibleIcon';
import { useSelector } from 'react-redux';
import ParagraphBlock from 'components/ParagraphBlock';
import { Lao } from 'model/objects';
import { makeCurrentLao } from 'store/reducers';
import PropTypes from 'prop-types';
import { ConnectToLao } from 'model/objects/ConnectToLao';
import QRCode from '../QRCode';

function renderProperties(lao: Lao, url: string) {
  const creationDateString = lao.creation.timestampToString();
  const obj = new ConnectToLao({
    server: url,
    lao: lao.id.toString(),
  });
  const connectToLao = ConnectToLao.fromJson(obj);

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
      <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
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
