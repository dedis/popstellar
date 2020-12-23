package db

// This file contains functions used to deal with messages in the database. Like create/update a channel and
//get a message in particular.

import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/lib"
	"student20_pop/message"
)

// writeMessage  writes a message to the database. The argument "creating" is to specify whether or not
// we want to update a message, which means overwriting existing data
func writeMessage(message message.Message, channel string, database string, creating bool) error {
	db, e := OpenDB(database)
	if e != nil {
		return e
	}
	defer db.Close()

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(channel))
		if err1 != nil {
			return lib.ErrDBFault
		}

		check := b.Get(message.MessageId)

		if check != nil && creating {
			return lib.ErrResourceAlreadyExists
		}
		if check == nil && !creating {
			return lib.ErrInvalidResource
		}

		// Marshal the message and store it
		msg, err2 := json.Marshal(message)
		if err2 != nil {
			return lib.ErrRequestDataInvalid
		}
		err := b.Put(message.MessageId, msg)
		if err != nil {
			return lib.ErrDBFault
		}

		return nil
	})

	return err
}

//CreateMessage stores a new message in the database. It returns an error if a message with the same ID already existed
// in the same channel. It just calls writeMessage with creating = true
func CreateMessage(message message.Message, channel string, database string) error {
	return writeMessage(message, channel, database, true)
}

// UpdateMessage overwrites a message in the database. It returns an error if there was no message with the same ID in
// that channel. It just calls writeMessage with creating = false
func UpdateMessage(message message.Message, channel string, database string) error {
	return writeMessage(message, channel, database, false)
}

// GetMessage returns the content of a message sent on a channel. Nil if the message, channel or DB does not exist
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
			log.Printf("Could not find bucket with corresponding channel ID in GetMessage()")
			return lib.ErrInvalidResource
		}

		data = b.Get(message)
		return nil
	})

	if err != nil {
		return nil
	}
	return data
}
