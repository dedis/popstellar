package repo

import (
	"go.dedis.ch/kyber/v3"
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

	// GetAllMessagesFromChannel return all the messages received + sent on a channel
	GetAllMessagesFromChannel(channelID string) ([]message.Message, error)

	GetChannelType(channel string) (string, error)
}

// ======================= Answer ==========================

type HandleAnswerRepository interface {
	StorePendingMessages(msgs map[string]map[string]message.Message) error
}

// ======================= Channel ==========================

type HandleChannelRepository interface {
	RootRepository
	ElectionRepository
	LAORepository

	// StoreChannel stores a channel inside the database.
	StoreChannel(channel string) error

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// HasChannel returns true if the channel already exists.
	HasChannel(laoChannelPath string) (bool, error)

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// StoreChannelsAndMessage stores a list of "sub" channels and a message inside the database.
	StoreChannelsAndMessage(channels []string, baseChannel string, msg message.Message) error
}

type RootRepository interface {

	// ChannelExists returns true if the channel already exists.
	ChannelExists(laoChannelPath string) (bool, error)

	// GetOwnerPubKey returns the public key of the owner of the server.
	GetOwnerPubKey() (kyber.Point, error)

	// GetClientServerAddress returns the address of the client server.
	GetClientServerAddress() (string, error)

	// StoreChannelsAndMessageWithLaoGreet stores a list of "sub" channels, a message and a lao greet message inside the database.
	StoreChannelsAndMessageWithLaoGreet(
		channels []string,
		baseChannel, channelRelation, messageIDRelation string,
		organizerPubBuf []byte,
		msg, laoGreetMsg message.Message) error
}

type ElectionRepository interface {

	//IsElectionStarted returns true if the election is started.
	IsElectionStarted(electionID string) (bool, error)

	// IsElectionTerminated returns true if the election is terminated.
	IsElectionTerminated(electionID string) (bool, error)

	// IsElectionStartedOrTerminated returns true if the election is started or terminated.
	IsElectionStartedOrTerminated(electionID string) (bool, error)

	// GetElectionCreationTime returns the creation time of an election.
	GetElectionCreationTime(electionID string) (int64, error)

	// GetLastVote returns the last vote of a sender in an election.
	GetLastVote(sender, electionID string) (messagedata.VoteCastVote, error)

	// GetElectionAttendees returns the attendees of an election.
	GetElectionAttendees(electionID string) (map[string]struct{}, error)

	// GetElectionQuestions returns the questions of an election.
	//GetElectionQuestions(electionID string) (map[string]channel.Question, error)

	// GetElectionType returns the type of an election.
	GetElectionType(electionID string) (string, error)

	// StoreCastVote stores a cast vote message inside the database.
	StoreCastVote(electionID string, msg message.Message, vote messagedata.VoteCastVote) error

	// GetResult returns the result of an election.
	GetResult(electionID string) (messagedata.ElectionResult, error)

	// CheckPrevID returns true if the previous roll call message ID is the same as the next roll call message ID.
	CheckPrevID(channel string, nextID string) (bool, error)

	// GetRollCallState returns the state of th lao roll call.
	GetRollCallState(channel string) (string, error)

	// StoreMessageWithElectionKey stores a message and a election key message inside the database.
	StoreMessageWithElectionKey(
		baseChannel, channelRelation, messageIDRelation string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg, electionKey message.Message) error
}

type LAORepository interface {
	// GetLaoWitnesses returns the list of witnesses of a LAO.
	GetLaoWitnesses(laoPath string) (map[string]struct{}, error)

	// GetOrganizerPubKey returns the organizer public key of a LAO.
	GetOrganizerPubKey(laoPath string) (kyber.Point, error)
}
