package types

type Question struct {
	// ID represents the ID of the Question.
	ID []byte

	// ballotOptions represents different ballot options.
	BallotOptions []string

	// validVotes represents the list of all valid votes. The key represents
	// the public key of the person casting the vote.
	ValidVotes map[string]ValidVote

	// method represents the voting method of the election. Either "Plurality"
	// or "Approval".
	Method string
}

type ValidVote struct {
	// msgID represents the ID of the message containing the cast vote
	MsgID string

	// ID represents the ID of the valid cast vote
	ID string

	// voteTime represents the time of the creation of the vote
	VoteTime int64

	// index represents the index of the ballot options
	Index interface{}
}
