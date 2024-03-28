package hub

import (
	"popstellar/message/answer"
)

func handleGetMessagesByIdAnswer(params handlerParameters, msg answer.Answer) (*int, error) {
	return msg.ID, nil
}
