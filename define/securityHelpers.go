package define

import "time"

const MaxSecondsElapsedBetweenLAOCreationAndPublish = 600

func LAOCreatedIsValid(data DataCreateLAO, message Message) bool {
	isValid := true
	//the last modified timestamp is equal to the creation timestamp,
	if data.Creation != data.LastModified {
		isValid = false
	}
	//the timestamp is reasonably recent with respect to the server’s clock,
	if data.Creation > time.Now().Unix() || data.Creation-time.Now().Unix() > MaxSecondsElapsedBetweenLAOCreationAndPublish {
		isValid = false
	}
	//the attestation is valid,
	// TODO hash function
	if message.MessageID != "0" {//hashTBD(message.Data+message.Signature) {
		isValid = false
	}
	//potentially more checks
	return isValid
}
