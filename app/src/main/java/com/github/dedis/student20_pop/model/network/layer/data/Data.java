package com.github.dedis.student20_pop.model.network.layer.data;

import com.github.dedis.student20_pop.model.network.layer.data.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.layer.data.lao.StateLao;
import com.github.dedis.student20_pop.model.network.layer.data.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.layer.data.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.layer.data.meeting.StateMeeting;
import com.github.dedis.student20_pop.model.network.layer.data.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.layer.data.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.layer.data.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.layer.data.rollcall.OpenRollCall;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * An abstract high level message
 */
public abstract class Data {

    /**
     * A mapping of object -> action -> class
     */
    public static final Map<Objects, Map<Action, Class<? extends Data>>> messages = buildMessagesMap();

    private static Map<Objects, Map<Action, Class<? extends Data>>> buildMessagesMap() {
        //Lao
        Map<Action, Class<? extends Data>> laoMap = new EnumMap<>(Action.class);
        laoMap.put(Action.CREATE, CreateLao.class);
        laoMap.put(Action.UPDATE, UpdateLao.class);
        laoMap.put(Action.STATE, StateLao.class);

        //Meeting
        Map<Action, Class<? extends Data>> meetingMap = new EnumMap<>(Action.class);
        meetingMap.put(Action.CREATE, CreateMeeting.class);
        meetingMap.put(Action.STATE, StateMeeting.class);

        //Message
        Map<Action, Class<? extends Data>> messageMap = new EnumMap<>(Action.class);
        messageMap.put(Action.WITNESS, WitnessMessage.class);

        //Roll Call
        Map<Action, Class<? extends Data>> rollCall = new EnumMap<>(Action.class);
        rollCall.put(Action.CREATE, CreateRollCall.class);
        rollCall.put(Action.OPEN, OpenRollCall.class);
        rollCall.put(Action.CLOSE, CloseRollCall.class);

        Map<Objects, Map<Action, Class<? extends Data>>> messagesMap = new EnumMap<>(Objects.class);
        messagesMap.put(Objects.LAO, Collections.unmodifiableMap(laoMap));
        messagesMap.put(Objects.MEETING, Collections.unmodifiableMap(meetingMap));
        messagesMap.put(Objects.MESSAGE, Collections.unmodifiableMap(messageMap));
        messagesMap.put(Objects.ROLL_CALL, Collections.unmodifiableMap(rollCall));

        return Collections.unmodifiableMap(messagesMap);
    }

    /**
     * Returns the object the message is referring to.
     */
    public abstract String getObject();

    /**
     * Returns the action the message is handling.
     */
    public abstract String getAction();
}
