package define

import (
	"bytes"
	"crypto/sha256"
	"strconv"
	"time"
)

const MaxTimeBetweenLAOCreationAndPublish = 600

// TODO if we use the json Schema, don't need to check structure correctness
func LAOCreatedIsValid(data DataCreateLAO, message Message) error {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		return ErrInvalidResource
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxTimeBetweenLAOCreationAndPublish {
		return ErrInvalidResource
	}
	//the attestation is valid,
	str := []byte(data.OrganizerPKey)
	str = append(str, []byte(strconv.FormatInt(data.Creation, 10))...)
	str = append(str, []byte(data.Name)...)
	hash := sha256.Sum256(str)
	if !bytes.Equal([]byte(message.Message_id), hash[:]) {
		return ErrInvalidResource
	}

	return nil
}

func MeetingCreatedIsValid(data DataCreateMeeting, message Message) error {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		return ErrInvalidResource
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxTimeBetweenLAOCreationAndPublish {
		return ErrInvalidResource
	}

	//we start after the creation and we end after the start
	if data.Start < data.Creation || data.End < data.Start {
		return ErrInvalidResource
	}
	//need to meet somewhere
	if data.Location == "" {
		return ErrInvalidResource
	}
	return nil
}
func PollCreatedIsValid(data DataCreatePoll, message Message) error {
	return nil
}
func RollCallCreatedIsValid(data DataCreateRollCall, message Message) error {
	return nil
}

func MessageIsValid(msg Message) error {
	return nil
}
