package db

import (
	"crypto/sha1"
	"errors"
	"github.com/boltdb/bolt"
	"strconv"
	"time"
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
 * @returns : the id of the created LAO (+ event error)
 */
func CreateLAO(name string, OrganizerPublicKey string, ip string) ([]byte, error) {

	db, e := OpenLAODB()
	defer db.Close()
	if e != nil {
		return nil, e
	}

	ctime := time.Now().Unix()
	id := generateID(ctime, name)

	err := db.Update(func(tx *bolt.Tx) error {

		bkt := tx.Bucket([]byte("general"))
		if bkt == nil {
			return errors.New("bkt does not exist")
		}

		b := bkt.Bucket(id)
		if b != nil {
			return errors.New("unable to create new lao because of hash collision")
		}
		//create bkt for the lao
		b, err1 := bkt.CreateBucket(id)
		if err1 != nil {
			return err1
		}

		// instantiate the lao
		err1 = b.Put([]byte("name"), []byte(name))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("timestamp"), make([]byte, ctime))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("organizerPkey"), []byte(OrganizerPublicKey))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("IP"), []byte(ip))
		if err1 != nil {
			return err1
		}
		b.CreateBucket([]byte("witness"))
		b.CreateBucket([]byte("member"))
		b.CreateBucket([]byte("event"))
		b.CreateBucket([]byte("signature"))

		return nil
	})

	//TODO cleanup if failed
	if err != nil {
		id = nil
	}
	return id, err
}

/**
 * Generate a hash for ID of LAO
 */
func generateID(timestamp int64, name string) []byte {
	h := sha1.New()
	h.Write(append([]byte(name), make([]byte, timestamp)...))

	str := h.Sum(nil)
	return str
}
