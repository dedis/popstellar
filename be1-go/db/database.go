package db

import "student20_pop/message"

// Repository specifies an interface that the hub may use to interact with the
// underlying database
type Repository interface {

	// GetMessages returns all the messages for the given `channelID` sorted
	// in ascending order of their timestamp.
	GetMessages(channelID string) ([]message.Message, error)

	// GetMessagesInRange returns all the messages for the given `channelID`
	// in the given timestamp range (inclusive).
	GetMessagesInRange(channelID string, start, end message.Timestamp) ([]message.Message, error)

	// AddWitnessToMessage adds a witness signature and public key pair to the
	// message with `messageID` as the identifier.
	AddWitnessToMessage(messageID string, keyAndSignature message.PublicKeySignaturePair) error

	// AddMessage adds a new message to the channel specified by `channelID`
	AddMessage(channelID string, msg message.Message, timestamp message.Timestamp) error

	// Close frees up resources and closes the underlying connection to the database
	Close() error
}
