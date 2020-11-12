package db

import (
	"errors"
	"github.com/boltdb/bolt"
	"student20_pop/channel"
)

const DatabaseLao = "channel.db"

// would be nice to have an interface that contains methods add, remove and edit for LAO, event and vote

/*
 * opens the LAO DB. creates it if not exists.
 * don't forget to close the database afterwards
 * TODO : Keep it ? or put everything in the Channel DB ?
 */
func OpenLAODB() (*bolt.DB, error) {
	return OpenDB(DatabaseLao)
}

/**
 * Function to create a new LAO and store it in the DB
 * @returns : the id of the created LAO (+ event error)
 */
func CreateLAO(create channel.MessageLaoCreate) error {

	db, e := OpenLAODB()
	defer db.Close()
	if e != nil {
		return e
	}

	ctime := create.Timestamp

	err := db.Update(func(tx *bolt.Tx) error {

		bkt := tx.Bucket([]byte("general"))
		if bkt == nil {
			return errors.New("bkt does not exist")
		}

		b := bkt.Bucket(create.ID)
		if b != nil {
			return errors.New("unable to create new lao because of hash collision")
		}
		//create bkt for the lao
		b, err1 := bkt.CreateBucket(create.ID)
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
	return err
}

//TODO
func WriteLao(lao channel.LAO) error {
	return nil
}

func GetFromID(id []byte) (channel.LAO, error) {
	//TODO
	return channel.LAO{}, errors.New("empty")
}
