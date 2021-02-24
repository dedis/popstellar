import React, { useState } from 'react';
import {
  TouchableOpacity, View, ViewStyle,
} from 'react-native';
import { useSelector } from 'react-redux';

import { makeCurrentLao } from 'store';
import { Lao } from 'model/objects';

import { Spacing } from 'styles';
import styleEventView from 'styles/stylesheets/eventView';

import TextBlock from 'components/TextBlock';
import ParagraphBlock from 'components/ParagraphBlock';
import ListCollapsibleIcon from './ListCollapsibleIcon';

function buildLaoProperties(lao: Lao) {
  return (
    <>
      <ParagraphBlock text={`Lao name: ${lao.name}`} />
      <ParagraphBlock text={`Lao creation: ${lao.creation.toString()}`} />
    </>
  );
}

const LaoProperties = () => {
  const currentLao = makeCurrentLao();
  const lao = useSelector(currentLao);
  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const toggleChildren = () => (setToggleChildrenVisible(!toggleChildrenVisible));

  return (
    <>
      <TextBlock bold text="Lao Properties" />
      <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
        <TouchableOpacity onPress={toggleChildren} style={{ textAlign: 'right' } as ViewStyle}>
          <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
        </TouchableOpacity>

        { toggleChildrenVisible && lao && buildLaoProperties(lao) }
      </View>
    </>
  );
};

export default LaoProperties;
