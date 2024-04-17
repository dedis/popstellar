package types

import (
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/popserver/repo"
	"popstellar/network/socket"
)

type HandlerParameters struct {
	Socket              socket.Socket
	DB                  repo.Repository
	OwnerPubKey         kyber.Point
	ClientServerAddress string
	ServerServerAddress string
	ServerPubKey        kyber.Point
	ServerSecretKey     kyber.Scalar
}
