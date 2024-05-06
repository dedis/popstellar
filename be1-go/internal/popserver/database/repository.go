package database

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/popserver/types"
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
	PopChaRepository
	ReactionRepository

	// StoreMessageAndData stores a message with an object and an action inside the database.
	StoreMessageAndData(channelID string, msg message.Message) error

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

type LAORepository interface {
	// GetLaoWitnesses returns the list of witnesses of a LAO.
	GetLaoWitnesses(laoID string) (map[string]struct{}, error)

	// GetOrganizerPubKey returns the organizer public key of a LAO.
	GetOrganizerPubKey(laoID string) (kyber.Point, error)

	// GetRollCallState returns the state of th lao roll call.
	GetRollCallState(channel string) (string, error)

	// CheckPrevID returns true if the previous roll call message ID is the same as the next roll call message ID.
	CheckPrevID(channel, nextID, expectedState string) (bool, error)

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

type ElectionRepository interface {

	// GetLAOOrganizerPubKey returns the organizer public key of an election.
	GetLAOOrganizerPubKey(electionID string) (kyber.Point, error)

	// GetElectionSecretKey returns the secret key of an election.
	GetElectionSecretKey(electionID string) (kyber.Scalar, error)

	// IsElectionStartedOrTerminated returns true if the election is started or terminated.
	IsElectionStartedOrEnded(electionID string) (bool, error)

	// IsElectionTerminated returns true if the election is terminated.
	IsElectionEnded(electionID string) (bool, error)

	//IsElectionStarted returns true if the election is started.
	IsElectionStarted(electionID string) (bool, error)

	// GetElectionType returns the type of an election.
	GetElectionType(electionID string) (string, error)

	// GetElectionCreationTime returns the creation time of an election.
	GetElectionCreationTime(electionID string) (int64, error)

	// GetElectionAttendees returns the attendees of an election.
	GetElectionAttendees(electionID string) (map[string]struct{}, error)

	// GetElectionQuestions returns the questions of an election.
	GetElectionQuestions(electionID string) (map[string]types.Question, error)

	//GetElectionQuestionsWithValidVotes returns the questions of an election with valid votes.
	GetElectionQuestionsWithValidVotes(electionID string) (map[string]types.Question, error)

	// StoreMessageAndElectionResult stores a message and an election result message inside the database.
	StoreMessageAndElectionResult(channelID string, msg, electionResultMsg message.Message) error

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
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

type PopChaRepository interface {
	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}

type ReactionRepository interface {
	// IsAttendee returns if the user has participated in the last roll-call from the LAO
	IsAttendee(laoID string, userID string) (bool, error)

	// GetReactionSender returns a reaction sender
	GetReactionSender(ID string) (string, error)

	// StoreMessage stores a message inside the database.
	StoreMessage(channelID string, msg message.Message) error
}
