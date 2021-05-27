import React from 'react';
import PropTypes from 'prop-types';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import CopyButton from 'components/CopyButton';
import TextBlock from './TextBlock';
import { Views } from '../styles';

/**
 * TextInput component which is removable by clicking the trashcan
 * It is used by the TextInputList.tsx component
 */

const styles = StyleSheet.create({
  view: {
    ...Views.base,
    flexDirection: 'row',
    zIndex: 3,
  } as ViewStyle,
});

const CopiableTextBlock = (props: IPropTypes) => {
  const { id } = props;
  const { text } = props;
  const { visibility } = props;

  return (
    <View style={styles.view}>
      <CopyButton action={() => navigator.clipboard.writeText(text)} />
      <TextBlock text={text} key={id} visibility={visibility} />
    </View>
  );
};

const propTypes = {
  id: PropTypes.number.isRequired,
  text: PropTypes.string,
  visibility: PropTypes.bool,
};

CopiableTextBlock.propTypes = propTypes;

CopiableTextBlock.defaultProps = {
  text: '',
  visibility: false,
};

type IPropTypes = {
  id: number,
  text: string,
  visibility: boolean,
};

export default CopiableTextBlock;
