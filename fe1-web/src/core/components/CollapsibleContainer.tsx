import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { Typography } from '../styles';
import TextBlock from './TextBlock';

const CollapsibleContainerStyles = StyleSheet.create({
  headerIcon: {
    position: 'absolute',
    right: 0,
  },
});

const CollapsibleContainer = ({ title, isInitiallyOpen, children }: IPropTypes) => {
  const [showChildren, setShowChildren] = useState(isInitiallyOpen);

  return (
    <View>
      <TouchableOpacity onPress={() => setShowChildren(!showChildren)}>
        <View>
          <TextBlock bold text={title || ''} />
          <Text style={[Typography.baseCentered, CollapsibleContainerStyles.headerIcon]}>
            {showChildren ? 'v' : '<'}
          </Text>
        </View>
      </TouchableOpacity>

      {showChildren && children}
    </View>
  );
};

const propTypes = {
  isInitiallyOpen: PropTypes.bool,
  title: PropTypes.string,
  children: PropTypes.node,
};
CollapsibleContainer.propTypes = propTypes;

CollapsibleContainer.defaultProps = {
  isInitiallyOpen: false,
  title: '',
  children: null,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CollapsibleContainer;
