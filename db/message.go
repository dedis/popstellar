package db

import (
	"encoding/json"
	"fmt"
	"github.com/boltdb/bolt"
	"student20_pop/define"
)

/**
* Writes a message to the database. If safe is true and a message with this ID already exists, returns an error
 */
func writeMessage(message define.Message, channel string, database string, creating bool) error {
	db, e := OpenDB(database)
	if e != nil {
		return e
	}
	defer db.Close()

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(channel))
		if err1 != nil {
			return define.ErrDBFault
		}

		if check := b.Get([]byte(message.Message_id)); check != nil && creating {
			return define.ErrResourceAlreadyExists
		}

		// Marshal the message and store it
		msg, err2 := json.Marshal(message)
		if err2 != nil {
			return define.ErrRequestDataInvalid
		}
		err := b.Put([]byte(message.Message_id), msg)
		if err != nil {
			return define.ErrDBFault
		}

		return nil
	})

	return err
}

/*writes a message to the DB, returns an error if ID already is key in DB*/
func CreateMessage(message define.Message, channel string, database string) error {
	return writeMessage(message, channel, database, true)
}

/*writes a message to the DB, regardless of ID already exists*/
func UpdateMessage(message define.Message, channel string, database string) error {
	return writeMessage(message, channel, database, false)
}

/*returns the content of a message sent on a channel. Nil if channel or DB does not exist*/
func GetMessage(channel []byte, message []byte, database string) []byte {
	db, err := OpenDB(database)
	if err != nil {
		return nil
	}
	defer db.Close()

	var data []byte
	err = db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket(channel)
		if b == nil {
			fmt.Printf("Could not find bucket with corresponding channel ID in GetMessage()")
			return define.ErrInvalidResource
		}

		data = b.Get(message)
		return nil
	})

	if err != nil {
		return nil
	}

	return data
}
