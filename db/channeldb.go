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

		// instantiate a user with no subscribe nor publish rights
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

/**
* Retrieve value from a given ID key, and update it with a new subscribtion or publish rights
* returns error message
 */
func UpdateChannelDB(userId []byte, channelId []byte, action []byte) error {

	//TODO correct the if checks
	// TODO create functions in jsonHelper addSubscribe, addPublish
	if action == "subscriber" {
		updatedString := Subscribe(oldString, channelId)
	} else if action == "publisher" {
		updatedString := addPublish(oldString, channelId)
	} else {
		return errors.New("action not recognized")
	}

	db, e := OpenChannelDB()
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("ids"))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		err1 := b.Put(userid, updatedString)
		if err1 != nil {
			return err1
		}
		return nil
	})

	return err
}

// ADD Helpers for UpdateChannelDB

/**
 * Returns a string which contains the subscribe and publish rights in the user database which matches the id passed an argument
 */
func GetChannelData(id []byte) ([]byte, error) {

	var data []byte

	db, e := OpenChannelDB()
	defer db.Close()
	if e != nil {
		return nil, e
	}

	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("ids"))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		data = b.Get(id)
		return nil
	})

	return data, err
}

//TODO move those functions to json helper but we might never need them
/*
func GetSubscribeOfUserFromId {
	data, err = GetUserDataFromID(userid)
	//slice json
}

func GetPublishOfUserFromId
*/
