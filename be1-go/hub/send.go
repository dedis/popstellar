package hub

import (
	"encoding/base64"
	"encoding/json"
	"golang.org/x/xerrors"
	jsonrpc "popstellar/message"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
)

// SendAndHandleMessage sends a publish message to all other known servers and
// handle it
func (h *Hub) SendAndHandleMessage(msg method.Broadcast) error {
	byteMsg, err := json.Marshal(msg)
	if err != nil {
		return xerrors.Errorf("failed to marshal publish message: %v", err)
	}

	h.log.Info().Str("msg", string(byteMsg)).Msg("sending new message")

	h.serverSockets.SendToAll(byteMsg)

	go func() {
		err = h.handleBroadcast(nil, byteMsg)
		if err != nil {
			h.log.Err(err).Msgf("Failed to handle self-produced message")
		}
	}()

	return nil
}

// sendHeartbeatToServers sends a heartbeat message to all servers
func (h *Hub) sendHeartbeatToServers() {
	heartbeatMessage := method.Heartbeat{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: "heartbeat",
		},
		Params: h.hubInbox.GetIDsTable(),
	}

	buf, err := json.Marshal(heartbeatMessage)
	if err != nil {
		h.log.Err(err).Msg("Failed to marshal and send heartbeat query")
	}
	h.serverSockets.SendToAll(buf)
}

// SendGreetServer implements hub.Hub
func (h *Hub) SendGreetServer(socket socket.Socket) error {
	pk, err := h.pubKeyServ.MarshalBinary()
	if err != nil {
		return xerrors.Errorf("failed to marshal server public key: %v", err)
	}

	serverInfo := method.ServerInfo{
		PublicKey:     base64.URLEncoding.EncodeToString(pk),
		ServerAddress: h.serverServerAddress,
		ClientAddress: h.clientServerAddress,
	}

	serverGreet := &method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGreetServer,
		},
		Params: serverInfo,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		return xerrors.Errorf("failed to marshal server greet: %v", err)
	}

	socket.Send(buf)

	h.peers.AddPeerGreeted(socket.ID())
	return nil
}

// SendToAll sends a message to all sockets.
func SendToAll(subs subscribers, buf []byte, channel string) error {

	sockets, ok := subs[channel]
	if !ok {
		return xerrors.Errorf("channel %s not found", channel)
	}
	for s := range sockets {
		s.Send(buf)
	}
	return nil
}
