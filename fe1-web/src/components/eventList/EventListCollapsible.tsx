import React from 'react';
import {
  SectionList, StyleSheet, Text, TextStyle, View, ViewStyle,
} from 'react-native';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import { makeIsLaoOrganizer } from 'store';

import { Spacing, Typography } from 'styles';
import STRINGS from 'res/strings';

import * as RootNavigation from 'navigation/RootNavigation';
import TextBlock from 'components/TextBlock';
import Event from './events';

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

function renderSectionHeader(title: string, isOrganizer: boolean) {
  const sectionTitle = <TextBlock bold text={title} />;
  const expandSign: string = '+';

  return (isOrganizer && title === 'Future')
    ? (
      <View style={styles.flexBox}>
        <Text style={styles.buttonMatcher}>{expandSign}</Text>
        { sectionTitle }
        <Text
          style={styles.expandButton}
          onPress={() => RootNavigation.navigate(STRINGS.organizer_navigation_tab_create_event, {})}
        >
          {expandSign}
        </Text>
      </View>
    )
    : sectionTitle;
}

/**
 * Collapsible list of events: list with 3 sections corresponding
 * to 'past', 'present' and 'future' events
 *
 * Nested events should be in the children value of the parent event
*/
const EventListCollapsible = (props: IPropTypes) => {
  const { data } = props;

  const isOrganizerSelect = makeIsLaoOrganizer();
  const isOrganizer = useSelector(isOrganizerSelect);

  const renderItemFn = (
    ({ item }: any) => <Event event={item} isOrganizer={isOrganizer} renderItemFn={renderItemFn} />
  );

  return (
    <SectionList
      sections={data}
      keyExtractor={(item) => `${item.id}`}
      renderItem={renderItemFn}
      renderSectionHeader={({ section: { title } }) => renderSectionHeader(title, !!isOrganizer)}
    />
  );
};

const propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PropTypes.shape({})),
  }).isRequired).isRequired,
};
EventListCollapsible.propTypes = propTypes;

EventListCollapsible.defaultProps = {
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventListCollapsible;
