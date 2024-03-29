package hub

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

type Repository interface {
	ChannelRepository

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// GetMessagesByID returns a set of messages by their IDs.
	GetMessagesByID(IDs []string) (map[string]message.Message, error)

	// GetMessageByID returns a message by its ID.
	GetMessageByID(ID string) (message.Message, error)

	// GetIDsTable returns the map of message IDs by channelID.
	GetIDsTable() (map[string][]string, error)
}

type ChannelRepository interface {
	ElectionRepository
	RootRepository
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

type RootRepository interface {

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// Haslao returns true if LAO already exists.
	Haslao(laoChannelPath string) (bool, error)

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
