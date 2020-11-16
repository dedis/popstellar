package com.github.dedis.student20_pop.model.network.level.high;

import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.StateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.meeting.StateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public abstract class Message {

    public static final Map<Objects, Map<Action, Class<? extends Message>>> messages = buildMessagesMap();

    private static Map<Objects, Map<Action, Class<? extends Message>>> buildMessagesMap() {
        Map<Objects, Map<Action, Class<? extends Message>>> messagesMap = new EnumMap<>(Objects.class);
        //Lao
        Map<Action, Class<? extends Message>> laoMap = new EnumMap<>(Action.class);
        laoMap.put(Action.CREATE, CreateLao.class);
        laoMap.put(Action.UPDATE, UpdateLao.class);
        laoMap.put(Action.STATE, StateLao.class);
        messagesMap.put(Objects.LAO, Collections.unmodifiableMap(laoMap));

        //Meeting
        Map<Action, Class<? extends Message>> meetingMap = new EnumMap<>(Action.class);
        meetingMap.put(Action.CREATE, CreateMeeting.class);
        meetingMap.put(Action.STATE, StateMeeting.class);
        messagesMap.put(Objects.MEETING, Collections.unmodifiableMap(meetingMap));

        //Message
        Map<Action, Class<? extends Message>> messageMap = new EnumMap<>(Action.class);
        messageMap.put(Action.WITNESS, WitnessMessage.class);
        messagesMap.put(Objects.MESSAGE, Collections.unmodifiableMap(messageMap));

        return Collections.unmodifiableMap(messagesMap);
    }

    public abstract String getObject();

    public abstract String getAction();
}
