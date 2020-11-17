package db

import (
	"errors"
	"github.com/boltdb/bolt"
)

const DatabaseChannel = "channel.db"

/**
 * opens the User DB. creates it if not exists.
 * don't forget to close the database afterwards
 */
func OpenChannelDB() (*bolt.DB, error) {
	return OpenDB(DatabaseChannel)
}

/**
 * Function to create a new lao in the ChannelDB
 * @param : a src.MessageLaoCreate. all fields are stored in DB
 * @returns : the id of the created user (+ event error)
 */
func CreateChannel(id string) error {

	db, e := OpenChannelDB()
	defer db.Close()
	if e != nil {
		return e
	}
	err := db.Update(func(tx *bolt.Tx) error {

		bkt := tx.Bucket([]byte("ids"))
		if bkt == nil {
			return errors.New("bkt does not exist")
		}

		// instantiate a channel with no subscriber and no publisher
		err1 := bkt.Put([]byte(id), []byte(""))
		if err1 != nil {
			return err1
		}
		return nil
	})

	return err
}

/**
 * Check that the attestation of a user is correct
 */
/*
func checkChannelValidity(id []byte) bool {
	user, err := GetFromID(id)
	attestation := lao.Attestation

	//TODO do something??

	return computed == attestation
}*/
