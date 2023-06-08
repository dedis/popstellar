import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { PoPIcon } from 'core/components';
import { PublicKey } from 'core/objects';
import { Color, Icon, List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

const styles = StyleSheet.create({
  tokenHighlight: {
    backgroundColor: Color.lightPopBlue,
  } as ViewStyle,
});

const AttendeeList = ({ popTokens, personalToken }: IPropTypes) => {
  const [showItems, setShowItems] = useState(true);
  const toast = useToast();

  // check if the attendee list is sorted alphabetically
  useEffect(() => {
    if (popTokens) {
      const isAttendeeListSorted = popTokens.reduce<[boolean, PublicKey]>(
        ([isSorted, lastValue], currentValue) => [
          isSorted && lastValue.valueOf().localeCompare(currentValue.valueOf()) <= 0,
          currentValue,
        ],
        [true, new PublicKey('')],
      );

      if (!isAttendeeListSorted[0]) {
        toast.show(STRINGS.roll_call_danger_attendee_list_not_sorted, {
          type: 'warning',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      }
    }
  }, [popTokens, toast]);

  return (
    <View style={List.container}>
      <ListItem.Accordion
        containerStyle={List.accordionItem}
        content={
          <ListItem.Content>
            <ListItem.Title style={[Typography.base, Typography.important]}>
              {STRINGS.roll_call_attendees}
            </ListItem.Title>
          </ListItem.Content>
        }
        isExpanded={showItems}
        onPress={() => setShowItems(!showItems)}>
        {popTokens.map((token, idx) => {
          const listStyle = List.getListItemStyles(idx === 0, idx === popTokens.length - 1);
          const isPersonalToken = personalToken !== undefined && token.valueOf() === personalToken;

          return (
            <ListItem
              key={token.valueOf()}
              containerStyle={[listStyle, isPersonalToken && styles.tokenHighlight]}
              style={listStyle}>
              <View style={List.icon}>
                <PoPIcon name="qrCode" color={Color.primary} size={Icon.size} />
              </View>
              <ListItem.Content>
                <ListItem.Title
                  style={[Typography.base, Typography.code]}
                  numberOfLines={1}
                  selectable>
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
  personalToken: PropTypes.string,
};

AttendeeList.defaultProps = {
  personalToken: undefined,
};

AttendeeList.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default AttendeeList;
