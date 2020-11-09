package db

import (
	"errors"
	"github.com/boltdb/bolt"
	"strconv"
)

const DatabaseUser = "user.db"

/**
ça sera quoi la tronche de notre user db ?
1) est-ce qu'on a besoin de pouvoir implémenter une méthode FetchAllSubscribedChannels ?
2) je pense qu'on va garder une user database juste pour pouvoir mapper un userID avec une connexion Websocket
*/

/*
 * opens the User DB. creates it if not exists.
 * don't forget to close the database afterwards
 */
func OpenUserDB() (*bolt.DB, error) {
	return OpenDB(DatabaseUser)
}

/**
 * Function will return an error if the DB was already initialized
 * TODO : remove if keeping a count is unuseful
 */
func InitUserDB(db *bolt.DB) error {
	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucket([]byte("ids"))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("count"), []byte(strconv.Itoa(0)))
		return err1
	})
	return err
}

// TODO don't forget to increment count when adding a user
/**
 * Function to create a new user and store it in the DB
 * @returns : an error if could not create user
 */
func CreateUser(id string) error {

	db, e := OpenUserDB()
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
func checkUserValidity(id []byte) bool {
	//TODO later
	return true
}

/**
 * Retrieve value from a given ID key, and update it with a new subscribtion or publish rights
 * returns error message
 */
func SubscribeUserDB(userId []byte, channelId []byte) error {

	//TODO correct the if checks
	//TODO create functions in jsonHelper addSubscribe, addPublish

	updatedString := addSubscribe(oldString, channelId)

	db, e := OpenUserDB()
	if e != nil {
		return e
	}
	defer db.Close()

	err := db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("ids"))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		err1 := b.Put(userId, updatedString)
		if err1 != nil {
			return err1
		}
		return nil
	})

	return err
}

/**
 * Returns a string which contains the subscribe and publish rights
 * in the user database which matches the id passed an argument
 */
func GetUserDataFromID(userid []byte) ([]byte, error) {

	var data []byte

	db, e := OpenUserDB()
	defer db.Close()
	if e != nil {
		return nil, e
	}

	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket(userid)
		if b == nil {
			return errors.New("bkt does not exist")
		}

		data = b.Get(userid) //TODO
		return nil
	})

	return data, err
}

/**
 * check if user is already registered
 */
func alreadyRegister(userid []byte) (bool, error) {
	//TODO
	bkt, err := GetUserDataFromID(userid)
	already := (bkt!=nil)
	return already, err
}
//TODO move those functions to json helper if we need them
/*
func GetSubscribeOfUserFromId {
	data, err = GetUserDataFromID(userid)
	//slice json
}

func GetPublishOfUserFromId
*/
