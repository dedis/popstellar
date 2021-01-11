// db is a package defining and implementing methods to store read and manage an Actor's database
package db

//This file contains functions used to deal with channels in the database. Like create/update a channel and
//get infos about a channel.

import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"log"
	"student20_pop/event"
	"student20_pop/lib"
)

const bucketChannel = "channels"

// writeChannel is a function that stores a channel in the Database. The argument "secure" is to specify whether or not
// we want to update a channel, which means overwriting existing data.
func writeChannel(obj interface{}, database string, secure bool) error {
	db, e := OpenDB(database)
	if e != nil {
		return lib.ErrDBFault
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
		// type assert
		case event.LAO:
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
				log.Printf("Could not find (key, val) pair to update in write channel with param secure=false")
				return lib.ErrInvalidResource
			}
		}
		dt, err2 := json.Marshal(obj)
		if err2 != nil {
			log.Printf("could not marshall object to store")
			return lib.ErrRequestDataInvalid
		}
		err3 := b.Put(objID, dt)
		return err3
	})

	if err != nil {
		log.Printf("an error occured in the database transaction.")
	}
	return err
}

//CreateChannel writes a channel (LAO, meeting, rollCall, etc.) to the DB. It will return an error if
// the obj ID is already a key in DB the database. It just calls writeChannel with secure = true
func CreateChannel(obj interface{}, database string) error {
	return writeChannel(obj, database, true)
}

// UpdateChannel overwrites a channel (LAO, meeting, rollCall, etc.) to the DB, only if the obj 's ID ID already exists.
// Otherwise it will return an error. It just calls writeChannel with secure = false
func UpdateChannel(obj interface{}, database string) error {
	return writeChannel(obj, database, false)
}

// GetChannel returns a channel's infos from a given ID. Returns ni if the channel does not exists.
func GetChannel(id []byte, database string) []byte {
	db, e := OpenDB(database)
	if e != nil {
		return nil
	}
	defer db.Close()
	var result []byte
	e = db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucketChannel))
		data := b.Get(id)
		result = make([]byte, len(data))
		copy(result, data)
		return nil
	})
	if e != nil {
		log.Printf("error occured while getting channel infos")
		return nil
	}
	return result
}
