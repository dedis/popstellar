import React, { useState } from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, FlatList, Button, TextInput,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';

import STRINGS from 'res/strings';
import EventGeneral from 'components/eventList/events/EventGeneral';
import { requestUpdateLao } from 'network';
import { Typography, Spacing, Buttons } from '../styles';
import PROPS_TYPE from '../res/Props';

/**
 * Manage the collapsable list of events for a organizer: contain a section list of events
 *
 * By default all sections are open, you can set the closed section by putting their names in the
 *  closedList
 *
 * It is assume that the nested events are already been calculated.
 * They shoud be in the children value of the event
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

/**
 * List of events of the same section
 *
 * If the section is the proprety one (empty name) show an edit button in the section name bar and
 * allow the organizer to add/remove witness and edit the LAO name
 *
 * TODO confirm button need to send modification to the server
 * TODO the process to add witness must use the camera to scan a QR code
 */
const Item = ({ events, closedList }) => {
  const [open, setopen] = closedList.includes(events.title) ? useState(false) : useState(true);
  const [isPropretyEditing, setPropretyEditing] = useState(false);
  const onPress = () => {
    setopen(!open);
  };
  const navigation = useNavigation();

  const tempOrganizationName = events.data.filter((e) => e.object === 'organization_name');
  const organizationNameBase = events.title === '' && tempOrganizationName.length > 0 ? tempOrganizationName[0].name : '';
  const [organizationName, setOrganizationName] = useState(organizationNameBase);

  const tempWitnesses = events.data.filter((e) => e.object === 'witness');
  const witnessesBase = events.title === '' && tempWitnesses.length > 0 ? [...tempWitnesses[0].witnesses].map((w) => ({ name: w, isRemove: false })) : [];
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

  // the part to manage the edition of the propreties
  const renderPropretiesEditing = (item) => {
    switch (item.object) {
      case 'organization_name':
        return (
          <TextInput
            style={styles.text}
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
            listKey={`OrganizerEventsCollapsableList-${events.title}-properties-editing`}
            ListFooterComponent={(
              <View>
                <View style={styles.button}>
                  <Button
                    title={STRINGS.general_button_confirm}
                    disabled={organizationName === organizationNameBase
                      && witnesses.toString() === witnessesBase.toString()}
                    onPress={() => {
                      setPropretyEditing(false);
                      requestUpdateLao(organizationName, witnesses.map((x) => x.name));
                    }}
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
          renderItem={({ item }) => <EventGeneral event={item} />}
          listKey={`OrganizerEventsCollapsableList-${events.title}-properties`}
        />
      );
    } if (open) {
      return (
        <FlatList
          data={events.data}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => <EventGeneral event={item} />}
          listKey={`OrganizerEventsCollapsableList-${events.title}-open`}
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
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  }).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

/**
 * List of all section of the data
 *
 * Data must have a title (String) and a data (List of event object) field
 */
const OrganizerEventsCollapsableList = ({ data, closedList }) => (
  <FlatList
    data={data}
    keyExtractor={(item) => item.title}
    renderItem={({ item }) => <Item events={item} closedList={closedList} />}
    listKey="OrganizerEventsCollapsableList"
  />
);

OrganizerEventsCollapsableList.propTypes = {
  data: PropTypes.arrayOf(Item.propTypes.events).isRequired,
  closedList: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default OrganizerEventsCollapsableList;
