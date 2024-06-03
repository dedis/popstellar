package sqlite

// GetChannelType returns the type of the channelPath.
func (s *SQLite) GetChannelType(channelPath string) (string, error) {
	dbLock.Lock()
	defer dbLock.Unlock()

	var channelType string
	err := s.database.QueryRow(selectChannelType, channelPath).Scan(&channelType)
	return channelType, err
}
