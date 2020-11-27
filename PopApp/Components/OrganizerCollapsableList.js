import React, { useState } from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, FlatList, Button, TextInput,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';

import { Typography, Spacing, Buttons } from '../Styles';
import EventItem from './EventItem';
import STRINGS from '../res/strings';

/**
* The Collapsable list component for the organizer
*
* It is assume that the nested events are already been calculated.
* They shoud be in the childrens value of the event
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    ...Typography.base,
  },
  textItem: {
    borderWidth: 1,
    borderRadius: 5,
    marginBottom: Spacing.xs,
    paddingHorizontal: Spacing.xs,
  },
  touchable: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  button: {
    ...Buttons.base,
  },
});

const Item = ({ events, closedList }) => {
  const [open, setopen] = closedList.includes(events.title) ? useState(false) : useState(true);
  const [isPropretyEditing, setPropretyEditing] = useState(false);
  const onPress = () => {
    setopen(!open);
  };
  const navigation = useNavigation();

  const organizationNameBase = events.title === '' ? events.data.filter((e) => e.type === 'organization_name')[0].name : '';
  const [organizationName, setOrganizationName] = useState(organizationNameBase);

  const witnessesBase = events.title === '' ? [...events.data.filter((e) => e.type === 'witness')[0].witnesses].map((w) => ({ name: w, isRemove: false })) : [];
  const [witnesses, setWitnesses] = useState(witnessesBase);

  const removeElement = (index) => {
    const wit = witnesses[index];
    wit.isRemove = true;
    witnesses[index] = wit;
    setWitnesses([...witnesses]);
  };

  const addPreviouslyRemoveElement = (index) => {
    const wit = witnesses[index];
    wit.isRemove = false;
    witnesses[index] = wit;
    setWitnesses([...witnesses]);
  };

  const renderArrowDropDown = () => {
    if (events.title === '') {
      return (
        <View style={{ flexDirection: 'row' }}>
          {!isPropretyEditing && (
            <TouchableOpacity
              onPress={() => { setPropretyEditing(true); }}
            >
              <Text style={[styles.text, { marginRight: Spacing.xs }]}>edit</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.text}>{open ? '⌵' : 'ᐳ'}</Text>
        </View>
      );
    }
    if (events.title === 'Future') {
      return (
        <View style={{ flexDirection: 'row' }}>
          {!isPropretyEditing && (
            <TouchableOpacity
              onPress={() => {
                navigation.navigate(STRINGS.organizer_navigation_tab_create_event);
              }}
            >
              <Text style={[styles.text, { marginRight: Spacing.xs }]}>+</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.text}>{open ? '⌵' : 'ᐳ'}</Text>
        </View>
      );
    }
    return (
      <Text style={styles.text}>{open ? '⌵' : 'ᐳ'}</Text>
    );
  };

  const renderPropretiesEditing = (item) => {
    switch (item.type) {
      case 'organization_name':
        return (
          <TextInput
            placeholder={STRINGS.organization_name}
            value={organizationName}
            onChangeText={(text) => setOrganizationName(text)}
          />
        );
      case 'witness':
        return (
          <FlatList
            data={witnesses}
            keyExtractor={(it, index) => index.toString()}
            renderItem={({ index }) => (
              <View style={{ flexDirection: 'row' }}>
                <Text
                  style={[styles.text, { flex: 10, textDecorationLine: witnesses[index].isRemove ? 'line-through' : 'none' }]}
                >
                  {witnesses[index].name}
                </Text>
                <View style={[styles.button, { flex: 1 }]}>
                  {witnesses[index].isRemove === false && (
                  <Button onPress={() => removeElement(index)} title="-" />
                  )}
                  {witnesses[index].isRemove === true && (
                  <Button onPress={() => addPreviouslyRemoveElement(index)} title="+" />
                  )}
                </View>
              </View>
            )}
            ListFooterComponent={(
              <View style={styles.button}>
                <Button
                  title={STRINGS.organizer_navigation_tab_add_witness}
                  onPress={() => {
                    navigation.navigate(STRINGS.organizer_navigation_tab_add_witness);
                    witnesses.push({ name: `Witness ${witnesses.length + 1}`, isRemove: false });
                    setWitnesses([...witnesses]);
                  }}
                />
              </View>
            )}
          />
        );
      default:
        return null;
    }
  };

  const renderFlatList = () => {
    if (events.title === '' && open) {
      if (isPropretyEditing) {
        return (
          <FlatList
            data={events.data}
            keyExtractor={(item) => item.id.toString()}
            renderItem={({ item }) => renderPropretiesEditing(item)}
            listKey={events.title}
            ListFooterComponent={(
              <View>
                <View style={styles.button}>
                  <Button
                    title={STRINGS.general_button_confirm}
                    disabled={organizationName === organizationNameBase
                      && witnesses.toString() === witnessesBase.toString()}
                  />
                </View>
                <View style={styles.button}>
                  <Button
                    title={STRINGS.general_button_cancel}
                    onPress={() => {
                      setPropretyEditing(false);
                      setOrganizationName(organizationNameBase);
                      setWitnesses(witnessesBase);
                    }}
                  />
                </View>
              </View>
            )}
          />
        );
      }
      return (
        <FlatList
          data={events.data}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => <EventItem event={item} isOrganizer />}
          listKey={events.title}
        />
      );
    } if (open) {
      return (
        <FlatList
          data={events.data}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => <EventItem event={item} isOrganizer />}
          listKey={events.title}
        />
      );
    }
    return (null);
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        onPress={onPress}
        activeOpacity={1}
        style={styles.touchable}
      >
        <Text style={styles.text}>{events.title}</Text>
        {renderArrowDropDown()}
      </TouchableOpacity>

      {renderFlatList()}
    </View>
  );
};

Item.propTypes = {
  events: PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(EventItem.propTypes.event).isRequired,
  }).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

const OrganizerEventsCollapsableList = ({ data, closedList }) => (
  <FlatList
    data={data}
    keyExtractor={(item) => item.title}
    renderItem={({ item }) => <Item events={item} closedList={closedList} />}
    listKey="Base"
  />
);

OrganizerEventsCollapsableList.propTypes = {
  data: PropTypes.arrayOf(Item.propTypes.events).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default OrganizerEventsCollapsableList;
