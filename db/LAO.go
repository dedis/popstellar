package db

import (
	"crypto/sha1"
	"errors"
	"github.com/boltdb/bolt"
	"strconv"
	src "student20_pop/classes"
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
		b.CreateBucket([]byte("witnesses"))
		b.CreateBucket([]byte("members"))
		b.CreateBucket([]byte("events"))
		// TODO: to get the attestation signed by the organizer, we need to send the LAO ID back to the Organizer so that they answer with the hash. 
		// might need to add a validation function which blocks actions from being taken on the LAO if attestation is invalid??
		err1 = b.Put([]byte("attestation"), []byte(""))
		if err1 != nil {
			return err1
		}

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

/**
 * Check that the attestation of an LAO is correct
 */
func checkValidity(id []byte) bool {
	lao, err := GetFromID(id)
	attestation := lao.Attestation

	h := sha1.New()
	h.Write(append([]byte(lao.Name), []byte(lao.Timestamp), []byte(lao.Witnesses)))
	computed := h.Sum(nil)

	return computed == attestation
}


/**
 * Returns an LAO struct based on the LAO in the database which matches the id passed an argument
 */
func GetFromID(id []byte) (src.LAO, error) {

	var lao src.LAO

	db, e := OpenLAODB()
	defer db.Close()
	if e != nil {
		return lao, e
	}


	
	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte("general")).Bucket(id)
		if b == nil {
			return errors.New("bkt does not exist")
		}

		// TODO right now, we get byte arrays from the db. and the LAO struct wants ints and else. We need to choose to either use only []bytes and strings, or write helpers functions to convert everything in-between
		lao.Name = string(b.Get([]byte("name")))
		lao.Timestamp = binary.ReadVarint(b.Get([]byte("timestamp")))
		lao.Id = id
		lao.OrganizerPKey = b.Get([]byte("organizerPkey"))
		lao.Witnesses = NestedToList(b.Bucket([]byte("witnesses")))
		lao.Members = NestedToList(b.Bucket([]byte("members")))
		lao.Events = NestedToList(b.Bucket([]byte("events")))
		lao.Attestation = b.Get([]byte("attestation"))
		//lao.TokensEmitted = NestedToList(b.Bucket("???"))

		return nil
	})

	return lao, err
}
