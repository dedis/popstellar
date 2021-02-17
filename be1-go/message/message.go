package message

import (
	"encoding/json"

	"golang.org/x/xerrors"
)

type internalMessage struct {
	MessageID         []byte                   `json:"message_id"`
	Data              []byte                   `json:"data"`
	Sender            PublicKey                `json:"sender"`
	Signature         Signature                `json:"signature"`
	WitnessSignatures []PublicKeySignaturePair `json:"witness_signatures"`
}

type Message struct {
	MessageID         []byte                   `json:"message_id"`
	Data              Data                     `json:"data"`
	Sender            PublicKey                `json:"sender"`
	Signature         Signature                `json:"signature"`
	WitnessSignatures []PublicKeySignaturePair `json:"witness_signatures"`
}

func (m Message) MarshalJSON() ([]byte, error) {
	dataBuf, err := json.Marshal(m.Data)
	if err != nil {
		return nil, xerrors.Errorf("error marshaling data: %v", err)
	}

	tmp := internalMessage{
		MessageID:         m.MessageID,
		Sender:            m.Sender,
		Signature:         m.Signature,
		WitnessSignatures: m.WitnessSignatures,
		Data:              dataBuf,
	}
	return json.Marshal(tmp)
}

func (m *Message) UnmarshalJSON(data []byte) error {
	tmp := &internalMessage{}
	err := json.Unmarshal(data, tmp)
	if err != nil {
		return xerrors.Errorf("error parsing message: %v", err)
	}

	gd := &GenericData{}
	err = json.Unmarshal(tmp.Data, gd)
	if err != nil {
		return xerrors.Errorf("error parsing data wrapper: %v", err)
	}

	m.MessageID = tmp.MessageID
	m.Sender = tmp.Sender
	m.Signature = tmp.Signature
	m.WitnessSignatures = tmp.WitnessSignatures

	action := gd.GetAction()

	switch gd.GetObject() {
	case DataObject(LaoObject):
		err := m.parseLAOData(LaoDataAction(action), tmp.Data)
		if err != nil {
			return xerrors.Errorf("error parsing lao data: %v", err)
		}
	case DataObject(MeetingObject):
		err := m.parseMeetingData(MeetingDataAction(action), tmp.Data)
		if err != nil {
			return xerrors.Errorf("error parsing meeting data: %v", err)
		}
	case DataObject(MessageObject):
		err := m.parseMessageData(MessageDataAction(action), tmp.Data)
		if err != nil {
			return xerrors.Errorf("error parsing message data: %v", err)
		}
	case DataObject(RollCallObject):
		err := m.parseRollCallData(RollCallAction(action), tmp.Data)
		if err != nil {
			return xerrors.Errorf("error parsing roll call data: %v", err)
		}
	default:
		return xerrors.Errorf("failed to parse data object of type: %s", gd.GetObject())
	}

	return nil
}

func (m *Message) parseRollCallData(action RollCallAction, data []byte) error {
	switch action {
	case CreateRollCallAction:
		create := &CreateRollCallData{}

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		m.Data = create
		return nil
	case RollCallAction(OpenRollCallAction), RollCallAction(ReopenRollCallAction):
		open := &OpenRollCallData{}

		err := json.Unmarshal(data, open)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		m.Data = open
		return nil
	case CloseRollCallAction:
		closeInst := &CloseRollCallData{}

		err := json.Unmarshal(data, closeInst)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		m.Data = closeInst
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)
	}
}

func (m *Message) parseMessageData(action MessageDataAction, data []byte) error {
	if action != WitnessAction {
		return xerrors.Errorf("invalid action type: %s", action)
	}

	witness := &WitnessMessageData{}
	err := json.Unmarshal(data, witness)
	if err != nil {
		return xerrors.Errorf("failed to parse witness action: %v", err)
	}

	m.Data = witness
	return nil
}

func (m *Message) parseMeetingData(action MeetingDataAction, data []byte) error {
	switch action {
	case CreateMeetingAction:
		create := &CreateMeetingData{}

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		m.Data = create
		return nil
	case StateMeetingAction:
		state := &StateMeetingData{}

		err := json.Unmarshal(data, state)
		if err != nil {
			return xerrors.Errorf("failed to parse state lao data: %v", err)
		}

		m.Data = state
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)

	}
}

func (m *Message) parseLAOData(action LaoDataAction, data []byte) error {
	switch action {
	case CreateLaoAction:
		create := &CreateLAOData{}
		create.Raw = data

		err := json.Unmarshal(data, create)
		if err != nil {
			return xerrors.Errorf("failed to parse create lao data: %v", err)
		}

		m.Data = create
		return nil
	case UpdateLaoAction:
		update := &UpdateLAOData{}
		update.Raw = data

		err := json.Unmarshal(data, update)
		if err != nil {
			return xerrors.Errorf("failed to parse update lao data: %v", err)
		}

		m.Data = update
		return nil
	case StateLaoAction:
		state := &StateLAOData{}
		state.Raw = data

		err := json.Unmarshal(data, state)
		if err != nil {
			return xerrors.Errorf("failed to parse state lao data: %v", err)
		}

		m.Data = state
		return nil
	default:
		return xerrors.Errorf("invalid action: %s", action)
	}
}
