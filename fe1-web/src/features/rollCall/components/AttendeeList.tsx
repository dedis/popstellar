import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { PoPIcon, PoPTextButton } from 'core/components';
import { PublicKey } from 'core/objects';
import { Color, Icon, List, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

const styles = StyleSheet.create({
  attendeeAddButtonContainer: {
    marginTop: Spacing.x1,
  } as ViewStyle,
});

const AttendeeList = ({ popTokens, isOrganizer, onAddAttendee }: IPropTypes) => {
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
      {isOrganizer && onAddAttendee && (
        <View style={styles.attendeeAddButtonContainer}>
          <PoPTextButton onPress={onAddAttendee}>{STRINGS.roll_call_scan_attendees}</PoPTextButton>
        </View>
      )}
    </View>
  );
};

const propTypes = {
  popTokens: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.instanceOf(PublicKey).isRequired).isRequired,
    PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  ]).isRequired,
  isOrganizer: PropTypes.bool,
  onAddAttendee: PropTypes.func,
};
AttendeeList.propTypes = propTypes;

AttendeeList.defaultProps = {
  isOrganizer: false,
  onAddAttendee: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default AttendeeList;
