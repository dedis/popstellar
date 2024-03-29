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
		return nil, answer.NewInvalidMessageFieldError("failed to unmarshal publish message: %v",
			err).Wrap("handlePublish")
	}

	channelType, err := params.db.GetChannelType(publish.Params.Channel)
	if err != nil {
		return &publish.ID, answer.NewInvalidResourceError("channel %s doesn't exist in the database",
			publish.Params.Channel).Wrap("handlePublish")
	}

	errAnswer := handleChannel(params, channelType, publish.Params.Message)
	if errAnswer != nil {
		return &publish.ID, errAnswer.Wrap("handlePublish")
	}

	return &publish.ID, nil
}
