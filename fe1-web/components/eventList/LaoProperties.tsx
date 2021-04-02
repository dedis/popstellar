import React, { useState } from 'react';
import {
  TouchableOpacity, View, ViewStyle,
} from 'react-native';
import { Spacing } from 'styles';
import TextBlock from 'components/TextBlock';
import styleEventView from 'styles/stylesheets/eventView';
import ListCollapsibleIcon from 'components/eventList/ListCollapsibleIcon';
import { useSelector } from 'react-redux';
import ParagraphBlock from 'components/ParagraphBlock';
import { Lao, Timestamp } from 'model/objects';
import { makeCurrentLao } from 'store/reducers';
import EdiText from 'react-editext';
import { KeyPairStore } from '../../store';
import { editText } from '../../styles/typography';
import DatePicker from '../DatePicker';

const dateToTimestamp = (date: Date) => new Timestamp(Math.floor(date.getTime() / 1000));

const timestampToDate = (t: Timestamp) => {
  const dateAndTime = new Date(t.valueOf());
  return `${dateAndTime.toDateString()},  ${dateAndTime.toLocaleTimeString()}`;
};

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const dateTimePicker = (lao: Lao) => {
  const [creationDate, setCreationDate] = useState(lao.creation);

  return (
    <>
      <DatePicker
        selected={creationDate}
        onChange={(date: Date) => {
          setCreationDate(dateToTimestamp(date));
          // TODO: carry out the necessary LAO update interactions with the backend here
        }}
      />
    </>
  );
};

function renderProperties(lao: Lao) {
  const isOrganizer = KeyPairStore.getPublicKey().toString() === lao.organizer.toString();

  return (isOrganizer)
    ? (
      <View>
        <ParagraphBlock text="Lao name: " />
        <EdiText
          hint="type the new LAO name"
          viewProps={{ style: editText }}
          inputProps={{ style: editText }}
          type="text"
          onSave={() => {
          // TODO: carry out the necessary LAO update interactions with the backend here
          }}
          value={`${lao.name}`}
        />
        <ParagraphBlock text="Lao creation: " />
        <ParagraphBlock text={`${timestampToDate(lao.creation)}`} />

      </View>
    )
    : (
      <>
        <ParagraphBlock text={`Lao name: ${lao.name}`} />
        <ParagraphBlock text={`Lao creation: ${timestampToDate(lao.creation)}`} />
      </>
    );
  // { dateTimePicker(lao) } on line 59 ?? rendered more hooks then during the previous render..
}

const LaoProperties = () => {
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);
  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const toggleChildren = () => (setToggleChildrenVisible(!toggleChildrenVisible));

  return (
    <>
      <TextBlock bold text="Lao Properties" />
      <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
        <TouchableOpacity onPress={toggleChildren} style={{ textAlign: 'right' } as ViewStyle}>
          <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
        </TouchableOpacity>
        { toggleChildrenVisible && lao && renderProperties(lao) }
      </View>
    </>
  );
};

export default LaoProperties;
