// Package db defines and implements methods to store read and manage a hub's database
package db

//This file contains functions used to deal with channels in the database. Like create/update a channel and
//get infos about a channel.

import (
	"log"

	"github.com/boltdb/bolt"
	"golang.org/x/xerrors"
	//"student20_pop/lib"
)

const bucketChannel = "channels"

// TODO: these have been added to make the linter happy. Ideally we'd want to
// chain the error. To be taken up when the database is refactored
var (
	errorKeyExists       = xerrors.Errorf("key exists in the db")
	errorInvalidResource = xerrors.Errorf("invalid resource")
	errorInvalidData     = xerrors.Errorf("request data invalid")
)

// writeChannel is a function that stores a channel in the Database. The argument "secure" is to specify whether or not
// we want to update a channel, which means overwriting existing data.
func writeChannel(obj interface{}, database string, secure bool) error {
	db, err := OpenDB(database)
	if err != nil {
		return xerrors.Errorf("failed to open db: %v", err)
	}
	defer db.Close()

	err = db.Update(func(tx *bolt.Tx) error {
		_, err := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err != nil {
			return err
		}
		//generic adaptation
		//var objID []byte
		//switch obj.(type) {
		// type assert
		//case event.LAO:
		//objID = []byte(obj.(event.LAO).ID)
		//case event.Meeting:
		//objID = []byte(obj.(event.Meeting).ID)
		//case event.Poll:
		//objID = []byte(obj.(event.Poll).ID)
		//case event.RollCall:
		//objID = []byte(obj.(event.RollCall).ID)
		//default:
		//return errorInvalidDataType
		//}
		//checks if there is already an entry with that ID if secure is true
		//if secure {
		//key := b.Get(objID)
		//if key != nil {
		//return errorKeyExists
		//}
		//} else {
		//exists := b.Get(objID)
		//if exists == nil {
		//log.Printf("Could not find (key, val) pair to update in write channel with param secure=false")
		//return errorInvalidResource
		//}
		//}
		//dt, err := json.Marshal(obj)
		//if err != nil {
		//log.Printf("could not marshall object to store")
		//return errorInvalidData
		//}
		//err = b.Put(objID, dt)
		return err
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

// GetChannel returns a channel's infos from a given ID. Returns nil if the channel does not exists.
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
	if e != nil || len(result) == 0 {
		log.Printf("error occured while getting channel infos")
		return nil
	}
	return result
}
