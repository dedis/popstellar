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

/**
 * An abstract high level message
 */
public abstract class Message {

    /**
     * A mapping of object -> action -> class
     */
    public static final Map<Objects, Map<Action, Class<? extends Message>>> messages = buildMessagesMap();

    private static Map<Objects, Map<Action, Class<? extends Message>>> buildMessagesMap() {
        //Lao
        Map<Action, Class<? extends Message>> laoMap = new EnumMap<>(Action.class);
        laoMap.put(Action.CREATE, CreateLao.class);
        laoMap.put(Action.UPDATE, UpdateLao.class);
        laoMap.put(Action.STATE, StateLao.class);

        //Meeting
        Map<Action, Class<? extends Message>> meetingMap = new EnumMap<>(Action.class);
        meetingMap.put(Action.CREATE, CreateMeeting.class);
        meetingMap.put(Action.STATE, StateMeeting.class);

        //Message
        Map<Action, Class<? extends Message>> messageMap = new EnumMap<>(Action.class);
        messageMap.put(Action.WITNESS, WitnessMessage.class);

        Map<Objects, Map<Action, Class<? extends Message>>> messagesMap = new EnumMap<>(Objects.class);
        messagesMap.put(Objects.LAO, Collections.unmodifiableMap(laoMap));
        messagesMap.put(Objects.MEETING, Collections.unmodifiableMap(meetingMap));
        messagesMap.put(Objects.MESSAGE, Collections.unmodifiableMap(messageMap));

        return Collections.unmodifiableMap(messagesMap);
    }

    /**
     * @return the object the message is referring to
     */
    public abstract String getObject();

    /**
     * @return the action the message is handling
     */
    public abstract String getAction();
}
