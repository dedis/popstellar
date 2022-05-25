import { CompositeScreenProps, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';
import { SectionList, StyleSheet, Text, TextStyle, View } from 'react-native';
import { Badge } from 'react-native-elements';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';

import { CheckboxList, TimeDisplay, Button } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import BarChartDisplay from '../components/BarChartDisplay';
import { EvotingHooks } from '../hooks';
import { EvotingFeature } from '../interface';
import { castVote, openElection, terminateElection } from '../network/ElectionMessageApi';
import { ElectionStatus, QuestionResult, SelectedBallots } from '../objects';
import { makeElectionSelector } from '../reducer';

const styles = StyleSheet.create({
  text: {
    ...Typography.baseCentered,
  } as TextStyle,
  textOptions: {
    marginHorizontal: Spacing.x2,
    fontSize: 16,
    textAlign: 'center',
  } as TextStyle,
  textQuestions: {
    ...Typography.baseCentered,
    fontSize: 20,
  } as TextStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoEventsParamList, typeof STRINGS.navigation_lao_events_view_single_election>,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_lao_events>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

const ViewSingleElection = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: electionId, isOrganizer } = route.params;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find a roll call with id ${electionId}`);
  }

  const laoId = EvotingHooks.useCurrentLaoId();

  const toast = useToast();
  const questions = useMemo(
    () => election.questions.map((q) => ({ title: q.question, data: q.ballot_options })),
    [election.questions],
  );
  const [selectedBallots, setSelectedBallots] = useState<SelectedBallots>({});
  const [hasVoted, setHasVoted] = useState(0);

  const onCastVote = () => {
    castVote(laoId, election, selectedBallots)
      .then(() => setHasVoted((prev) => prev + 1))
      .catch((err) => {
        console.error('Could not cast Vote, error:', err);
        toast.show(`Could not cast Vote, error: ${err}`, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      });
  };

  const onOpenElection = () => {
    console.log('Opening Election');
    openElection(laoId, election)
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

  const onTerminateElection = () => {
    console.log('Terminating Election');
    terminateElection(laoId, election)
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

  // Here we use the election object form the redux store in order to see the electionStatus
  // update when an  incoming electionEnd or electionResult message comes
  // (in handler/ElectionHandler.ts)
  const getElectionDisplay = (status: ElectionStatus) => {
    switch (status) {
      case ElectionStatus.NOT_STARTED:
        return (
          <>
            <SectionList
              sections={questions}
              keyExtractor={(item, index) => item + index}
              renderSectionHeader={({ section: { title } }) => (
                <Text style={styles.textQuestions}>{title}</Text>
              )}
              renderItem={({ item }) => <Text style={styles.textOptions}>{`\u2022 ${item}`}</Text>}
            />
            {isOrganizer && (
              <Button onPress={onOpenElection}>
                <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                  Open election
                </Text>
              </Button>
            )}
          </>
        );
      case ElectionStatus.OPENED:
        return (
          <>
            {questions.map((q, idx) => (
              <CheckboxList
                key={q.title + idx.toString()}
                title={q.title}
                values={q.data}
                onChange={(values: number[]) =>
                  setSelectedBallots({ ...selectedBallots, [idx]: new Set(values) })
                }
              />
            ))}

            <Button onPress={onCastVote}>
              <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                {STRINGS.cast_vote}
              </Text>
            </Button>
            <Badge value={hasVoted} status="success" />
            {isOrganizer && (
              <Button onPress={onTerminateElection}>
                <Text style={[Typography.base, Typography.centered, Typography.negative]}>
                  Terminate Election / Tally Votes
                </Text>
              </Button>
            )}
          </>
        );
      case ElectionStatus.TERMINATED:
        return (
          <>
            <Text style={styles.text}>Election Terminated</Text>
            <Text style={styles.text}>Waiting for result</Text>
          </>
        );
      case ElectionStatus.RESULT:
        return (
          <>
            <Text style={styles.text}>Election Results</Text>
            {election.questionResult &&
              election.questionResult.map((questionResult: QuestionResult) => {
                const question = election.questions.find((q) => q.id === questionResult.id);

                return question ? (
                  <View>
                    <Text style={styles.text}>{question.question}</Text>
                    <BarChartDisplay
                      data={questionResult.result}
                      key={questionResult.id.valueOf()}
                    />
                  </View>
                ) : null;
              })}
          </>
        );
      default:
        console.warn('Election Status was undefined in Election display', election);
        return null;
    }
  };

  return (
    <ScreenWrapper>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
      {getElectionDisplay(election.electionStatus)}
    </ScreenWrapper>
  );
};

export default ViewSingleElection;

export const ViewSingleElectionScreenHeader = () => {
  const route = useRoute<NavigationProps['route']>();
  const { eventId: electionId } = route.params;

  const selectElection = useMemo(() => makeElectionSelector(electionId), [electionId]);
  const election = useSelector(selectElection);

  if (!election) {
    throw new Error(`Could not find a roll call with id ${electionId}`);
  }

  return <Text style={Typography.topNavigationHeading}>{election.name}</Text>;
};

export const ViewSingleElectionScreen: EvotingFeature.LaoEventScreen = {
  id: STRINGS.navigation_lao_events_view_single_election,
  Component: ViewSingleElection,
  headerTitle: ViewSingleElectionScreenHeader,
};
