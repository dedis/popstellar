package channel

import (
	"encoding/json"
	"errors"
	"github.com/boltdb/bolt"
	"student20_pop/db"
	"student20_pop/define"
)

// would be nice to have an interface that contains methods add, remove and edit for LAO, event and vote

/**
 * Function to create a new LAO and store it in the DB
 * @returns : error
 */
func CreateLAO(data define.LAO) error {
	db, e := db.OpenChannelDB()
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err1 != nil {
			return err1
		}
		//checks if there is already an entry with that ID
		key := b.Get(data.ID)
		if key != nil {
			return errors.New("unable to create new LAO because of hash collision")
		}

		// Marshal the LAO and store it
		dt, err2 := json.Marshal(data)
		if err2 != nil {
			return err2
		}
		err3 := b.Put(data.ID, dt)

		return err3
	})

	return err
}

//TODO
func UpdateLao(lao define.LAO) error {
	// TODO adapt struct
	return nil
}

func GetFromID(id []byte) (define.LAO, error) {
	// TODO adapt struct
	//TODO
	return define.LAO{}, nil
}
