import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useCallback, useMemo, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import ReactTimeago from 'react-timeago';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { ToolbarItem } from 'core/components/Toolbar';
import { Timestamp } from 'core/objects';
import { Icon, List, Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { castVote, terminateElection } from '../network/ElectionMessageApi';
import { Election, ElectionVersion, SelectedBallots } from '../objects';
import { makeElectionKeySelector } from '../reducer';
import ElectionVersionNotice from './ElectionVersionNotice';

const styles = StyleSheet.create({
  questionList: {
    marginBottom: Spacing.x1,
  } as ViewStyle,
});

/**
 * Screen component for opened elections
 */
const ElectionOpened = ({ election, isConnected, isOrganizer }: IPropTypes) => {
  const toast = useToast();

  const [selectedBallots, setSelectedBallots] = useState<SelectedBallots>({});
  const [isQuestionOpen, setIsQuestionOpen] = useState(
    election.questions.reduce((obj, question) => {
      // this makes the reduce efficient. creating a new object
      // in every iteration is not necessary
      // eslint-disable-next-line no-param-reassign
      obj[question.id.toString()] = true;
      return obj;
    }, {} as Record<string, boolean | undefined>),
  );

  const electionKeySelector = useMemo(() => makeElectionKeySelector(election.id), [election.id]);
  const electionKey = useSelector(electionKeySelector);

  const canCastVote = !!(election.version !== ElectionVersion.SECRET_BALLOT || electionKey);

  const onTerminateElection = useCallback(() => {
    console.log('Terminating Election');
    terminateElection(election)
      .then(() => console.log('Election Terminated'))
      .catch((err) => {
        console.error('Could not terminate election, error:', err);
        toast.show(`Could not terminate election, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      });
  }, [toast, election]);

  const onCastVote = useCallback(() => {
    console.log('Casting Vote');
    castVote(election, electionKey || undefined, selectedBallots)
      .then(() => {
        toast.show(STRINGS.cast_vote_success, {
          type: 'success',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      })
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
        toast.show(`Could not cast Vote, error: ${err}`, {
          type: 'danger',
          placement: 'bottom',
          duration: FOUR_SECONDS,
        });
      });
  }, [toast, election, electionKey, selectedBallots]);

  const toolbarItems: ToolbarItem[] = useMemo(() => {
    if (!isOrganizer) {
      return [
        {
          id: 'election_vote_selector',
          title: STRINGS.cast_vote,
          onPress: onCastVote,
        },
      ] as ToolbarItem[];
    }

    return [
      {
        id: 'election_opened_option_selector',
        title: STRINGS.election_end,
        onPress: onTerminateElection,
        buttonStyle: 'secondary',
        disabled: isConnected !== true,
      },
      {
        id: 'election_vote_selector',
        title: STRINGS.cast_vote,
        onPress: onCastVote,
        disabled: isConnected !== true,
      },
    ] as ToolbarItem[];
  }, [isConnected, isOrganizer, onTerminateElection, onCastVote]);

  if (!canCastVote) {
    return (
      <ScreenWrapper>
        <Text style={Typography.paragraph}>{STRINGS.election_wait_for_election_key}</Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper toolbarItems={toolbarItems}>
      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{election.name}</Text>
        {'\n'}
        {Timestamp.EpochNow().before(election.end) ? (
          <Text>
            {STRINGS.general_ending} <ReactTimeago live date={election.end.toDate()} />
          </Text>
        ) : (
          <Text>{STRINGS.general_ending_now}</Text>
        )}
      </Text>

      <ElectionVersionNotice election={election} />

      <View style={[List.container, styles.questionList]}>
        {election.questions.map((question, questionIndex) => {
          const questionId = question.id.toString();

          return (
            <ListItem.Accordion
              key={questionId}
              containerStyle={List.accordionItem}
              content={
                <ListItem.Content>
                  <ListItem.Title style={[Typography.base, Typography.important]}>
                    {question.question}
                  </ListItem.Title>
                </ListItem.Content>
              }
              onPress={() =>
                setIsQuestionOpen({
                  ...isQuestionOpen,
                  [questionId]: !isQuestionOpen[questionId],
                })
              }
              isExpanded={!!isQuestionOpen[questionId]}>
              {question.ballot_options.map((ballotOption, ballotOptionIndex) => {
                const listStyle = List.getListItemStyles(
                  ballotOptionIndex === 0,
                  ballotOptionIndex === question.ballot_options.length - 1,
                );

                if (!isQuestionOpen[questionId]) {
                  listStyle.push(List.hiddenItem);
                }

                const onPress = () => {
                  setSelectedBallots({
                    ...selectedBallots,
                    [questionIndex]: ballotOptionIndex,
                  });
                };

                return (
                  <ListItem
                    key={ballotOption}
                    containerStyle={listStyle}
                    style={listStyle}
                    onPress={onPress}>
                    <ListItem.CheckBox
                      testID={`questions_${questionIndex}_ballots_option_${ballotOptionIndex}_checkbox`}
                      size={Icon.size}
                      checked={selectedBallots[questionIndex] === ballotOptionIndex}
                      onPress={() =>
                        setSelectedBallots({
                          ...selectedBallots,
                          [questionIndex]: ballotOptionIndex,
                        })
                      }
                    />
                    <ListItem.Content>
                      <ListItem.Title style={Typography.base}>{ballotOption}</ListItem.Title>
                    </ListItem.Content>
                  </ListItem>
                );
              })}
            </ListItem.Accordion>
          );
        })}
      </View>
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isConnected: PropTypes.bool,
  isOrganizer: PropTypes.bool.isRequired,
};
ElectionOpened.propTypes = propTypes;

ElectionOpened.defaultProps = {
  isConnected: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionOpened;
