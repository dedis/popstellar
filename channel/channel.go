package channel

import (
	"encoding/json"
	"fmt"
	"github.com/boltdb/bolt"
	"strings"
	"../db"
)

/*
 * Function that subscribe a user to a channel.
 * if user was already subscribed, does nothing
 */
func Subscribe(userId int, channelId []byte) error {

	db, err := db.OpenChannelDB()
	if err != nil {
		return err
	}
	defer db.Close()

	err = db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists("sub")
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
		if _, found := Find(ints, userId); found {
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

func Unsubscribe(userId []byte, channelId []byte) error {

	db, err := db.OpenChannelDB()
	if err != nil {
		return err
	}
	defer db.Close()

	err = db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists("sub")
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
		if _, found := Find(ints, userId); found {
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

func Find(slice []int, val int) (int, bool) {
	for i, item := range slice {
		if item == val {
			return i, true
		}
	}
	return -1, false
}

func GetSubscribers(channel []byte) error {
	return nil
}
