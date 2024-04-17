package message

import (
	"encoding/base64"
	"encoding/json"
	"popstellar/internal/popserver/channel"
	"popstellar/internal/popserver/state"
	"popstellar/internal/popserver/types"
	jsonrpc "popstellar/message"
	"popstellar/message/answer"
	"popstellar/message/query"
	"popstellar/message/query/method"
)

const rootChannel = "/root"

func handleCatchUp(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
	var catchup method.Catchup

	err := json.Unmarshal(msg, &catchup)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleCatchUp")
		return nil, errAnswer
	}

	result, err := params.DB.GetAllMessagesFromChannel(catchup.Params.Channel)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleCatchUp")
		return &catchup.ID, errAnswer
	}

	params.Socket.SendResult(catchup.ID, result, nil)

	return &catchup.ID, nil
}

func handleGetMessagesByID(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
	var getMessagesById method.GetMessagesById

	err := json.Unmarshal(msg, &getMessagesById)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal message: %v",
			err).Wrap("handleGetMessageByID")
		return nil, errAnswer
	}

	result, err := params.DB.GetResultForGetMessagesByID(getMessagesById.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleGetMessageByID")
		return &getMessagesById.ID, errAnswer
	}

	params.Socket.SendResult(getMessagesById.ID, nil, result)

	return &getMessagesById.ID, nil
}

func handleGreetServer(params types.HandlerParameters, byteMessage []byte) (*int, *answer.Error) {
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

	err = peers.AddPeerInfo(params.Socket.ID(), greetServer.Params)
	if err != nil {
		errAnswer := answer.NewInvalidActionError("failed to add peer: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	if peers.IsPeerGreeted(params.Socket.ID()) {
		return nil, nil
	}

	pkBuf, err := params.ServerPubKey.MarshalBinary()
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to unmarshall server public key", err)
		errAnswer = errAnswer.Wrap("copyToGeneral")
		return nil, errAnswer
	}

	serverInfo := method.GreetServerParams{
		PublicKey:     base64.URLEncoding.EncodeToString(pkBuf),
		ServerAddress: params.ServerServerAddress,
		ClientAddress: params.ClientServerAddress,
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
		errAnswer := answer.NewInternalServerError("failed to marshal: %v", err).Wrap("handleGreetServer")
		return nil, errAnswer
	}

	params.Socket.Send(buf)

	peers.AddPeerGreeted(params.Socket.ID())

	return nil, nil
}

func handleHeartbeat(params types.HandlerParameters, byteMessage []byte) (*int, *answer.Error) {
	var heartbeat method.Heartbeat

	err := json.Unmarshal(byteMessage, &heartbeat)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	result, err := params.DB.GetParamsForGetMessageByID(heartbeat.Params)
	if err != nil {
		errAnswer := answer.NewInternalServerError("failed to query DB: %v", err).Wrap("handleHeartbeat")
		return nil, errAnswer
	}

	if len(result) == 0 {
		return nil, nil
	}

	queries, ok := state.GetQueriesInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleGreetServer")
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

	params.Socket.Send(buf)

	queries.AddQuery(queryId, getMessagesById)

	return nil, nil
}

func handlePublish(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handlePublish")
		return nil, errAnswer
	}

	errAnswer := channel.HandleChannel(params, publish.Params.Channel, publish.Params.Message)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handlePublish")
		return &publish.ID, errAnswer
	}

	return &publish.ID, nil
}

func handleSubscribe(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
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

	subs, ok := state.GetSubsInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleGreetServer")
		return &subscribe.ID, errAnswer
	}

	errAnswer := subs.Subscribe(subscribe.Params.Channel, params.Socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleSubscribe")
		return &subscribe.ID, errAnswer
	}

	return &subscribe.ID, nil
}

func handleUnsubscribe(params types.HandlerParameters, msg []byte) (*int, *answer.Error) {
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

	subs, ok := state.GetSubsInstance()
	if !ok {
		errAnswer := answer.NewInternalServerError("failed to get state").Wrap("handleGreetServer")
		return &unsubscribe.ID, errAnswer
	}

	errAnswer := subs.Unsubscribe(unsubscribe.Params.Channel, params.Socket)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handleUnsubscribe")
		return &unsubscribe.ID, errAnswer
	}

	return &unsubscribe.ID, nil
}