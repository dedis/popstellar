package main

import (
	"encoding/json"
	"github.com/fsnotify/fsnotify"
	"golang.org/x/xerrors"
	"io"
	"os"
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

const (
	// validConfigPath is the path to a valid config file
	validConfigPath = "test_config_files/valid_config.json"

	// invalidConfigEqualClientServerPortsPath is the path to a config file with the same client and server ports
	invalidConfigEqualClientServerPortsPath = "test_config_files/invalid_config_equal_client_server_ports.json"

	// invalidConfigEqualClientAuthPortsPath is the path to a config file with the same client and auth ports
	invalidConfigEqualClientAuthPortsPath = "test_config_files/invalid_config_equal_ports.json"

	// validConfigWatcherPath is the path to a valid config file that used for testing the config watcher
	validConfigWatcherPath = "test_config_files/valid_config_watcher.json"
)

func TestConnectToSocket(t *testing.T) {
	log := zerolog.New(io.Discard)

	oh, err := standard_hub.NewHub(crypto.Suite.Point(), "", "", log, lao.NewChannel)
	require.NoError(t, err)
	oh.Start()

	remoteSrv := network.NewServer(oh, "localhost", 9001, socket.ServerSocketType, log)
	remoteSrv.Start()
	<-remoteSrv.Started

	time.Sleep(1 * time.Second)

	wh, err := standard_hub.NewHub(crypto.Suite.Point(), "", "", log, lao.NewChannel)
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
	ServerConfig, err := startWithConfigFile(validConfigPath)
	require.NoError(t, err)
	require.Equal(t, "", ServerConfig.PublicKey)
	require.Equal(t, "localhost", ServerConfig.PublicAddress)
	require.Equal(t, "localhost", ServerConfig.PrivateAddress)
	require.Equal(t, "localhost", ServerConfig.AuthAddress)
	require.Equal(t, 9000, ServerConfig.ClientPort)
	require.Equal(t, 9001, ServerConfig.ServerPort)
	require.Equal(t, 9100, ServerConfig.AuthPort)
	require.Equal(t, []string{}, ServerConfig.OtherServers)
}

// TestLoadInvalidConfigFilename tests that loading a config file with an invalid filename fails
func TestLoadInvalidConfigFilename(t *testing.T) {
	_, err := startWithConfigFile("invalid_filename.json")
	require.Error(t, err)
}

// TestLoadConfigFileWithInvalidPorts tests that loading a config file with the same client and server ports fails
func TestLoadConfigFileWithInvalidPorts(t *testing.T) {
	_, err := startWithConfigFile(invalidConfigEqualClientServerPortsPath)
	require.Error(t, err)
}

// TestLoadConfigFileWithInvalidPorts tests that loading a config file with the same client and PoPCHA ports fails
func TestLoadConfigFileWithInvalidAuthPort(t *testing.T) {
	_, err := startWithConfigFile(invalidConfigEqualClientAuthPortsPath)
	require.Error(t, err)
}

// TestWatchConfigFile tests that a config file is watched correctly and the updated servers are received
func TestWatchConfigFile(t *testing.T) {
	// Load the config from the file
	serverConfig, err := loadConfig(validConfigWatcherPath)
	require.NoError(t, err)

	// Restore the initial config to the file at the end of the test
	defer func(filePath string, config ServerConfig) {
		require.NoError(t, writeConfigToPath(filePath, config))
	}(validConfigWatcherPath, serverConfig)

	// Create a watcher for the config file
	watcher, err := fsnotify.NewWatcher()
	require.NoError(t, err)
	err = watcher.Add(validConfigWatcherPath)
	require.NoError(t, err)

	// Create a channel to receive the updated servers
	updatedServersChan := make(chan []string)

	// Start watching the config file
	go watchConfigFile(watcher, validConfigWatcherPath, &serverConfig.OtherServers, updatedServersChan)
	defer func(watcher *fsnotify.Watcher) {
		require.NoError(t, watcher.Close())
	}(watcher)

	// Update the config file with new servers
	updatedConfig := serverConfig
	updatedConfig.OtherServers = []string{"server1", "server2"}
	err = writeConfigToPath(validConfigWatcherPath, updatedConfig)
	if err != nil {
		t.Fatalf("Failed to update config file: %v", err)
	}

	// Wait for the updated servers to be received
	select {
	case newServers := <-updatedServersChan:
		// check that the new servers were received correctly
		if len(newServers) != len(updatedConfig.OtherServers) || newServers[0] != updatedConfig.OtherServers[0] || newServers[1] != updatedConfig.OtherServers[1] {
			t.Errorf("Received incorrect servers: %v", newServers)
		}
	case <-time.After(200 * time.Millisecond):
		t.Error("Timed out waiting for updated servers")
	}
}

// TestIncreaseDelay tests that the delay is increased by the correct amount
// following an exponential backoff and that the delay is capped at the maximum
func TestIncreaseDelay(t *testing.T) {
	// Test that the delay is increased by the correct amount
	delay := connectionRetryInitialDelay
	increaseDelay(&delay)
	require.Equal(t, connectionRetryInitialDelay*connectionRetryRate, delay)
	// Test that the delay is capped at the maximum
	delay = connectionRetryMaxDelay
	increaseDelay(&delay)
	require.Equal(t, connectionRetryMaxDelay, delay)
}

// TestNewServersAdded tests that the function correctly returns true if the
// new servers are different from the old servers
func TestNewServersAdded(t *testing.T) {
	oldServers := []string{"server1", "server2"}
	newServers := []string{"server1", "server2", "server4"}
	require.True(t, newServersAdded(oldServers, &newServers))
}

/**
 * -----------------------
 * Util functions
 * -----------------------
 */

// writeConfigToPath writes the given config in a given config file path
func writeConfigToPath(filePath string, config ServerConfig) error {
	buf, err := json.Marshal(config)
	if err != nil {
		return xerrors.Errorf("Failed to marshal config: %v", err)
	}
	// Overwrite the file with the updated config
	err = os.Truncate(filePath, 0)
	if err != nil {
		return xerrors.Errorf("Failed to truncate config file: %v", err)
	}
	if err := os.WriteFile(filePath, buf, 0644); err != nil {
		return xerrors.Errorf("Failed to write updated config to temp file: %v", err)
	}
	return nil
}
