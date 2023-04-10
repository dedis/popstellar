package socket

import (
	"encoding/json"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
	"os"
	"popstellar/message/answer"
	"popstellar/message/query/method/message"
	"sync"
	"testing"
)

var socket = *newBaseSocket(
	ServerSocketType,
	make(chan IncomingMessage),
	make(chan string),
	&websocket.Conn{},
	&sync.WaitGroup{},
	make(chan struct{}),
	zerolog.New(zerolog.ConsoleWriter{Out: os.Stdout}).With().Timestamp().Logger(),
)
var msg1 = message.Message{
	Data:              "data1",
	Sender:            "sender1",
	Signature:         "signature1",
	MessageID:         "message1",
	WitnessSignatures: nil,
}
var msg2 = message.Message{
	Data:              "data2",
	Sender:            "sender2",
	Signature:         "signature2",
	MessageID:         "message2",
	WitnessSignatures: nil,
}

var msg3 = message.Message{
	Data:              "data3",
	Sender:            "sender3",
	Signature:         "signature3",
	MessageID:         "message3",
	WitnessSignatures: nil,
}

var res1 = []message.Message{msg1, msg2}
var res2 = []message.Message{msg3}

func Test_SendResult_Res(t *testing.T) {
	socket.SendResult(1, res1, nil)

	var receivedAnswer answer.Answer

	ans := <-socket.send

	err := json.Unmarshal(ans, &receivedAnswer)
	require.NoError(t, err)

	require.False(t, receivedAnswer.Result.IsEmpty())

	require.Equal(t, 1, *receivedAnswer.ID)

	messages := receivedAnswer.Result.GetData()
	for msg := range messages {
		var messageData message.Message
		err = json.Unmarshal(messages[msg], &messageData)
		require.NoError(t, err)

		require.Contains(t, res1, messageData)
	}
}

func Test_SendResult_Messages_By_Channel_Id(t *testing.T) {
	msgsByChannelId := make(map[string][]message.Message)
	msgsByChannelId["channel1"] = res1
	msgsByChannelId["channel2"] = res2

	socket.SendResult(1, nil, msgsByChannelId)

	var receivedAnswer answer.Answer

	ans := <-socket.send

	err := json.Unmarshal(ans, &receivedAnswer)
	require.NoError(t, err)

	require.False(t, receivedAnswer.Result.IsEmpty())

	messagesByChannel := receivedAnswer.Result.GetMessagesByChannel()
	for channelId, msgs := range messagesByChannel {
		for _, msg := range msgs {
			var messageData message.Message
			err = json.Unmarshal(msg, &messageData)
			require.NoError(t, err)
			require.Contains(t, msgsByChannelId[channelId], messageData)
		}
	}
}

func Test_SendResult_Empty_Answer(t *testing.T) {
	socket.SendResult(0, nil, nil)
	var receivedAnswer answer.Answer

	ans := <-socket.send

	err := json.Unmarshal(ans, &receivedAnswer)
	require.NoError(t, err)

	require.True(t, receivedAnswer.Result.IsEmpty())
}
