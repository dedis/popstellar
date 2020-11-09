package db

import (
	"crypto/sha1"
	"errors"
	"github.com/boltdb/bolt"
	"strconv"
	"../src"
)

const DatabaseLao = "LAO.db"

// would be nice to have an interface that contains methos add, remove and edit for LAO, event and vote

/*
 * opens the LAO DB. creates it if not exists.
 * don't forget to close the database afterwards
 * TODO : Keep it ? or put everything in the Channel DB ?
 */
func OpenLAODB() (*bolt.DB, error) {
	return OpenDB(DatabaseLao)
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
func CreateLAO(create src.MessageLaoCreate) ([]byte, error) {

	db, e := OpenLAODB()
	defer db.Close()
	if e != nil {
		return nil, e
	}

	ctime := create.Timestamp
	id := generateID(ctime, create.Name)

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
		err1 = b.Put([]byte("name"), create.Name)
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("timestamp"), make([]byte, ctime))
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("organizerPkey"), create.OrganizerPkey)
		if err1 != nil {
			return err1
		}
		err1 = b.Put([]byte("IP"), create.Ip)
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
 * //TODO on garde ou on tej' nous on doit pas générer on doit check validity il me semble
 */
func generateID(timestamp int64, name []byte) []byte {
	h := sha1.New()
	h.Write(append(name, make([]byte, timestamp)...))

	str := h.Sum(nil)
	return str
}

func GetFromID(id []byte) (src.LAO, error) {
	//TODO
	return src.LAO{}, errors.New("empty")
}
