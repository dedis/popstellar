package message

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/popserver/channel"
	"popstellar/internal/popserver/config"
	"popstellar/internal/popserver/database"
	"popstellar/internal/popserver/state"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
	"popstellar/network/socket"
)

const rootChannel = "/root"

func handleGreetServer(socket socket.Socket, byteMessage []byte) (*int, *answer.Error) {
	var greetServer method.GreetServer

	err := json.Unmarshal(byteMessage, &greetServer)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshal: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	peers, ok := state.GetPeersInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleGreetServer")
		return nil, errAnswer
	}

	err = peers.AddPeerInfo(socket.ID(), greetServer.Params)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to add peer: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	if peers.IsPeerGreeted(socket.ID()) {
		return nil, nil
	}

	pk, clientAddress, serverAddress, ok := config.GetServerInfo()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get config").Wrap("handleGreetServer")
		return nil, errAnswer
	}

	pkBuf, err := pk.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		errAnswer = errAnswer.Wrap("handleGreetServer")
		return nil, errAnswer
	}

	greetServerParams := method.GreetServerParams{
		PublicKey:     base64.URLEncoding.EncodeToString(pkBuf),
		ServerAddress: serverAddress,
		ClientAddress: clientAddress,
	}

	serverGreet := &method.GreetServer{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGreetServer,
		},
		Params: greetServerParams,
	}

	buf, err := json.Marshal(serverGreet)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	socket.Send(buf)

	peers.AddPeerGreeted(socket.ID())

	return nil, nil
}

func handleSubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var subscribe method.Subscribe

	err := json.Unmarshal(msg, &subscribe)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleSubscribe")
		return nil, errAnswer
	}

	if rootChannel == subscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Subscribe to root channel").Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	errAnswer := state.Subscribe(socket, subscribe.Params.Channel)
	if errAnswer != nil {
		return &subscribe.ID, errAnswer.Wrap("handleSubscribe")
	}

	socket.SendResult(subscribe.ID, nil, nil)

	return &subscribe.ID, nil
}

func handleUnsubscribe(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var unsubscribe method.Unsubscribe

	err := json.Unmarshal(msg, &unsubscribe)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleUnsubscribe")
		return nil, errAnswer
	}

	if rootChannel == unsubscribe.Params.Channel {
		errAnswer := answer.NewInvalidActionError("cannot Unsubscribe from root channel").Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	errAnswer := state.Unsubscribe(socket, unsubscribe.Params.Channel)
	if errAnswer != nil {
		return &unsubscribe.ID, errAnswer.Wrap("handleUnsubscribe")
	}

	socket.SendResult(unsubscribe.ID, nil, nil)

	return &unsubscribe.ID, nil
}

func handlePublish(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handlePublish")
		return nil, errAnswer
	}

	errAnswer := channel.HandleChannel(publish.Params.Channel, publish.Params.Message)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handlePublish")
		return &publish.ID, errAnswer
	}

	socket.SendResult(publish.ID, nil, nil)

	return &publish.ID, nil
}

func handleCatchUp(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleCatchUp")
		return nil, errAnswer
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleCatchUp")
		return &catchup.ID, errAnswer
	}

	result, err := db.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleCatchUp")
		return &catchup.ID, errAnswer
	}

	socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}

func handleHeartbeat(socket socket.Socket, byteMessage []byte) (*int, *answer.Error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	result, err := db.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	if len(result) == 0 {
		return nil, nil
	}

	queries, ok := state.GetQueriesInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	queryId := queries.GetNextID()

	getMessagesById := method.GetMessagesById{
		Base: query.Base{
			JSONRPCBase: jsonrpc.JSONRPCBase{
				JSONRPC: "2.0",
			},
			Method: query.MethodGetMessagesById,
		},
		ID:     queryId,
		Params: result,
	}

	buf, err := json.Marshal(getMessagesById)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	socket.Send(buf)

	queries.AddQuery(queryId, getMessagesById)

	return nil, nil
}

func handleGetMessagesByID(socket socket.Socket, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal message: %v",
			err).Wrap("handleGetMessageByID")
		return nil, errAnswer
	}

	db, errAnswer := database.GetQueryRepositoryInstance()
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleGetMessageByID")
		return &getMessagesById.ID, errAnswer
	}

	result, err := db.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleGetMessageByID")
		return &getMessagesById.ID, errAnswer
	}

	socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}
