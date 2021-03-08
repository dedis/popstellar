import React, { useState } from "react";
import { TouchableOpacity, View, ViewStyle } from "react-native";
import { Spacing } from "styles";
import TextBlock from "components/TextBlock";
import styleEventView from "styles/stylesheets/eventView";
import ListCollapsibleIcon from "components/eventList/ListCollapsibleIcon";
import { useSelector } from "react-redux";
import ParagraphBlock from "components/ParagraphBlock";
import { Lao } from "model/objects";
import { makeCurrentLao } from "store/reducers";
import EdiText from "react-editext";

function renderProperties(lao: Lao) {
  const style = {
    fontFamily: 'Helvetica Bold',
    fontSize: '13px',
    width: 200
  };
  return (
    <>
      <ParagraphBlock text='Lao name: ' />
      <EdiText
        hint='type the new LAO name'
        viewProps={{ style: style }}
        inputProps={{ style: style }}
        type='text'
        onSave={
          (newLaoName: string) => console.log(' The new name is ' + newLaoName)
          // TODO: carry out the necessary LAO update interactions with the backend here
        }
        value={`${lao.name}`}
      />
      <ParagraphBlock text='Lao creation: ' />
      <EdiText
        hint='type the new creation date'
        viewProps={{ style: style }}
        inputProps={{ style: style }}
        type='text'
        onSave={
          (newCreation: string) =>
            console.log(' The new creation date is ' + newCreation)
          // TODO: carry out the necessary LAO update interactions with the backend here
        }
        value={`${lao.creation.toString()}`}
      />
    </>
  );
}

const LaoProperties = () => {
  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  const [toggleChildrenVisible, setToggleChildrenVisible] = useState(false);

  const toggleChildren = () => setToggleChildrenVisible(!toggleChildrenVisible);

  return (
    <>
      <TextBlock bold text='Lao Properties' />
      <View style={[styleEventView.default, { marginTop: Spacing.s }]}>
        <TouchableOpacity
          onPress={toggleChildren}
          style={{ textAlign: "right" } as ViewStyle}
        >
          <ListCollapsibleIcon isOpen={toggleChildrenVisible} />
        </TouchableOpacity>

        {toggleChildrenVisible && lao && renderProperties(lao)}
      </View>
    </>
  );
};

export default LaoProperties;
