package channel

import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"student20_pop/db"
	"student20_pop/define"
)

// would be nice to have an interface that contains methods add, remove and edit for LAO, event and vote

/**
 * Function to create a new LAO and store it in the DB
 * @returns : error
 */
func writeLAO(lao define.Message, canal string, secure bool) error {
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
		//checks if there is already an entry with that ID if secure is true
		if secure {
			key := b.Get([]byte (canal))
			if key != nil {
				return define.ErrResourceAlreadyExists
			}
		} else {
			exists := b.Get([]byte (canal))
			if exists == nil {
				return define.ErrInvalidResource
			}
		}
		// Marshal the LAO and store it
		dt, err2 := json.Marshal(lao)
		if err2 != nil {
			return define.ErrRequestDataInvalid
		}
		err3 := b.Put([]byte (canal), dt)

		return err3
	})

	return err
}

/*writes a lao to the DB, returns an error if ID already is key in DB*/
func CreateLAO(lao define.Message, canal string) error {
	return writeLAO(lao, canal, true)
}


// TODO Update must append !!! Not Overwrite !!!!!!!!!!!!!!!!!!
// TODO merge channel and LAO ?
/*writes a lao to the DB, regardless of ID already exists*/
func UpdateLao(lao define.Message, canal string) error {
	return writeLAO(lao, canal, false)
}

/*returns channel data from a given ID */
func GetFromID(id []byte) []byte {
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
