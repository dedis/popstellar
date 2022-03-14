import React from 'react';
import { SectionList, StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useNavigation } from '@react-navigation/native';

import PropTypes from 'prop-types';
import { TextBlock } from 'core/components';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Event } from './index';
import { eventPropTypes } from './Event';
import { EventsHooks } from '../hooks';

const styles = StyleSheet.create({
  flexBox: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  buttonMatcher: {
    ...Typography.base,
    paddingLeft: Spacing.m,
    opacity: 0,
  } as TextStyle,
  expandButton: {
    ...Typography.base,
    paddingRight: Spacing.m,
  } as TextStyle,
});

/**
 * Collapsible list of events: list with 3 sections corresponding
 * to 'past', 'present' and 'future' events.
 *
 * Nested events should be in the children value of the parent event.
 */
const EventListCollapsible = (props: IPropTypes) => {
  const { data } = props;

  const isOrganizer = EventsHooks.useIsLaoOrganizer();

  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const renderSectionHeader = (title: string) => {
    const sectionTitle = <TextBlock bold text={title} />;
    const expandSign: string = '+';

    return isOrganizer && title === 'Future' ? (
      <View style={styles.flexBox}>
        <Text style={styles.buttonMatcher}>{expandSign}</Text>
        {sectionTitle}
        <Text
          style={styles.expandButton}
          onPress={() => navigation.navigate(STRINGS.organizer_navigation_tab_create_event, {})}>
          {expandSign}
        </Text>
      </View>
    ) : (
      sectionTitle
    );
  };

  return (
    <SectionList
      sections={data}
      keyExtractor={(item) => item.id.valueOf()}
      renderItem={({ item }) => <Event event={item} />}
      renderSectionHeader={({ section: { title } }) => renderSectionHeader(title)}
    />
  );
};

const propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      data: PropTypes.arrayOf(eventPropTypes).isRequired,
    }).isRequired,
  ).isRequired,
};
EventListCollapsible.propTypes = propTypes;

EventListCollapsible.defaultProps = {};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventListCollapsible;
