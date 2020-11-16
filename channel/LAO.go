package channel

import (
	"errors"
	"github.com/boltdb/bolt"
	"student20_pop/db"
	"student20_pop/define"
)

const DatabaseLao = "channel.db"

// would be nice to have an interface that contains methods add, remove and edit for LAO, event and vote

/*
 * opens the LAO DB. creates it if not exists.
 * don't forget to close the database afterwards
 * TODO : Keep it ? or put everything in the Channel DB ?
 */
func OpenLAODB() (*bolt.DB, error) {
	return db.OpenDB(DatabaseLao)
}

/**
 * Function to create a new LAO and store it in the DB
 * @returns : error
 */
func CreateLAO(data define.LAO) error {
	// TODO openLAODB might change if we have a single DB
	db, e := OpenLAODB()
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte("LAO"))
		if err1 != nil {
			return err1
		}

		key := b.Get(data.ID)
		if key != nil {
			return errors.New("unable to create new LAO because of hash collision")
		}

		// Marshal the LAO and store it
		dt, err2 := json.Marshal(data)
		if err2 != nil {
			return err2
		}
		err3 := bkt.Put(data.ID, dt)
		if err3 != nil {
			return err3
		}

		return nil
	})

	//TODO cleanup if failed
	return err
}

//TODO
func UpdateLao(lao channel.LAO) error {
	// TODO adapt struct
	return nil
}

func GetFromID(id []byte) (channel.LAO, error) {
	// TODO adapt struct
	//TODO
	return channel.LAO{}, nil
}
