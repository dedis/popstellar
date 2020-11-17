package channel

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/boltdb/bolt"
	"strings"
	"student20_pop/db"
	"student20_pop/define"
)

const bucketChannel = "general"
const bucketSubscribers = "sub"

/*
 * Function that subscribe a user to a channel. ONLY AT THE PUB/SUB LAYER
 * if user was already subscribed, does nothing
 * does not change LAO's member field
 */
func Subscribe(userId int, channelId []byte) error {

	db, err := db.OpenChannelDB()
	if err != nil {
		return err
	}
	defer db.Close()

	err = db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketSubscribers))
		if err1 != nil {
			return err1
		}
		var ints []int
		//gets the list of subscribers if exists, converts it to a list of int
		data := b.Get(channelId)
		if data != nil {
			err1 = json.Unmarshal(data, &ints)
			if err1 != nil {
				return err1
			}
		}

		//check if was already susbscribed
		if _, found := define.Find(ints, userId); found {
			fmt.Println("user was already subscribed")
			return nil
		}
		ints = append(ints, userId)
		//converts []int to string to []byte
		data = []byte(strings.Trim(strings.Join(strings.Split(fmt.Sprint(ints), " "), ","), ""))
		//push values back
		err1 = b.Put(channelId, data)
		return err1
	})

	return err
}

/*
 function that unsubscribes a user from a channel. ONLY AT THE PUB/SUB LAYER
 does nothing if that user was not already subscribed
 does not change LAO's member field
*/
func Unsubscribe(userId int, channelId []byte) error {

	db, err := db.OpenChannelDB()
	if err != nil {
		return err
	}
	defer db.Close()

	err = db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketSubscribers))
		if err1 != nil {
			return err1
		}
		var ints []int
		//gets the list of subscribers if exists, converts it to a list of int
		data := b.Get(channelId)
		if data != nil {
			err1 = json.Unmarshal(data, &ints)
			if err1 != nil {
				return err1
			}
		}

		//check if was already susbscribed
		i, found := define.Find(ints, userId)
		if !found {
			fmt.Println("this user was not subscribed to this channel")
			return nil
		}
		//remove elem from array
		ints[i] = ints[len(ints)-1]
		ints = ints[:len(ints)-1]

		//converts []int to string to []byte
		data = []byte(strings.Trim(strings.Join(strings.Split(fmt.Sprint(ints), " "), ","), ""))
		//push values back
		err1 = b.Put(channelId, data)
		return err1
	})

	return err
}

/*helper function to find a channel's subscribers */
func GetSubscribers(channel []byte) ([]int, error) {
	db, err := db.OpenChannelDB()
	if err != nil {
		return nil, err
	}
	defer db.Close()

	var data []int

	err = db.View(func(tx *bolt.Tx) error {
		bkt := tx.Bucket([]byte(bucketSubscribers))
		if bkt == nil {
			return nil
		}

		content := bkt.Get(channel)
		err = json.Unmarshal(content, &data)

		return err
	})

	return data, nil
}

/*returns the content of a channel. Nil if channel does not exist*/
func GetData(channel []byte) ([]byte, error) {
	db, err := db.OpenChannelDB()
	defer db.Close()
	if err != nil {
		return nil, err
	}

	var data []byte
	err = db.View(func(tx *bolt.Tx) error {
		b, err := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err != nil {
			return err
		}

		data = b.Get(channel)
		return nil
	})

	return data, err
}

/**
* Retrieve value from a given ID key, and adds a pubish
* returns error message
 */
func UpdateChannel(userId int, channelId []byte, action []byte) error {

	db, e := db.OpenChannelDB()
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucketChannel))
		if b == nil {
			return errors.New("bkt does not exist")
		}

		return nil
	})

	return err
}
