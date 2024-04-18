package config

import (
	"go.dedis.ch/kyber/v3"
	"sync"
)

var once sync.Once
var instance *config

type config struct {
	ownerPubKey         kyber.Point
	serverPubKey        kyber.Point
	serverSecretKey     kyber.Scalar
	clientServerAddress string
	serverServerAddress string
}

func InitConfig(ownerPubKey, serverPubKey kyber.Point, serverSecretKey kyber.Scalar,
	clientServerAddress, serverServerAddress string) {
	once.Do(func() {
		instance = &config{
			ownerPubKey:         ownerPubKey,
			serverPubKey:        serverPubKey,
			serverSecretKey:     serverSecretKey,
			clientServerAddress: clientServerAddress,
			serverServerAddress: serverServerAddress,
		}
	})
}

func GetOwnerPublicKeyInstance() (kyber.Point, bool) {
	if instance == nil {
		return nil, false
	}

	return instance.ownerPubKey, true
}

func GetServerPublicKeyInstance() (kyber.Point, bool) {
	if instance == nil || instance.serverPubKey == nil {
		return nil, false
	}

	return instance.serverPubKey, true
}

func GetServerSecretKeyInstance() (kyber.Scalar, bool) {
	if instance == nil || instance.serverPubKey == nil {
		return nil, false
	}

	return instance.serverSecretKey, true
}

func GetServerInfo() (kyber.Point, string, string, bool) {
	if instance == nil {
		return nil, "", "", false
	}

	return instance.serverPubKey, instance.clientServerAddress, instance.serverServerAddress, true
}
