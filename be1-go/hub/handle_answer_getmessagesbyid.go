package hub

import (
	"popstellar/message/answer"
)

func handleGetMessagesByIdAnswer(params handlerParameters, msg answer.Answer) (*int, *answer.Error) {
	return msg.ID, nil
}
