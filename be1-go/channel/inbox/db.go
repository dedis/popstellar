package inbox

import (
	"database/sql"
	"log"
	"popstellar/db/sqlite"
	"popstellar/message/query/method/message"

	"golang.org/x/xerrors"
)

const (
	dbPrepareErr  = "failed to prepare query: %v"
	dbParseRowErr = "failed to parse row: %v"
	dbRowIterErr  = "error in row iteration: %v"
	dbQueryRowErr = "failed to query rows: %v"
)

// storeMessageInDB stores a message into the db. It should be used in case the
// `HUB_DB` env variable is set.
func (i *Inbox) storeMessageInDB(messageInfo *messageInfo) error {
	db, err := sql.Open("sqlite3", sqlite.GetDBPath())
	if err != nil {
		return xerrors.Errorf("failed to open connection: %v", err)
	}

	defer db.Close()

	// We can have multiple channels saving the same message. In that case we
	// ignore if the message already exists.
	query := `
		INSERT OR IGNORE INTO 
			message_info(
				message_id, 
				sender, 
				message_signature, 
				raw_data, 
				message_timestamp, 
				lao_channel_id) 
		VALUES(?, ?, ?, ?, ?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	msg := messageInfo.message

	_, err = stmt.Exec(msg.MessageID, msg.Sender, msg.Signature, msg.Data, messageInfo.storedTime, i.channelID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	for _, pubKeySigPair := range messageInfo.message.WitnessSignatures {
		err = addWitnessInDB(db, string(messageInfo.message.MessageID),
			pubKeySigPair.Witness, pubKeySigPair.Signature)
		if err != nil {
			return xerrors.Errorf("failed to store witness: %v", err)
		}
	}

	return nil
}

func addWitnessInDB(db *sql.DB, messageID string, pubKey string, signature string) error {
	query := `
		INSERT INTO
			message_witness(
				pub_key, 
				witness_signature, 
				message_id) 
		VALUES(?, ?, ?)`

	stmt, err := db.Prepare(query)
	if err != nil {
		return xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	_, err = stmt.Exec(pubKey, signature, messageID)
	if err != nil {
		return xerrors.Errorf("failed to exec query: %v", err)
	}

	return nil
}

// CreateInboxFromDB creates an inbox from a database
func CreateInboxFromDB(db *sql.DB, channelID string) (*Inbox, error) {
	inbox := NewInbox(channelID)

	query := `
		SELECT
			message_id, 
			sender, 
			message_signature, 
			raw_data, 
			message_timestamp
		FROM
			message_info
		WHERE
			lao_channel_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(channelID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	for rows.Next() {
		var messageID string
		var sender string
		var messageSignature string
		var rawData string
		var timestamp int64

		err = rows.Scan(&messageID, &sender, &messageSignature, &rawData, &timestamp)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		witnesses, err := getWitnessesMessageFromDB(db, messageID)
		if err != nil {
			return nil, xerrors.Errorf("failed to get witnesses: %v", err)
		}

		messageInfo := messageInfo{
			message: message.Message{
				MessageID:         messageID,
				Sender:            sender,
				Signature:         messageSignature,
				WitnessSignatures: witnesses,
				Data:              rawData,
			},
			storedTime: timestamp,
		}

		log.Printf("Msg load: %+v", messageInfo.message)

		inbox.msgs[messageID] = &messageInfo
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return inbox, nil
}

func getWitnessesMessageFromDB(db *sql.DB, messageID string) ([]message.WitnessSignature, error) {
	query := `
		SELECT
			pub_key,
			witness_signature
		FROM
			message_witness
		WHERE
			message_id = ?`

	stmt, err := db.Prepare(query)
	if err != nil {
		return nil, xerrors.Errorf(dbPrepareErr, err)
	}

	defer stmt.Close()

	rows, err := stmt.Query(messageID)
	if err != nil {
		return nil, xerrors.Errorf(dbQueryRowErr, err)
	}

	defer rows.Close()

	result := make([]message.WitnessSignature, 0)

	for rows.Next() {
		var pubKey string
		var signature string

		err = rows.Scan(&pubKey, &signature)
		if err != nil {
			return nil, xerrors.Errorf(dbParseRowErr, err)
		}

		result = append(result, message.WitnessSignature{
			Witness:   pubKey,
			Signature: signature,
		})
	}

	err = rows.Err()
	if err != nil {
		return nil, xerrors.Errorf(dbRowIterErr, err)
	}

	return result, nil
}
