package types

import (
	"github.com/rs/zerolog"
	"go.dedis.ch/kyber/v3"
	"popstellar/hub/standard_hub/hub_state"
	"popstellar/internal/popserver/repo"
	"popstellar/network/socket"
	"popstellar/validation"
)

type HandlerParameters struct {
	Log                 zerolog.Logger
	Socket              socket.Socket
	SchemaValidator     validation.SchemaValidator
	DB                  repo.Repository
	Subs                Subscribers
	Peers               *hub_state.Peers
	Queries             *hub_state.Queries
	OwnerPubKey         kyber.Point
	ClientServerAddress string
	ServerServerAddress string
	ServerPubKey        kyber.Point
	ServerSecretKey     kyber.Scalar
}
