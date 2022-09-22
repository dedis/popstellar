import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { ListItem } from 'react-native-elements';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { EventHooks } from '../hooks';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_home>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const EventListItem = (props: IPropTypes) => {
  const { eventId, eventType, isFirstItem, isLastItem, testID } = props;

  const navigation = useNavigation<NavigationProps['navigation']>();

  const isOrganizer = EventHooks.useIsLaoOrganizer();
  const eventTypes = EventHooks.useEventTypes();

  const EventType = useMemo(() => {
    return eventTypes.find((c) => c.eventType === eventType);
  }, [eventType, eventTypes]);

  const listStyle = List.getListItemStyles(isFirstItem, isLastItem);

  return EventType ? (
    <ListItem
      containerStyle={listStyle}
      style={listStyle}
      bottomDivider
      testID={testID || undefined}
      onPress={() =>
        navigation.push(STRINGS.navigation_app_lao, {
          screen: STRINGS.navigation_lao_events,
          params: {
            screen: EventType.navigationNames.screenSingle,
            params: {
              eventId: eventId,
              isOrganizer,
            },
          },
        })
      }
      hasTVPreferredFocus
      tvParallaxProperties>
      <EventType.ListItemComponent eventId={eventId} isOrganizer={isOrganizer} />
    </ListItem>
  ) : (
    <ListItem
      containerStyle={[List.item, List.firstItem, List.lastItem]}
      bottomDivider
      hasTVPreferredFocus
      tvParallaxProperties>
      <ListItem.Content>
        <ListItem.Title
          style={Typography.base}>{`Event type '${eventType}' was not registered!`}</ListItem.Title>
      </ListItem.Content>
    </ListItem>
  );
};

const propTypes = {
  eventId: PropTypes.string.isRequired,
  eventType: PropTypes.string.isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
  testID: PropTypes.string,
};
EventListItem.propTypes = propTypes;

EventListItem.defaultProps = {
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventListItem;
