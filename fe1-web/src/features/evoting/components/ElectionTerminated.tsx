import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { ListItem } from 'react-native-elements';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';

import { Election } from '../objects';

const ElectionTerminated = ({ election }: IPropTypes) => {
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
      <Text style={Typography.base}>
        The election was terminated and the votes are being tallied... Waiting for the result
      </Text>
      <Text style={[Typography.paragraph, Typography.important]}>Questions</Text>

      <View style={List.container}>
        {election.questions.map((question) => (
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
              setIsQuestionOpen({ ...isQuestionOpen, [question.id]: !isQuestionOpen[question.id] })
            }
            isExpanded={!!isQuestionOpen[question.id]}>
            {question.ballot_options.map((ballotOption, idx) => {
              const listStyle = List.getListItemStyles(
                idx === 0,
                idx === question.ballot_options.length - 1,
              );

              return (
                <ListItem key={ballotOption} containerStyle={listStyle} style={listStyle}>
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
ElectionTerminated.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionTerminated;
