package hub

import (
	"encoding/json"
	"golang.org/x/xerrors"
	"popstellar/message/query/method"
)

func handlePublish(params handlerParameters, msg []byte) (*int, error) {
	var publish method.Publish

	err := json.Unmarshal(msg, &publish)
	if err != nil {
		return nil, xerrors.Errorf("failed to unmarshal publish message: %v", err)
	}

	channelType, err := params.db.GetChannelType(publish.Params.Channel)
	if err != nil {
		return &publish.ID, xerrors.Errorf("channel %s doesn't exist in the database", publish.Params.Channel)
	}

	err = handleChannel(params, channelType, publish.Params.Message)

	return &publish.ID, err
}
