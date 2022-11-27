import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch, useSelector } from 'react-redux';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import { List, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { connectToLao, resubscribeToLao } from '../functions';
import { Lao } from '../objects';
import { makeIsLaoOrganizerSelector, makeIsLaoWitnessSelector, setCurrentLao } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<HomeParamList, typeof STRINGS.navigation_home_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const LaoItem = ({ lao, isFirstItem, isLastItem }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();

  const isWitnessSelector = makeIsLaoWitnessSelector(lao.id.valueOf());
  const isOrganizerSelector = makeIsLaoOrganizerSelector(lao.id.valueOf());

  const isWitness = useSelector(isWitnessSelector);
  const isOrganizer = useSelector(isOrganizerSelector);

  const dispatch = useDispatch();

  const role = useMemo(() => {
    if (isOrganizer) {
      return STRINGS.user_role_organizer;
    }
    if (isWitness) {
      return STRINGS.user_role_witness;
    }

    return STRINGS.user_role_attendee;
  }, [isWitness, isOrganizer]);

  const reconnectToLao = async () => {
    try {
      // connect to toe lao
      const connections = await connectToLao(lao);
      // and subscribe to all previously subscribed to channels on the new connections
      await resubscribeToLao(lao, dispatch, connections);

      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_events,
        params: { screen: STRINGS.navigation_lao_events_home },
      });
    } catch (err) {
      dispatch(setCurrentLao({ lao: lao.toState(), connected: false }));

      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_events,
        params: { screen: STRINGS.navigation_lao_events_home },
      });

      console.error(`Failed to establish lao connection: ${err}`);
      toast.show(`Failed to establish connection, entering offline mode`, {
        type: 'warning',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  const listStyle = List.getListItemStyles(isFirstItem, isLastItem);

  return (
    <ListItem
      key={lao.id.valueOf()}
      containerStyle={listStyle}
      style={listStyle}
      onPress={reconnectToLao}
      bottomDivider>
      <ListItem.Content>
        <ListItem.Title style={Typography.base}>{lao.name}</ListItem.Title>
        <ListItem.Subtitle style={Typography.small}>
          {STRINGS.user_role}: {role}
        </ListItem.Subtitle>
        <ListItem.Subtitle style={Typography.small}>
          {lao.server_addresses.join(', ')}
        </ListItem.Subtitle>
      </ListItem.Content>
      <ListItem.Chevron />
    </ListItem>
  );
};

const propTypes = {
  lao: PropTypes.instanceOf(Lao).isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
};
LaoItem.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaoItem;
