package lao

import (
	"encoding/base64"
	"fmt"
	"golang.org/x/xerrors"
	"popstellar/message/messagedata"
	"strings"
)

// rollCallFlag for the RollCall ID
const rollCallFlag = "R"

// verifyMessageLaoState checks the lao#state message data is valid.
func (c *Channel) verifyMessageLaoState(laoState messagedata.LaoState) error {
	c.log.Info().Msgf("verifying lao#state message of lao %s", laoState.ID)

	// verify id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(laoState.ID); err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", laoState.ID)
	}

	// verify if a lao message id is the same as the lao id
	expectedID := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	if expectedID != laoState.ID {
		return xerrors.Errorf("lao id is %s, should be %s", laoState.ID, expectedID)
	}

	// verify name is non-empty
	if len(laoState.Name) == 0 {
		return xerrors.Errorf("lao name is %s, should not be empty", laoState.Name)
	}

	// verify creation is positive
	if laoState.Creation < 0 {
		return xerrors.Errorf("lao creation is %d, should be minimum 0", laoState.Creation)
	}

	// verify last modified is positive
	if laoState.LastModified < 0 {
		return xerrors.Errorf("lao last modified is %d, should be minimum 0", laoState.LastModified)
	}

	// verify creation before or equal to last modified
	if laoState.LastModified < laoState.Creation {
		return xerrors.Errorf("lao creation is %d, should be smaller or equal to last modified %d", laoState.Creation, laoState.LastModified)
	}

	// verify organizer is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(laoState.Organizer); err != nil {
		return xerrors.Errorf("lao organizer is %s, should be base64URL encoded", laoState.Organizer)
	}

	// verify if all witnesses are base64URL encoded
	for _, witness := range laoState.Witnesses {
		if _, err := base64.URLEncoding.DecodeString(witness); err != nil {
			return xerrors.Errorf("lao witness is %s, should be base64URL encoded", witness)
		}
	}

	// verify modification id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(laoState.ModificationID); err != nil {
		return xerrors.Errorf("lao modification id is %s, should be base64URL encoded", laoState.ModificationID)
	}

	// verify all witnesses in modification signatures are base64URL encoded
	for _, mod := range laoState.ModificationSignatures {
		if _, err := base64.URLEncoding.DecodeString(mod.Witness); err != nil {
			return xerrors.Errorf("lao modification signature witness is %s, should be base64URL encoded", mod.Witness)
		}
	}

	return nil
}

// verifyMessageRollCallCreate checks the roll_call#create message data is valid.
func (c *Channel) verifyMessageRollCallCreate(rollCallCreate messagedata.RollCallCreate) error {
	c.log.Info().Msgf("verifying roll_call#create message of roll call %s", rollCallCreate.ID)

	// verify id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(rollCallCreate.ID); err != nil {
		return xerrors.Errorf("roll call id is %s, should be base64URL encoded", rollCallCreate.ID)
	}

	// verify roll call create message id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		fmt.Sprintf("%d", rollCallCreate.Creation),
		rollCallCreate.Name,
	)
	if rollCallCreate.ID != expectedID {
		return xerrors.Errorf("roll call id is %s, should be %s", rollCallCreate.ID, expectedID)
	}

	// verify creation is positive
	if rollCallCreate.Creation < 0 {
		return xerrors.Errorf("roll call creation is %d, should be minimum 0", rollCallCreate.Creation)
	}

	// verify proposed start is positive
	if rollCallCreate.ProposedStart < 0 {
		return xerrors.Errorf("roll call proposed start is %d, should be minimum 0", rollCallCreate.ProposedStart)
	}

	// verify proposed end is positive
	if rollCallCreate.ProposedEnd < 0 {
		return xerrors.Errorf("roll call proposed end is %d, should be minimum 0", rollCallCreate.ProposedEnd)
	}

	// verify proposed start after creation
	if rollCallCreate.ProposedStart < rollCallCreate.Creation {
		return xerrors.Errorf("roll call proposed start is %d, should be greater or equal to creation %d",
			rollCallCreate.ProposedStart, rollCallCreate.Creation)
	}

	// verify proposed end after creation
	if rollCallCreate.ProposedEnd <  rollCallCreate.Creation {
		return xerrors.Errorf("roll call proposed end is %d, should be greater or equal to creation %d",
			rollCallCreate.ProposedEnd, rollCallCreate.Creation)
	}

	// verify proposed end after proposed start
	if rollCallCreate.ProposedEnd <  rollCallCreate.ProposedStart {
		return xerrors.Errorf("roll call proposed end is %d, should be greater or equal to proposed start %d",
			rollCallCreate.ProposedEnd, rollCallCreate.ProposedStart)
	}

	return nil
}

// verifyMessageRollCallOpen checks the roll_call#open message data is valid.
func (c *Channel) verifyMessageRollCallOpen(rollCallOpen messagedata.RollCallOpen) error {
	c.log.Info().Msgf("verifying roll_call#open message of roll call with update id %s", rollCallOpen.UpdateID)

	// verify update id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(rollCallOpen.UpdateID); err != nil {
		return xerrors.Errorf("roll call update id is %s, should be base64URL encoded", rollCallOpen.UpdateID)
	}

	// verify roll call open message update id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		rollCallOpen.Opens,
		fmt.Sprintf("%d", rollCallOpen.OpenedAt),
	)
	if rollCallOpen.UpdateID != expectedID {
		return xerrors.Errorf("roll call update id is %s, should be %s", rollCallOpen.UpdateID, expectedID)
	}

	// verify opens is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(rollCallOpen.Opens); err != nil {
		return xerrors.Errorf("roll call opens is %s, should be base64URL encoded", rollCallOpen.Opens)
	}

	// verify opened at is positive
	if rollCallOpen.OpenedAt < 0 {
		return xerrors.Errorf("roll call opened at is %d, should be minimum 0", rollCallOpen.OpenedAt)
	}

	return nil
}

// verifyMessageRollCallClose checks the roll_call#close message data is valid.
func (c *Channel) verifyMessageRollCallClose(rollCallClose messagedata.RollCallClose) error {
	c.log.Info().Msgf("verifying roll_call#close message of roll call with update id %s", rollCallClose.UpdateID)

	// verify update id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(rollCallClose.UpdateID); err != nil {
		return xerrors.Errorf("roll call update id is %s, should be base64URL encoded", rollCallClose.UpdateID)
	}

	// verify roll call close message update id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		rollCallClose.Closes,
		fmt.Sprintf("%d", rollCallClose.ClosedAt),
	)
	if rollCallClose.UpdateID != expectedID {
		return xerrors.Errorf("roll call update id is %s, should be %s", rollCallClose.UpdateID, expectedID)
	}

	// verify closes is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(rollCallClose.Closes); err != nil {
		return xerrors.Errorf("roll call closes is %s, should be base64URL encoded", rollCallClose.Closes)
	}

	// verify closed at is positive
	if rollCallClose.ClosedAt < 0 {
		return xerrors.Errorf("roll call closed at is %d, should be minimum 0", rollCallClose.ClosedAt)
	}

	// verify all attendees are base64URL encoded
	for _, attendee := range rollCallClose.Attendees {
		if _, err := base64.URLEncoding.DecodeString(attendee); err != nil {
			return xerrors.Errorf("roll call attendee is %s, should be base64URL encoded", attendee)
		}
	}

	return nil
}