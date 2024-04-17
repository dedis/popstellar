package database

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/message/messagedata"
	"popstellar/message/query/method/message"
)

type Repository interface {
	QueryRepository
	AnswerRepository
	ChannelRepository
	RootRepository
	ElectionRepository
	LAORepository
	ChirpRepository
	CoinRepository
	ConsensusRepository
	GeneralChirpRepository
	PopChaRepository

	// StoreMessageWithObjectAction stores a message with an object and an action inside the database.
	StoreMessageWithObjectAction(channelID, object, action string, msg message.Message) error

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
	// GetMessagesByID returns a set of messages by their IDs.
	GetMessagesByID(IDs []string) (map[string]message.Message, error)

	// GetMessageByID returns a message by its ID.
	GetMessageByID(ID string) (message.Message, error)
}

// ======================= Query ==========================

type QueryRepository interface {
	GetResultForGetMessagesByID(params map[string][]string) (map[string][]message.Message, error)

	// GetParamsForGetMessageByID returns the params to do the getMessageByID msg in reponse of heartbeat
	GetParamsForGetMessageByID(params map[string][]string) (map[string][]string, error)

	// GetAllMessagesFromChannel return all the messages received + sent on a channel
	GetAllMessagesFromChannel(channelID string) ([]message.Message, error)
}

// ======================= Answer ==========================

type AnswerRepository interface {
	StorePendingMessages(msgs map[string]map[string]message.Message) error
}

// ======================= Channel ==========================

type ChannelRepository interface {
	// HasChannel returns true if the channel already exists.
	HasChannel(channel string) (bool, error)

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)

	// GetChannelType returns the type of the channel.
	GetChannelType(channel string) (string, error)
}

type RootRepository interface {

	// StoreChannelsAndMessageWithLaoGreet stores a list of "sub" channels, a message and a lao greet message inside the database.
	StoreChannelsAndMessageWithLaoGreet(
		channels map[string]string,
		laoID string,
		organizerPubBuf []byte,
		msg, laoGreetMsg message.Message) error

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// HasChannel returns true if the channel already exists.
	HasChannel(channel string) (bool, error)
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
	// GetElectionQuestions(electionID string) (map[string]channel.Question, error)

	// GetElectionType returns the type of an election.
	GetElectionType(electionID string) (string, error)

	// StoreCastVote stores a cast vote message inside the database.
	StoreCastVote(electionID string, msg message.Message, vote messagedata.VoteCastVote) error

	// GetResult returns the result of an election.
	GetResult(electionID string) (messagedata.ElectionResult, error)

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type LAORepository interface {
	// GetLaoWitnesses returns the list of witnesses of a LAO.
	GetLaoWitnesses(laoPath string) (map[string]struct{}, error)

	// GetOrganizerPubKey returns the organizer public key of a LAO.
	GetOrganizerPubKey(laoPath string) (kyber.Point, error)

	// GetRollCallState returns the state of th lao roll call.
	GetRollCallState(channel string) (string, error)

	// CheckPrevID returns true if the previous roll call message ID is the same as the next roll call message ID.
	CheckPrevID(channel string, nextID string) (bool, error)

	// StoreChannelsAndMessage stores a list of "sub" channels and a message inside the database.
	StoreChannelsAndMessage(channels []string, laoID string, msg message.Message) error

	// StoreMessageWithElectionKey stores a message and a election key message inside the database.
	StoreMessageWithElectionKey(
		laoID, electionID string,
		electionPubKey kyber.Point,
		electionSecretKey kyber.Scalar,
		msg, electionKeyMsg message.Message) error

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)
}

type ChirpRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error

	// HasMessage returns true if the message already exists.
	HasMessage(messageID string) (bool, error)
}

type CoinRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type ConsensusRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type GeneralChirpRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type PopChaRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type ReactionRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}
