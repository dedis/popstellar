import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

import { PoPIcon } from 'core/components';
import { PublicKey } from 'core/objects';
import { Color, Icon, List, Typography } from 'core/styles';

const AttendeeList = ({ popTokens }: IPropTypes) => {
  return (
    <View style={List.container}>
      <ListItem.Accordion
        containerStyle={List.accordionItem}
        content={
          <ListItem.Content>
            <ListItem.Title style={[Typography.base, Typography.important]}>
              Attendees
            </ListItem.Title>
          </ListItem.Content>
        }
        isExpanded>
        {popTokens.map((token, idx) => {
          const listStyle = List.getListItemStyles(idx === 0, idx === popTokens.length - 1);

          return (
            <ListItem key={token.valueOf()} containerStyle={listStyle} style={listStyle}>
              <View style={List.icon}>
                <PoPIcon name="qrCode" color={Color.primary} size={Icon.size} />
              </View>
              <ListItem.Content>
                <ListItem.Title style={Typography.base} numberOfLines={1} selectable>
                  {token.valueOf()}
                </ListItem.Title>
              </ListItem.Content>
            </ListItem>
          );
        })}
      </ListItem.Accordion>
    </View>
  );
};

const propTypes = {
  popTokens: PropTypes.arrayOf(PropTypes.instanceOf(PublicKey).isRequired).isRequired,
};
AttendeeList.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default AttendeeList;
