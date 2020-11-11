package db

import (
	"errors"
	"github.com/boltdb/bolt"
	"student20_pop/src"
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
 * Function will return an error if the DB was already initialized
 * TODO : is it useful ? do we need a count for the # of channels ?
 * if count not needed, we can remove this function
 */
func InitChannelDB(db *bolt.DB) error {
	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucket([]byte("ids"))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("count"), []byte("0"))
		return err1
	})
	return err
}

// TODO don't forget to increment count when adding a user


/**
 * Function to create a new user and store it in the DB
 * @returns : the id of the created user (+ event error)
 */
func CreateChannel(id string) (error) {

	db, e := OpenUserDB()
	defer db.Close()
	if e != nil {
		return nil, e
	}

	err := db.Update(func(tx *bolt.Tx) error {

		bkt := tx.Bucket([]byte("ids"))
		if bkt == nil {
			return errors.New("bkt does not exist")
		}

		// instantiate a user with no subscribe nor publish rights
		err1 = b.Put([]byte(id), []byte(""))
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
func UpdateChannelDB (userId []byte, channelId []byte, action []byte) error {

	//TODO correct the if checks
	// TODO create functions in jsonHelper addSubscribe, addPublish
	if(action == "subscriber") {
		updatedString := addSubscribe(oldString, channelId)
	} else if(action == "publisher") {
		updatedString := addPublish(oldString, channelId)
	} else {
		return errors.New("action not recognized")
	}

	db, e := OpenUserDB()
	defer db.Close()
	if e != nil {
		return lao, e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("ids"))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		err1 = b.Put(userid, updatedString)
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
func GetChannelDataFromID(userid []byte) ([]byte, error) {

	var data []byte

	db, e := OpenUserDB()
	defer db.Close()
	if e != nil {
		return lao, e
	}

	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("ids"))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		data = b.Get(userid)
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
