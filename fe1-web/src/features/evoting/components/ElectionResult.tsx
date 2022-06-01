import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

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
      <Text style={Typography.heading}>{STRINGS.election_results}</Text>

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
                {
                  // create a copy since sort() mutates the original object but the election but be immutable
                  [...questionResult.result]
                    .sort((a, b) => a.count - b.count)
                    .map((ballotOption, idx) => {
                      const listStyles = List.getListItemStyles(
                        idx === 0,
                        idx === questionResult.result.length - 1,
                      );

                      return (
                        <ListItem
                          key={ballotOption.ballotOption}
                          containerStyle={listStyles}
                          style={listStyles}>
                          <View style={List.iconPlaceholder} />
                          <ListItem.Content style={styles.ballotOptionResult}>
                            <ListItem.Title style={Typography.base}>
                              {ballotOption.ballotOption}
                            </ListItem.Title>
                            <ListItem.Title style={Typography.base}>
                              {ballotOption.count}
                            </ListItem.Title>
                          </ListItem.Content>
                        </ListItem>
                      );
                    })
                }
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
