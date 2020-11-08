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

/**
 * Function to create a new lao in the ChannelDB
 * @param : a src.MessageLaoCreate. all fields are stored in DB
 * @returns : the id of the created user (+ event error)
 */
func CreateChannel(lao src.MessageLaoCreate) error {

	db, e := OpenChannelDB()
	defer db.Close()
	if e != nil {
		return e
	}

	//see create LAO for an example. Do we want to keep this function ?
	//I mean do we want a generic function or 1 function per type of channel ?
	// I think option 2 is better @ouriel @raoul ?

	return nil
}

/**
 * Check that the attestation of a user is correct
 */
func checkChannelValidity(id []byte) bool {
	//TODO later
	return true
}

/**
* Updates a channel by adding a publisher or subscriber to it
* returns error message
 */
func UpdateChannelDB(userId []byte, channelId []byte, action []byte) error {

	//TODO correct the if checks
	// TODO create functions in jsonHelper addSubscribe, addPublish
	switch action {
	case []byte("subscriber"):
		return addSubscriber(userId, channelId)
	case []byte("publisher"):
		return addPublisher(userId, channelId)
	default:
		return errors.New("action not recognized")
	}

}

/**
* function to add a publisher to a channel
 */
func addPublisher(id []byte, channel []byte) error {
	//TODO
	return nil
}

/**
* function to add a subsciber to a channel
 */
func addSubscriber(id []byte, channel []byte) error {
	//TODO
	return nil
}

/**
 * Returns a string which contains the Data of the requested Channel
 */
func GetChannelData(id []byte) ([]byte, error) {

	db, e := OpenChannelDB()
	if e != nil {
		return nil, e
	}
	defer db.Close()

	var data []byte

	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket(id)
		if b == nil {
			return errors.New("Channel with ID " + string(id) + "does not exist")
		}

		err1 := b.ForEach(func(k, v []byte) error {
			//TODO what should go into that string ? Formatted how ?
			data = append(data, k...)
			data = append(data, v...)
			return nil
		})
		return err1
	})

	return data, err
}
