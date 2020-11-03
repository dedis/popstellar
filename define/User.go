package src

import (
	"golang.org/x/tools/go/ssa/interp/testdata/src/errors"
	"log"
)

func publish(userId []byte,  channelId []byte,  content []byte) error{
	//action := //parse pour choper Json
	err := userdb.UpdateUserDB(userId,channelId,action)
	if err != nil {
		return err
	}
	//channel := //parse pour choper Json
	switch action {
	case []byte("createLAO") :

	}
	//interpretation associated to the action
	err = channeldb.UpdateChannelDB(channelId, intepretation)
	if err != nil {
		return err
	}
	return nil
}

func createLao(userId []byte,  channelId []byte,  content []byte) error{
	//parse json pour retirer le , ... de la lao

	createchannel

	return nil
}
//fetch([]byte userId, [] channelID)

subscribe([]byte userId, [] channelID)