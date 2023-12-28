package com.github.dedis.popstellar.repository.remote

import com.github.dedis.popstellar.model.network.GenericMessage
import com.github.dedis.popstellar.model.network.method.Message
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Observable

interface LAOService {
    @Send
    fun sendMessage(msg: Message)

    @Receive
    fun observeMessage(): Observable<GenericMessage?>

    @Receive
    fun observeWebsocket(): Observable<WebSocket.Event?>
}