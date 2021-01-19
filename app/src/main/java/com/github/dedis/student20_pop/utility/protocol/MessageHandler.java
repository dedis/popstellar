package com.github.dedis.student20_pop.utility.protocol;

import com.github.dedis.student20_pop.model.network.answer.Error;
import com.github.dedis.student20_pop.model.network.answer.Result;
import com.github.dedis.student20_pop.model.network.method.Broadcast;
import com.github.dedis.student20_pop.model.network.method.Catchup;
import com.github.dedis.student20_pop.model.network.method.Publish;
import com.github.dedis.student20_pop.model.network.method.Subscribe;
import com.github.dedis.student20_pop.model.network.method.Unsubscribe;

/**
 * Handler of the low-level messages
 *
 * @see com.github.dedis.student20_pop.model.network.GenericMessage
 */
public interface MessageHandler {

    /**
     * Handles a Result message
     *
     * @param result to handle
     */
    default void handle(Result result) {}

    /**
     * Handles a Error message
     *
     * @param error to handle
     */
    default void handle(Error error) {}

    /**
     * Handles a Broadcast message
     *
     * @param broadcast to handle
     */
    default void handle(Broadcast broadcast) {}

    /**
     * Handles a Catchup message
     *
     * @param catchup to handle
     */
    default void handle(Catchup catchup) {}

    /**
     * Handles a Publish message
     *
     * @param publish to handle
     */
    default void handle(Publish publish) {}

    /**
     * Handles a Subscribe message
     *
     * @param subscribe to handle
     */
    default void handle(Subscribe subscribe) {}

    /**
     * Handles a Unsubscribe message
     *
     * @param unsubscribe to handle
     */
    default void handle(Unsubscribe unsubscribe) {}
}
