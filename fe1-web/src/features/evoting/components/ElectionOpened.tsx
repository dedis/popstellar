import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import ReactTimeago from 'react-timeago';

import { PoPIcon, PoPTextButton } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { Border, Color, Icon, List, Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { castVote, terminateElection } from '../network/ElectionMessageApi';
import { Election, ElectionVersion, SelectedBallots } from '../objects';
import { makeElectionKeySelector } from '../reducer';

const styles = StyleSheet.create({
  warning: {
    padding: Spacing.x1,
    marginBottom: Spacing.x1,
    borderRadius: Border.radius,

    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  warningText: {
    flex: 1,
    marginLeft: Spacing.x1,
  } as ViewStyle,
  openBallot: {
    backgroundColor: Color.warning,
  } as ViewStyle,
  secretBallot: {
    backgroundColor: Color.success,
  } as ViewStyle,
  questionList: {
    marginBottom: Spacing.x1,
  } as ViewStyle,
});

/**
 * Screen component for opened elections
 */
const ElectionOpened = ({ election }: IPropTypes) => {
  const toast = useToast();

  const [selectedBallots, setSelectedBallots] = useState<SelectedBallots>({});
  const [isQuestionOpen, setIsQuestionOpen] = useState(
    election.questions.reduce((obj, question) => {
      // this makes the reduce efficient. creating a new object
      // in every iteration is not necessary
      // eslint-disable-next-line no-param-reassign
      obj[question.id] = true;
      return obj;
    }, {} as Record<string, boolean | undefined>),
  );

  const electionKeySelector = useMemo(
    () => makeElectionKeySelector(election.id.valueOf()),
    [election.id],
  );
  const electionKey = useSelector(electionKeySelector);

  const canCastVote = !!(election.version !== ElectionVersion.SECRET_BALLOT || electionKey);

  const onCastVote = () => {
    console.log('Casting Vote');
    castVote(election, electionKey || undefined, selectedBallots)
      .then(() => {
        toast.show(STRINGS.cast_vote_success, {
          type: 'success',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      })
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
        toast.show(`Could not cast Vote, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  if (!canCastVote) {
    return (
      <ScreenWrapper>
        <Text style={Typography.paragraph}>{STRINGS.election_wait_for_election_key}</Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      {election.version === ElectionVersion.OPEN_BALLOT && (
        <View style={[styles.warning, styles.openBallot]}>
          <PoPIcon name="warning" color={Color.contrast} size={Icon.largeSize} />
          <View style={styles.warningText}>
            <Text style={[Typography.base, Typography.important, Typography.negative]}>
              Warning
            </Text>
            <Text style={[Typography.base, Typography.negative]}>
              {STRINGS.election_warning_open_ballot}
            </Text>
          </View>
        </View>
      )}

      {election.version === ElectionVersion.SECRET_BALLOT && (
        <View style={[styles.warning, styles.secretBallot]}>
          <PoPIcon name="info" color={Color.contrast} size={Icon.largeSize} />
          <View style={styles.warningText}>
            <Text style={[Typography.base, Typography.important, Typography.negative]}>Notice</Text>
            <Text style={[Typography.base, Typography.negative]}>
              {STRINGS.election_info_secret_ballot}
            </Text>
          </View>
        </View>
      )}

      <Text style={Typography.paragraph}>
        <Text style={[Typography.base, Typography.important]}>{STRINGS.general_ending_at}</Text>
        {'\n'}
        <Text>
          <ReactTimeago date={election.end.valueOf() * 1000} />
        </Text>
      </Text>

      <Text style={[Typography.paragraph, Typography.important]}>Questions</Text>

      <View style={[List.container, styles.questionList]}>
        {election.questions.map((question, questionIndex) => (
          <ListItem.Accordion
            key={question.id}
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
                [question.id]: !isQuestionOpen[question.id],
              })
            }
            isExpanded={!!isQuestionOpen[question.id]}>
            {question.ballot_options.map((ballotOption, ballotOptionIndex) => {
              const listStyle = List.getListItemStyles(
                ballotOptionIndex === 0,
                ballotOptionIndex === question.ballot_options.length - 1,
              );

              if (!isQuestionOpen[question.id]) {
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
                  <View style={List.icon}>
                    <ListItem.CheckBox
                      testID={`questions_${questionIndex}_ballots_option_${ballotOptionIndex}_checkbox`}
                      size={Icon.size}
                      checked={selectedBallots[questionIndex] === ballotOptionIndex}
                      onPress={onPress}
                    />
                  </View>
                  <ListItem.Content>
                    <ListItem.Title style={Typography.base}>{ballotOption}</ListItem.Title>
                  </ListItem.Content>
                </ListItem>
              );
            })}
          </ListItem.Accordion>
        ))}
      </View>

      <PoPTextButton testID="election_vote_selector" onPress={onCastVote}>
        {STRINGS.cast_vote}
      </PoPTextButton>
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionOpened.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionOpened;

/**
 * Component that is rendered in the top right of the navigation bar for opened elections.
 * Allows for example to show icons that then trigger different actions
 */
export const ElectionOpenedRightHeader = (props: RightHeaderIPropTypes) => {
  const { election, isOrganizer } = props;

  const showActionSheet = useActionSheet();
  const toast = useToast();

  const onTerminateElection = () => {
    console.log('Terminating Election');
    terminateElection(election)
      .then(() => console.log('Election Terminated'))
      .catch((err) => {
        console.error('Could not terminate election, error:', err);
        toast.show(`Could not terminate election, error: ${err}`, {
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
    <PoPTouchableOpacity
      testID="election_opened_option_selector"
      onPress={() =>
        showActionSheet([
          {
            displayName: STRINGS.election_end,
            action: onTerminateElection,
          },
        ])
      }>
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

const rightHeaderPropTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  isOrganizer: PropTypes.bool,
};

type RightHeaderIPropTypes = PropTypes.InferProps<typeof rightHeaderPropTypes>;

ElectionOpenedRightHeader.propTypes = rightHeaderPropTypes;

ElectionOpenedRightHeader.defaultProps = {
  isOrganizer: false,
};
