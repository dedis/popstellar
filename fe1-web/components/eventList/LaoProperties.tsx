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

const dateTimePicker = (lao: Lao) => {
  const [creationDate, setCreationDate] = useState(lao.creation);

  function dateToTimestamp(date: Date): Timestamp {
    return new Timestamp(Math.floor(date.getTime() / 1000));
  }

  return (
    <View>
      <DatePicker
        selected={creationDate}
        onChange={(date: Date) => setCreationDate(dateToTimestamp(date))}
      />
    </View>
  );
};

function renderProperties(lao: Lao) {
  const isOrganizer = KeyPairStore.getPublicKey().toString() === lao.organizer.toString();

  return (isOrganizer)
    ? (
      <>
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
        <>
          <ParagraphBlock text="Lao creation: " />
          { () => dateTimePicker(lao) }
        </>
      </>
    ) : (
      <>
        <ParagraphBlock text={`Lao name: ${lao.name}`} />
        <ParagraphBlock text={`Lao creation: ${lao.creation.toString()}`} />
      </>
    );
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
