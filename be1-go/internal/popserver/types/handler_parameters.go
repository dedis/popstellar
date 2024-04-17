package types

import (
	"popstellar/internal/popserver/repo"
	"popstellar/network/socket"
)

type HandlerParameters struct {
	Socket socket.Socket
	DB     repo.Repository
}
