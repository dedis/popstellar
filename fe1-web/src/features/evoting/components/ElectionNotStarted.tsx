import PropTypes from 'prop-types';
import React from 'react';
import { SectionList, StyleSheet, Text, TextStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { Button, TimeDisplay } from 'core/components';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';

import { openElection } from '../network/ElectionMessageApi';
import { Election } from '../objects';

const styles = StyleSheet.create({
  textOptions: {
    marginHorizontal: Spacing.x1,
    fontSize: 16,
    textAlign: 'center',
  } as TextStyle,
  textQuestions: {
    ...Typography.baseCentered,
    fontSize: 20,
  } as TextStyle,
});

const ElectionNotStarted = ({ election, questions, isOrganizer }: IPropTypes) => {
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

  return (
    <>
      <TimeDisplay start={election.start.valueOf()} end={election.end.valueOf()} />
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
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
  questions: PropTypes.arrayOf(
    PropTypes.shape({
      title: PropTypes.string.isRequired,
      data: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
    }).isRequired,
  ).isRequired,
  isOrganizer: PropTypes.bool,
};
ElectionNotStarted.propTypes = propTypes;

ElectionNotStarted.defaultProps = {
  isOrganizer: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionNotStarted;
