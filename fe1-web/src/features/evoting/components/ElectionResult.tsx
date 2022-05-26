import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';

import { Election, QuestionResult } from '../objects';

const styles = StyleSheet.create({
  ballotOptionResult: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
});

const ElectionResult = ({ election }: IPropTypes) => {
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
      <Text style={Typography.heading}>Election Results</Text>

      <View style={List.container}>
        {election.questionResult &&
          election.questionResult.map((questionResult: QuestionResult) => {
            const question = election.questions.find((q) => q.id === questionResult.id);

            if (!question) {
              throw new Error(
                `Received an election result containing a result for the non-existent question with id ${questionResult.id}`,
              );
            }

            return (
              <ListItem.Accordion
                key={question.id}
                containerStyle={List.item}
                content={
                  <ListItem.Content>
                    <ListItem.Title>{question.question}</ListItem.Title>
                  </ListItem.Content>
                }
                onPress={() =>
                  setIsQuestionOpen({
                    ...isQuestionOpen,
                    [question.id]: !isQuestionOpen[question.id],
                  })
                }
                isExpanded={!!isQuestionOpen[question.id]}>
                {questionResult.result
                  .sort((a, b) => a.count - b.count)
                  .map((ballotOption) => (
                    <ListItem key={ballotOption.ballotOption} containerStyle={List.item}>
                      <View style={List.iconPlaceholder} />
                      <ListItem.Content style={styles.ballotOptionResult}>
                        <ListItem.Title>{ballotOption.ballotOption}</ListItem.Title>
                        <ListItem.Title>{ballotOption.count}</ListItem.Title>
                      </ListItem.Content>
                    </ListItem>
                  ))}
              </ListItem.Accordion>
            );
          })}
      </View>
    </ScreenWrapper>
  );
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionResult.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionResult;
