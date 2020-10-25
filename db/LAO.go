package db

import (
	"github.com/boltdb/bolt"
	"strconv"
	src "student20_pop/classes"
)

const LaoDBName = "LAO.db"

// would be nice to have an interface that contains methos add, remove and edit for LAO, event and vote

/*
 * opens the LAO DB. creates it if not exists.
 * don't forget to close the database afterwards
 */
func OpenLAODB() (*bolt.DB, error) {
	db, err := bolt.Open(LaoDBName, 0600, nil)
	if err != nil {
		return nil, err
	}

	return db, nil
}

/**
 * Function will return an error if the DB was already initialized
 */
func InitLAODB(db *bolt.DB) error {
	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucket([]byte("general"))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("count"), []byte(strconv.Itoa(0)))
		return err1
	})
	return err
}

/**
 * Function to create a new LAO and store it in the DB
 */

func CreateLAO(lao src.LAO, db *bolt.DB) error {
	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucket([]byte(strconv.Itoa(lao.Id))) //TODO find a way to convert hash to []byte .Sum() function maybe?
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("name"), []byte(lao.Name))
		if err1 != nil {
			return err1
		}

		err1 = b.Put([]byte("timestamp"), lao.Timestamp)
		err1 = b.Put([]byte("organizerPkey"), lao.OrganizerPKey)
		err1 = b.Put()
		err1 = b.Put()
		err1 = b.Put()

	})
	return err
}