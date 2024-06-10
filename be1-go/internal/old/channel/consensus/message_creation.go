package consensus

import (
	"encoding/json"
	"popstellar/internal/message/messagedata/mconsensus"

	"golang.org/x/xerrors"
)

// createPrepareMessage creates the data for a new prepare message
func (c *Channel) createPrepareMessage(consensusInstance *ConsensusInstance,
	messageID string) ([]byte, error) {

	newData := mconsensus.ConsensusPrepare{
		Object:     "consensus",
		Action:     "prepare",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),

		Value: mconsensus.ValuePrepare{
			ProposedTry: consensusInstance.proposedTry,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#prepare message: %v", err)
	}

	return byteMsg, nil
}

// createPromiseMessage creates the data for a new promise message
func (c *Channel) createPromiseMessage(consensusInstance *ConsensusInstance,
	messageID string) ([]byte, error) {

	newData := mconsensus.ConsensusPromise{
		Object:     "consensus",
		Action:     "promise",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),

		Value: mconsensus.ValuePromise{
			AcceptedTry:   consensusInstance.acceptedTry,
			AcceptedValue: consensusInstance.acceptedValue,
			PromisedTry:   consensusInstance.promisedTry,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#promise message: %v", err)
	}

	return byteMsg, nil
}

// createProposeMessage creates the data for a new propose message
func (c *Channel) createProposeMessage(consensusInstance *ConsensusInstance, messageID string,
	highestAccepted int64, highestValue bool) ([]byte, error) {

	newData := mconsensus.ConsensusPropose{
		Object:     "consensus",
		Action:     "propose",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),

		Value: mconsensus.ValuePropose{
			ProposedValue: highestValue,
		},

		AcceptorSignatures: make([]string, 0),
	}

	for acceptor := range consensusInstance.promises {
		newData.AcceptorSignatures = append(newData.AcceptorSignatures, acceptor)
	}

	if highestAccepted == -1 {
		newData.Value.ProposedTry = consensusInstance.proposedTry
	} else {
		newData.Value.ProposedTry = highestAccepted
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#propose message: %v", err)
	}

	return byteMsg, nil
}

// createAcceptMessage creates the data for a new accept message
func (c *Channel) createAcceptMessage(consensusInstance *ConsensusInstance,
	messageID string) ([]byte, error) {

	newData := mconsensus.ConsensusAccept{
		Object:     "consensus",
		Action:     "accept",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),

		Value: mconsensus.ValueAccept{
			AcceptedTry:   consensusInstance.acceptedTry,
			AcceptedValue: consensusInstance.acceptedValue,
		},
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#accept message: %v", err)
	}

	return byteMsg, nil
}

// createLearnMessage creates the data for a learn message
func (c *Channel) createLearnMessage(consensusInstance *ConsensusInstance,
	messageID string) ([]byte, error) {

	newData := mconsensus.ConsensusLearn{
		Object:     "consensus",
		Action:     "learn",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),

		Value: mconsensus.ValueLearn{
			Decision: consensusInstance.decision,
		},

		AcceptorSignatures: make([]string, 0),
	}

	for acceptor := range consensusInstance.accepts {
		newData.AcceptorSignatures = append(newData.AcceptorSignatures, acceptor)
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#learn message: %v", err)
	}

	return byteMsg, nil
}

// createFailureMessage creates the data for a failure message
func (c *Channel) createFailureMessage(consensusInstance *ConsensusInstance,
	messageID string) ([]byte, error) {

	electInstance, ok := consensusInstance.electInstances[messageID]
	if !ok {
		return nil, xerrors.Errorf("message Id doesn't correspond to any " +
			"previously received message")
	}

	if electInstance.failed {
		return nil, xerrors.Errorf("consensus already failed")
	}
	electInstance.failed = true

	c.log.Warn().Msgf("failure of the consensus")

	newData := mconsensus.ConsensusFailure{
		Object:     "consensus",
		Action:     "failure",
		InstanceID: consensusInstance.id,
		MessageID:  messageID,

		CreatedAt: c.clock.Now().Unix(),
	}

	byteMsg, err := json.Marshal(newData)
	if err != nil {
		return nil, xerrors.Errorf("failed to marshal new consensus#failure message: %v", err)
	}

	return byteMsg, nil
}
