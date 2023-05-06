package main

import (
	"io"
	"popstellar/channel/lao"
	"popstellar/crypto"
	"popstellar/hub/standard_hub"
	"popstellar/network"
	"popstellar/network/socket"
	"sync"
	"testing"
	"time"

	"github.com/rs/zerolog"
	"github.com/stretchr/testify/require"
)

func TestConnectToSocket(t *testing.T) {
	log := zerolog.New(io.Discard)

	oh, err := standard_hub.NewHub(crypto.Suite.Point(), "", log, lao.NewChannel)
	require.NoError(t, err)
	oh.Start()

	remoteSrv := network.NewServer(oh, "localhost", 9001, socket.ServerSocketType, log)
	remoteSrv.Start()
	<-remoteSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := standard_hub.NewHub(crypto.Suite.Point(), "", log, lao.NewChannel)
	require.NoError(t, err)
	wDone := make(chan struct{})
	wh.Start()

	wg := &sync.WaitGroup{}
	err = connectToSocket("localhost:9001", wh, wg, wDone)
	require.NoError(t, err)

	err = remoteSrv.Shutdown()
	require.NoError(t, err)
	<-remoteSrv.Stopped

	oh.Stop()
	wh.Stop()
	close(wDone)
	wg.Wait()
}

// TestLoadConfigFile tests the loadConfig function is able to load the config file
// and parse it into a ServerConfig struct
func TestLoadConfigFile(t *testing.T) {
	ServerConfig, err := startWithConfigFile("test_config_files/valid_config.json")
	require.NoError(t, err)
	require.Equal(t, "", ServerConfig.PublicKey)
	require.Equal(t, "localhost", ServerConfig.PublicAddress)
	require.Equal(t, "localhost", ServerConfig.PrivateAddress)
	require.Equal(t, 9000, ServerConfig.ClientPort)
	require.Equal(t, 9001, ServerConfig.ServerPort)
	require.Equal(t, []string{}, ServerConfig.OtherServers)
}

// TestLoadInvalidConfigFilename tests that loading a config file with an invalid filename fails
func TestLoadInvalidConfigFilename(t *testing.T) {
	_, err := startWithConfigFile("invalid_filename.json")
	require.Error(t, err)
}

// TestLoadConfigFileWithInvalidPorts tests that loading a config file with the same client and server ports fails
func TestLoadConfigFileWithInvalidPorts(t *testing.T) {
	_, err := startWithConfigFile("test_config_files/invalid_config_equal_client_server_ports.json")
	require.Error(t, err)
}
