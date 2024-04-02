package hub

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/message/answer"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

type Repository interface {
	HandleQueryRepository
	HandleAnswerRepository
	HandleChannelRepository

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// GetMessagesByID returns a set of messages by their IDs.
	GetMessagesByID(IDs []string) (map[string]message.Message, error)

	// GetMessageByID returns a message by its ID.
	GetMessageByID(ID string) (message.Message, error)

	// GetIDsTable returns the map of message IDs by channelID.
	GetIDsTable() (map[string][]string, error)
}

// ======================= Query ==========================

type HandleQueryRepository interface {
	GetServerPubKey() ([]byte, error)
	GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error)

	// GetParamsForGetMessageByID returns the params to do the getMessageByID msg in reponse of heartbeat
	GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error)

	// GetAllMsgFromChannel return all the messages received + sent on a channel
	GetAllMessagesFromChannel(channelID string) ([]message.Message, error)

	GetChannelType(channel string) (string, error)
}

// ======================= Answer ==========================

type HandleAnswerRepository interface {
	AddNewBlackList(msgs map[string]map[string]message.Message) *answer.Error
}

// ======================= Channel ==========================

type HandleChannelRepository interface {
	RootRepository
	ElectionRepository
	LAORepository

	// HasChannel returns true if the channel already exists.
	HasChannel(laoChannelPath string) (bool, error)

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// StoreMessageID stores a message ID inside the database.
	StoreMessageID(messageID, channel string) error
}

type RootRepository interface {

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// ChannelExists returns true if the channel already exists.
	ChannelExists(laoChannelPath string) (bool, error)

	// GetOwnerPubKey returns the public key of the owner of the server.
	GetOwnerPubKey() (kyber.Point, error)

	// StoreChannel stores a channel inside the database.
	StoreChannel(channel string, organizerPubKey []byte) error

	// GetClientServerAddress returns the address of the client server.
	GetClientServerAddress() (string, error)

	// GetServerPubKey returns the public key of the server.
	GetServerPubKey() ([]byte, error)

	// GetServerSecretKey returns the secret key of the server.
	GetServerSecretKey() ([]byte, error)
}

type ElectionRepository interface {

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	//IsElectionStarted returns true if the election is started.
	IsElectionStarted(electionID string) (bool, error)

	// IsElectionTerminated returns true if the election is terminated.
	IsElectionTerminated(electionID string) (bool, error)

	// GetLastVote returns the last vote of a sender in an election.
	GetLastVote(sender, electionID string) (messagedata.VoteCastVote, error)

	// GetResult returns the result of an election.
	GetResult(electionID string) (messagedata.ElectionResult, error)
}

type LAORepository interface {
	// GetLaoWitnesses returns the list of witnesses of a LAO.
	GetLaoWitnesses(laoPath string) (map[string]struct{}, error)
}
