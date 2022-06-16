import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { ListItem } from 'react-native-elements';

import { List, Typography } from '../styles';

const CollapsibleContainer = ({ title, isInitiallyOpen, children }: IPropTypes) => {
  const [showChildren, setShowChildren] = useState<boolean>(!!isInitiallyOpen);

  return (
    <ListItem.Accordion
      containerStyle={List.accordionItem}
      style={List.accordionItem}
      content={
        <ListItem.Content>
          <ListItem.Title style={[Typography.base, Typography.important]}>{title}</ListItem.Title>
        </ListItem.Content>
      }
      isExpanded={showChildren}
      onPress={() => setShowChildren(!showChildren)}>
      {showChildren && children}
    </ListItem.Accordion>
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
