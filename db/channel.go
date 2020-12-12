/* This file contains functions used to deal with channels in the database. Like create/update a channel and
get infos about a channel. */

package db

import (
	"encoding/json"
	"fmt"
	"github.com/boltdb/bolt"
	"student20_pop/event"
	"student20_pop/lib"
)

const bucketChannel = "channels"

/**
 * Function to create a new Object (LAO,Event...) and store it in the DB
 * @returns : error
 */
func writeChannel(obj interface{}, database string, secure bool) error {
	db, e := OpenDB(database)
	if e != nil {
		return e
	}
	defer db.Close()

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err1 != nil {
			return err1
		}
		//generic adaptation
		var objID []byte
		switch obj.(type) {
		case event.LAO:
			// type assert
			objID = []byte(obj.(event.LAO).ID)
		case event.Meeting:
			objID = []byte(obj.(event.Meeting).ID)
		case event.Poll:
			objID = []byte(obj.(event.Poll).ID)
		case event.RollCall:
			objID = []byte(obj.(event.RollCall).ID)
		default:
			return lib.ErrRequestDataInvalid
		}
		//checks if there is already an entry with that ID if secure is true
		if secure {
			key := b.Get(objID)
			if key != nil {
				return lib.ErrResourceAlreadyExists
			}
		} else {
			exists := b.Get(objID)
			if exists == nil {
				fmt.Printf("Could not find (key, val) pair to update in write channel with param secure=false")
				return lib.ErrInvalidResource
			}
		}
		var dt []byte
		var err2 error
		switch obj.(type) {
		case event.LAO:
			// type assert
			dt, err2 = json.Marshal(obj.(event.LAO).ID)
		case event.Meeting:
			dt, err2 = json.Marshal(obj.(event.Meeting).ID)
		case event.Poll:
			dt, err2 = json.Marshal(obj.(event.Poll).ID)
		case event.RollCall:
			dt, err2 = json.Marshal(obj.(event.RollCall).ID)
		default:
			return lib.ErrRequestDataInvalid
		}
		// Marshal the Obj and store it
		if err2 != nil {
			return lib.ErrRequestDataInvalid
		}
		err3 := b.Put(objID, dt)
		return err3
	})

	return err
}

/*writes a channel (LAO, meeting, rollCall, etc.) to the DB, returns an error if ID already is key in DB*/
func CreateChannel(obj interface{}, database string) error {
	return writeChannel(obj, database, true)
}

/*writes a channel (LAO, meeting, rollCall, etc.) to the DB, only if ID already exists, otherwise return an error*/
func UpdateChannel(obj interface{}, database string) error {
	return writeChannel(obj, database, false)
}

/*returns channel data from a given ID */
func GetChannel(id []byte, database string) []byte {
	db, e := OpenDB(database)
	if e != nil {
		return nil
	}
	defer db.Close()
	var data []byte
	e = db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucketChannel))
		data = b.Get(id)
		return nil
	})
	return data
}
