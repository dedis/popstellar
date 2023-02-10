package network

import (
	"context"
	"fmt"
	"net/http"
	"popstellar"
	"popstellar/hub"
	"popstellar/network/socket"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"golang.org/x/xerrors"
)

type key int

const requestIDKey key = 0

const infoPath = "/infos"

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

// Server represents a Websocket Server for an organizer or a witness and it may
// listen to requests from organizer, witness or an attendee.
type Server struct {
	hub hub.Hub
	st  socket.SocketType
	srv *http.Server

	Started chan struct{}
	Stopped chan struct{}

	closing  *sync.Mutex
	isClosed bool
	wg       *sync.WaitGroup
	done     chan struct{}

	log zerolog.Logger
}

// NewServer creates a new Server which is used to handle requests for
// /<hubType>/<socketType> endpoint. Please use the Start() method to start
// listening for connections.
func NewServer(hub hub.Hub, addr string, port int, st socket.SocketType, log zerolog.Logger) *Server {
	log = log.With().Str("role", "server").Logger()

	server := &Server{
		hub:     hub,
		st:      st,
		Started: make(chan struct{}, 1),
		Stopped: make(chan struct{}, 1),
		wg:      &sync.WaitGroup{},
		done:    make(chan struct{}),
		closing: &sync.Mutex{},
		log:     log,
	}

	path := fmt.Sprintf("/server/%s", st)
	mux := http.NewServeMux()
	mux.HandleFunc(path, server.ServeHTTP)

	mux.HandleFunc(infoPath, server.infoHandler)

	nextRequestID := func() string {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}

	httpServer := &http.Server{
		Addr:    fmt.Sprintf("%s:%d", addr, port),
		Handler: tracing(nextRequestID)(logging(log)(mux)),
	}

	log.Info().Msgf("setting handler at %s for port %d", path, port)

	server.srv = httpServer

	return server
}

// Start spawns a new go-routine which invokes ListenAndServe on the http.Server
func (s *Server) Start() {
	go func() {
		s.log.Info().Msgf("starting to listen at: %s", s.srv.Addr)
		s.Started <- struct{}{}
		err := s.srv.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			s.log.Fatal().Err(err).Msg("failed to start the server")
		}

		s.Stopped <- struct{}{}
		s.log.Info().Msg("stopped the server...")
	}()
}

// ServeHTTP handles a websocket connection based on the socket type.
func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	s.closing.Lock()
	defer s.closing.Unlock()

	if s.isClosed {
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		s.log.Err(err).Msg("failed to upgrade connection")
		return
	}

	switch s.st {
	case socket.ClientSocketType:
		client := socket.NewClientSocket(s.hub.Receiver(), s.hub.OnSocketClose(),
			conn, s.wg, s.done, s.log)
		s.wg.Add(2)

		go client.ReadPump()
		go client.WritePump()
	case socket.ServerSocketType:
		server := socket.NewServerSocket(s.hub.Receiver(), s.hub.OnSocketClose(),
			conn, s.wg, s.done, s.log)
		s.wg.Add(2)

		go server.ReadPump()
		go server.WritePump()

		err = s.hub.NotifyNewServer(server)
		if err != nil {
			s.log.Err(err).Msg("error while trying to catchup to server")

			http.Error(w, "failed to add socket: "+err.Error(), http.StatusInternalServerError)
			return
		}
	}
}

func (s *Server) infoHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "not supported", http.StatusMethodNotAllowed)
		return
	}

	w.Header().Set("Content-Type", "application/json")

	fmtStr := `{
	"version": "%s",
	"commit": "%s",
	"buildTime": "%s",
	"hubType": "%s",
	"socketType": "%s"
}`

	resp := fmt.Sprintf(fmtStr, popstellar.Version, popstellar.ShortSHA,
		popstellar.BuildTime, s.st)

	w.Write([]byte(resp))
}

// Shutdown shuts the HTTP server, signals the read and write pumps to
// close and waits for them to finish.
func (s *Server) Shutdown() error {
	s.closing.Lock()
	defer s.closing.Unlock()

	s.isClosed = true

	err := s.srv.Shutdown(context.Background())
	if err != nil {
		return xerrors.Errorf("failed to shutdown server: %v", err)
	}

	close(s.done)

	s.wg.Wait()

	return nil
}

// logging is a utility function that logs the http server events
func logging(logger zerolog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			defer func() {
				requestID, ok := r.Context().Value(requestIDKey).(string)
				if !ok {
					requestID = "unknown"
				}
				logger.Info().Str("requestID", requestID).
					Str("method", r.Method).
					Str("url", r.URL.Path).
					Str("remoteAddr", r.RemoteAddr).
					Str("agent", r.UserAgent()).Msg("")
			}()
			next.ServeHTTP(w, r)
		})
	}
}

// tracing is a utility function that adds header tracing
func tracing(nextRequestID func() string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			requestID := r.Header.Get("X-Request-Id")
			if requestID == "" {
				requestID = nextRequestID()
			}
			ctx := context.WithValue(r.Context(), requestIDKey, requestID)
			w.Header().Set("X-Request-Id", requestID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
