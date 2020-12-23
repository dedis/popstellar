package actors

// This file contains the methods to manage subscribers for witness and organizer
//
// Those methods are not located in actor.go and organizer.go for 2 reasons. They are
// extremely similar for both actor types, and the files witness.go and organizer.go are
// already long enough.

import (
	"log"
	"student20_pop/lib"
	"student20_pop/message"
	"student20_pop/parser"
)

// handleSubscribe is the function that reads a received message with method "subscribe" and adds connectionId
// to the subscribers list of the channel. Returns an error if connection was already subscribed, or if the channel
// is "/root"
func (o *Organizer) handleSubscribe(query message.Query, connectionId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse paramsLight in handleSubscribe()")
		return lib.ErrRequestDataInvalid
	}

	if _, found := lib.Find(o.channels[params.Channel], connectionId); found {
		return lib.ErrResourceAlreadyExists
	}

	o.channels[params.Channel] = append(o.channels[params.Channel], connectionId)
	return nil
}

// handleUnsubscribe is the function that reads a received message with method "unsubscribe" and removes connectionId
// from the subscribers list of the channel. Returns an error if connection was not already subscribed, or if
// the channel is "/root"
func (o *Organizer) handleUnsubscribe(query message.Query, userId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse paramsLight in handleUnsubscribe()")
		return lib.ErrRequestDataInvalid
	}

	if params.Channel == "/root" {
		log.Printf("root channel is not subscribable !")
		return lib.ErrInvalidResource
	}

	subs := o.channels[params.Channel]
	if index, found := lib.Find(subs, userId); found {
		subs = append(subs[:index], subs[index+1:]...)
		o.channels[params.Channel] = subs
	}
	return nil
}

// GetSubscribers returns the subscribers list of a channel
func (o *Organizer) GetSubscribers(channel string) []int {
	return o.channels[channel]
}

// handleSubscribe is the function that reads a received message with method "subscribe" and adds connectionId
// to the subscribers list of the channel. Returns an error if connection was already subscribed, or if the channel
// is "/root"
func (w *Witness) handleSubscribe(query message.Query, userId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse paramsLight in handleSubscribe()")
		return lib.ErrRequestDataInvalid
	}

	if params.Channel == "/root" {
		log.Printf("root channel is not subscribable !")
		return lib.ErrInvalidResource
	}

	if _, found := lib.Find(w.channels[params.Channel], userId); found {
		return lib.ErrResourceAlreadyExists
	}

	w.channels[params.Channel] = append(w.channels[params.Channel], userId)
	return nil
}

// handleUnsubscribe is the function that reads a received message with method "unsubscribe" and removes connectionId
// from the subscribers list of the channel. Returns an error if connection was not already subscribed, or if
// the channel is "/root"
func (w *Witness) handleUnsubscribe(query message.Query, userId int) error {
	params, err := parser.ParseParams(query.Params)
	if err != nil {
		log.Printf("unable to analyse paramsLight in handleUnsubscribe()")
		return lib.ErrRequestDataInvalid
	}

	subs := w.channels[params.Channel]
	if index, found := lib.Find(subs, userId); found {
		subs = append(subs[:index], subs[index+1:]...)
		w.channels[params.Channel] = subs
	}

	return nil
}

// GetSubscribers returns the subscribers list of a channel
func (w *Witness) GetSubscribers(channel string) []int {
	return w.channels[channel]
}
