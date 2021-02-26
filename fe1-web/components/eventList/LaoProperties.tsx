import React, { useState } from 'react';
import {
  TouchableOpacity, View, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { Spacing } from 'styles';
import TextBlock from 'components/TextBlock';
import styleEventView from 'styles/stylesheets/eventView';
import ListCollapsibleIcon from 'components/eventList/ListCollapsibleIcon';
import { connect } from 'react-redux';
import ParagraphBlock from 'components/ParagraphBlock';
import { Lao } from 'model/objects';

function buildLaoProperties(lao: Lao) {
  return (
    <>
      <ParagraphBlock text={`Lao name: ${lao.name}`} />
      <ParagraphBlock text={`Lao creation: ${lao.creation.toString()}`} />
    </>
  );
}

const LaoProperties = (props: IPropTypes) => {
  const { lao } = props;
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

const propTypes = {
  lao: PropTypes.instanceOf(Lao),
};
LaoProperties.propTypes = propTypes;

LaoProperties.defaultProps = {
  lao: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapStateToProps = (state: any) => ({
  lao: Lao.fromState(state.openedLao),
});

export default connect(mapStateToProps)(LaoProperties);
