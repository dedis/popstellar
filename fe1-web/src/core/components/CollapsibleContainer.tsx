import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, TextStyle, TouchableOpacity, View } from 'react-native';

import { Typography } from '../styles';
import TextBlock from './TextBlock';

const CollapsibleContainerStyles = StyleSheet.create({
  headerIcon: {
    position: 'absolute',
    right: 0,
  },
});

const CollapsibleContainer = ({ title, children }: IPropTypes) => {
  const [showChildren, setShowChildren] = useState(false);

  return (
    <View>
      <TouchableOpacity onPress={() => setShowChildren(!showChildren)}>
        <View>
          <TextBlock bold text={title || ''} />
          <Text style={[Typography.base as TextStyle, CollapsibleContainerStyles.headerIcon]}>
            {showChildren ? 'v' : '<'}
          </Text>
        </View>
      </TouchableOpacity>

      {showChildren && children}
    </View>
  );
};

const propTypes = {
  title: PropTypes.string,
  children: PropTypes.node,
};
CollapsibleContainer.propTypes = propTypes;

CollapsibleContainer.defaultProps = {
  title: '',
  children: null,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CollapsibleContainer;
