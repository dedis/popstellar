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
func writeLAO(lao define.LAO, secure bool) error {
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
			key := b.Get(lao.ID)
			if key != nil {
				return define.ErrResourceAlreadyExists
			}
		} else {
			exists := b.Get(lao.ID)
			if exists == nil {
				return define.ErrInvalidResource
			}
		}
		// Marshal the LAO and store it
		dt, err2 := json.Marshal(lao)
		if err2 != nil {
			return err2
		}
		err3 := b.Put(lao.ID, dt)

		return err3
	})

	return err
}

/*writes a lao to the DB, returns an error if ID already is key in DB*/
func CreateLAO(lao define.LAO) error {
	return writeLAO(lao, true)
}

/*writes a lao to the DB, regardless of ID already exists*/
func UpdateLao(lao define.LAO) error {
	return writeLAO(lao, false)
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
