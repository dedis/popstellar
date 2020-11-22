package channel

import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"student20_pop/db"
	"student20_pop/define"
)

// would be nice to have an interface that contains methods add, remove and edit for message, event and vote

/**
 * Function to create a new message and store it in the DB
 * @returns : error
 */
func writeMessage(message define.Message, canal string, creating bool) error {
	db, e := db.OpenDB(channelDatabase)
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err1 != nil {
			return err1
		}
		var current []byte
		//checks if there is already an entry with that ID if secure is true
		if creating {
			key := b.Get([]byte (canal))
			if key != nil {
				return define.ErrResourceAlreadyExists
			}
		} else {
			current = b.Get([]byte (canal))
			if current == nil {
				return define.ErrInvalidResource
			}
		}
		// Marshal the message and store it
		dt, err2 := json.Marshal(message)
		if err2 != nil {
			return define.ErrRequestDataInvalid
		}
		if current != nil {
			// TODO add a delimiter in-between to simplify parsing?
			dt = append(current, dt...)
		}
		err3 := b.Put([]byte (canal), dt)

		return err3
	})

	return err
}

/*writes a message to the DB, returns an error if ID already is key in DB*/
func StoreMessage(message define.Message, canal string) error {
	return writeMessage(message, canal, true)
}


// TODO merge channel and message ?
/*writes a message to the DB, regardless of ID already exists*/
func UpdateMessage(message define.Message, canal string) error {
	return writeMessage(message, canal, false)
}

/*returns channel data from a given ID */
func GetMessageFromID(id []byte) []byte {
	db, e := db.OpenDB(channelDatabase)
	defer db.Close()
	if e != nil {
		return nil
	}
	var data []byte
	e = db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucketChannel))
		data = b.Get(id)
		return nil
	})

	return data
}
