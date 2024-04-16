package types

import (
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"popstellar/internal/popserver/repo"
	"popstellar/network/socket"
	"popstellar/validation"
)

type HandlerParameters struct {
	Log                 zerolog.Logger
	Socket              socket.Socket
	SchemaValidator     validation.SchemaValidator
	DB                  repo.Repository
	OwnerPubKey         kyber.Point
	ClientServerAddress string
	ServerServerAddress string
	ServerPubKey        kyber.Point
	ServerSecretKey     kyber.Scalar
}
