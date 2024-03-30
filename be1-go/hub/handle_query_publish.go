package hub

import (
	"encoding/json"
	"popstellar/message/answer"
	"popstellar/message/query/method"
)

func handlePublish(params handlerParameters, msg []byte) (*int, *answer.Error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		errAnswer := answer.NewInvalidMessageFieldError("failed to unmarshal: %v", err).Wrap("handlePublish")
		return nil, errAnswer
	}

	channelType, err := params.db.GetChannelType(publish.Params.Channel)
	if err != nil {
		errAnswer := answer.NewInvalidResourceError("error while querying the db: %v", err).Wrap("handlePublish")
		return &publish.ID, errAnswer
	}

	errAnswer := handleChannel(params, channelType, publish.Params.Message)
	if errAnswer != nil {
		errAnswer = errAnswer.Wrap("handlePublish")
		return &publish.ID, errAnswer
	}

	return &publish.ID, nil
}
