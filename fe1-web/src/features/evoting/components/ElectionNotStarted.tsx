import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { TouchableOpacity } from 'react-native-gesture-handler';
import { useToast } from 'react-native-toast-notifications';

import OptionsIcon from 'core/components/icons/OptionsIcon';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { Color, Icon, List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { openElection } from '../network/ElectionMessageApi';
import { Election } from '../objects';

const ElectionNotStarted = ({ election }: IPropTypes) => {
  const [isQuestionOpen, setIsQuestionOpen] = useState(
    election.questions.reduce((obj, question) => {
      // this makes the reduce efficient. creating a new object
      // in every iteration is not necessary
      // eslint-disable-next-line no-param-reassign
      obj[question.id] = true;
      return obj;
    }, {} as Record<string, boolean | undefined>),
  );

  return (
    <ScreenWrapper>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.general_starting_at}</Text>
        {'\n'}
        <Text>
          {election.start.toDate().toLocaleDateString()}{' '}
          {election.start.toDate().toLocaleTimeString()}
        </Text>
      </Text>

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.general_ending_at}</Text>
        {'\n'}
        <Text>
          {election.end.toDate().toLocaleDateString()} {election.end.toDate().toLocaleTimeString()}
        </Text>
      </Text>

      <Text style={[Typography.paragraph, Typography.important]}>Questions</Text>

      <View style={List.container}>
        {election.questions.map((question) => (
          <ListItem.Accordion
            key={question.id}
            containerStyle={List.accordionItem}
            style={List.accordionItem}
            content={
              <ListItem.Content>
                <ListItem.Title style={[Typography.base, Typography.important]}>
                  {question.question}
                </ListItem.Title>
              </ListItem.Content>
            }
            onPress={() =>
              setIsQuestionOpen({ ...isQuestionOpen, [question.id]: !isQuestionOpen[question.id] })
            }
            isExpanded={!!isQuestionOpen[question.id]}>
            {question.ballot_options.map((ballotOption, idx) => {
              const listStyles = List.getListItemStyles(
                idx === 0,
                idx === question.ballot_options.length - 1,
              );

              return (
                <ListItem key={ballotOption} containerStyle={listStyles} style={listStyles}>
                  <View style={List.iconPlaceholder} />
                  <ListItem.Content>
                    <ListItem.Title style={Typography.base}>{ballotOption}</ListItem.Title>
                  </ListItem.Content>
                </ListItem>
              );
            })}
          </ListItem.Accordion>
        ))}
      </View>
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionNotStarted.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionNotStarted;

export const ElectionNotStartedRightHeader = (props: RightHeaderIPropTypes) => {
  const { election, isOrganizer } = props;

  const showActionSheet = useActionSheet();
  const toast = useToast();

  const onOpenElection = () => {
    console.log('Opening Election');
    openElection(election)
      .then(() => console.log('Election Opened'))
      .catch((err) => {
        console.error('Could not open election, error:', err);
        toast.show(`Could not open election, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  // don't show a button for non-organizers
  if (!isOrganizer) {
    return null;
  }

  return (
    <TouchableOpacity
      onPress={() => showActionSheet([{ displayName: 'Open Election', action: onOpenElection }])}>
      <OptionsIcon color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};

const rightHeaderPropTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool,
};

type RightHeaderIPropTypes = PropTypes.InferProps<typeof rightHeaderPropTypes>;

ElectionNotStartedRightHeader.propTypes = rightHeaderPropTypes;

ElectionNotStartedRightHeader.defaultProps = {
  isOrganizer: false,
};
