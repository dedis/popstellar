package define

import "time"

const MaxSecondsElapsedBetweenLAOCreationAndPublish = 600

func LAOCreatedIsValid(data DataCreateLAO, message Message) error {
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		return ErrInvalidResource
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxSecondsElapsedBetweenLAOCreationAndPublish {
		return ErrInvalidResource
	}
	//the attestation is valid,
	// TODO hash function
	if message.MessageID != "0" {//hashTBD(message.Data+message.Signature) {
		return ErrInvalidResource
	}
	//potentially more checks
	return nil
}
