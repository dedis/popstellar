package lao

import (
	"encoding/base64"
	"popstellar/message/messagedata"
	"strconv"
	"strings"

	"golang.org/x/xerrors"
)

// rollCallFlag for the RollCall ID
const rollCallFlag = "R"

// electionFlag for the Election ID
const electionFlag = "Election"

// questionFlag for the Question ID
const questionFlag = "Question"

// verifyMessageLaoState checks the lao#state message data is valid.
func (c *Channel) verifyMessageLaoState(laoState messagedata.LaoState) error {
	c.log.Info().Msgf("verifying lao#state message of lao %s", laoState.ID)

	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(laoState.ID)
	if err != nil {
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
		return xerrors.Errorf("lao creation is %d, should be smaller or equal to last modified %d",
			laoState.Creation, laoState.LastModified)
	}

	// verify organizer is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(laoState.Organizer)
	if err != nil {
		return xerrors.Errorf("lao organizer is %s, should be base64URL encoded", laoState.Organizer)
	}

	// verify if all witnesses are base64URL encoded
	for _, witness := range laoState.Witnesses {
		_, err := base64.URLEncoding.DecodeString(witness)
		if err != nil {
			return xerrors.Errorf("lao witness is %s, should be base64URL encoded", witness)
		}
	}

	// verify modification id is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(laoState.ModificationID)
	if err != nil {
		return xerrors.Errorf("lao modification id is %s, should be base64URL encoded",
			laoState.ModificationID)
	}

	// verify all witnesses in modification signatures are base64URL encoded
	for _, mod := range laoState.ModificationSignatures {
		_, err := base64.URLEncoding.DecodeString(mod.Witness)
		if err != nil {
			return xerrors.Errorf("lao modification signature witness is %s, "+
				"should be base64URL encoded", mod.Witness)
		}
	}

	return nil
}

// verifyMessageRollCallCreate checks the roll_call#create message data is valid.
func (c *Channel) verifyMessageRollCallCreate(rollCallCreate *messagedata.RollCallCreate) error {
	c.log.Info().Msgf("verifying roll_call#create message of roll call %s", rollCallCreate.ID)

	// verify id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(rollCallCreate.ID)
	if err != nil {
		return xerrors.Errorf("roll call id is %s, should be base64URL encoded", rollCallCreate.ID)
	}

	// verify roll call create message id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		strconv.Itoa(int(rollCallCreate.Creation)),
		rollCallCreate.Name,
	)
	if rollCallCreate.ID != expectedID {
		return xerrors.Errorf("roll call id is %s, should be %s", rollCallCreate.ID, expectedID)
	}

	// verify creation is positive
	if rollCallCreate.Creation < 0 {
		return xerrors.Errorf("roll call creation is %d, should be minimum 0",
			rollCallCreate.Creation)
	}

	// verify proposed start is positive
	if rollCallCreate.ProposedStart < 0 {
		return xerrors.Errorf("roll call proposed start is %d, should be minimum 0",
			rollCallCreate.ProposedStart)
	}

	// verify proposed end is positive
	if rollCallCreate.ProposedEnd < 0 {
		return xerrors.Errorf("roll call proposed end is %d, should be minimum 0",
			rollCallCreate.ProposedEnd)
	}

	// verify proposed start after creation
	if rollCallCreate.ProposedStart < rollCallCreate.Creation {
		return xerrors.Errorf("roll call proposed start is %d, "+
			"should be greater or equal to creation %d",
			rollCallCreate.ProposedStart, rollCallCreate.Creation)
	}

	// verify proposed end after creation
	if rollCallCreate.ProposedEnd < rollCallCreate.Creation {
		return xerrors.Errorf("roll call proposed end is %d, "+
			"should be greater or equal to creation %d",
			rollCallCreate.ProposedEnd, rollCallCreate.Creation)
	}

	// verify proposed end after proposed start
	if rollCallCreate.ProposedEnd < rollCallCreate.ProposedStart {
		return xerrors.Errorf("roll call proposed end is %d, "+
			"should be greater or equal to proposed start %d",
			rollCallCreate.ProposedEnd, rollCallCreate.ProposedStart)
	}

	return nil
}

// verifyMessageRollCallOpen checks the roll_call#open message data is valid.
func (c *Channel) verifyMessageRollCallOpen(rollCallOpen messagedata.RollCallOpen) error {
	c.log.Info().Msgf("verifying roll_call#open message of "+
		"roll call with update id %s", rollCallOpen.UpdateID)

	// verify update id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(rollCallOpen.UpdateID)
	if err != nil {
		return xerrors.Errorf("roll call update id is %s, should be base64URL encoded",
			rollCallOpen.UpdateID)
	}

	// verify roll call open message update id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		rollCallOpen.Opens,
		strconv.Itoa(int(rollCallOpen.OpenedAt)),
	)
	if rollCallOpen.UpdateID != expectedID {
		return xerrors.Errorf("roll call update id is %s, should be %s",
			rollCallOpen.UpdateID, expectedID)
	}

	// verify opens is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(rollCallOpen.Opens)
	if err != nil {
		return xerrors.Errorf("roll call opens is %s, should be base64URL encoded",
			rollCallOpen.Opens)
	}

	// verify opened at is positive
	if rollCallOpen.OpenedAt < 0 {
		return xerrors.Errorf("roll call opened at is %d, should be minimum 0",
			rollCallOpen.OpenedAt)
	}

	return nil
}

//TODO modif Noemien ??
// verifyMessageRollCallClose checks the roll_call#close message data is valid.
func (c *Channel) verifyMessageRollCallClose(rollCallClose *messagedata.RollCallClose) error {
	c.log.Info().Msgf("verifying roll_call#close message of roll call with update id %s",
		rollCallClose.UpdateID)

	// verify update id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(rollCallClose.UpdateID)
	if err != nil {
		return xerrors.Errorf("roll call update id is %s, should be base64URL encoded",
			rollCallClose.UpdateID)
	}

	// verify roll call close message update id
	expectedID := messagedata.Hash(
		rollCallFlag,
		strings.ReplaceAll(c.channelID, messagedata.RootPrefix, ""),
		rollCallClose.Closes,
		strconv.Itoa(int(rollCallClose.ClosedAt)),
	)
	if rollCallClose.UpdateID != expectedID {
		return xerrors.Errorf("roll call update id is %s, should be %s",
			rollCallClose.UpdateID, expectedID)
	}

	// verify closes is base64URL encoded
	_, err = base64.URLEncoding.DecodeString(rollCallClose.Closes)
	if err != nil {
		return xerrors.Errorf("roll call closes is %s, should be base64URL encoded",
			rollCallClose.Closes)
	}

	// verify closed at is positive
	if rollCallClose.ClosedAt < 0 {
		return xerrors.Errorf("roll call closed at is %d, should be minimum 0",
			rollCallClose.ClosedAt)
	}

	// verify all attendees are base64URL encoded
	for _, attendee := range rollCallClose.Attendees {
		_, err := base64.URLEncoding.DecodeString(attendee)
		if err != nil {
			return xerrors.Errorf("roll call attendee is %s, should be base64URL encoded",
				attendee)
		}
	}

	return nil
}

// verifyMessageElectionSetup checks the election#setup message data is valid.
func (c *Channel) verifyMessageElectionSetup(electionSetup messagedata.ElectionSetup) error {
	c.log.Info().Msgf("verifying election#setup message of election with id %s", electionSetup.ID)

	// verify lao id is base64URL encoded
	if _, err := base64.URLEncoding.DecodeString(electionSetup.Lao); err != nil {
		return xerrors.Errorf("lao id is %s, should be base64URL encoded", electionSetup.Lao)
	}

	// verify lao id is channel's lao id
	laoID := strings.ReplaceAll(c.channelID, messagedata.RootPrefix, "")
	if electionSetup.Lao != laoID {
		return xerrors.Errorf("lao id is %s, should be %s", electionSetup.Lao, laoID)
	}

	// verify election id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(electionSetup.ID)
	if err != nil {
		return xerrors.Errorf("election id is %s, should be base64URL encoded", electionSetup.ID)
	}

	// verify election setup message id
	expectedID := messagedata.Hash(
		electionFlag,
		laoID,
		strconv.Itoa(int(electionSetup.CreatedAt)),
		electionSetup.Name,
	)
	if electionSetup.ID != expectedID {
		return xerrors.Errorf("election setup id is %s, should be %s", electionSetup.ID, expectedID)
	}

	// verify election name non-empty
	if len(electionSetup.Name) == 0 {
		return xerrors.Errorf("election name should not be empty")
	}

	// verify ballot type is correct
	switch electionSetup.Version {
	case messagedata.OpenBallot:
	case messagedata.SecretBallot:
	default:
		return xerrors.Errorf("Version should not be %s", electionSetup.Version)
	}

	err = verifyElectionSetupTime(electionSetup.CreatedAt, electionSetup.StartTime, electionSetup.EndTime)
	if err != nil {
		return err
	}

	// verify questions is not empty
	if len(electionSetup.Questions) == 0 {
		return xerrors.Errorf("number of questions is 0 should be positive")
	}

	// verify the questions
	for _, question := range electionSetup.Questions {
		err := verifyElectionSetupQuestion(question, electionSetup.ID)
		if err != nil {
			return xerrors.Errorf("problem verifying question: %v", err)
		}
	}

	return nil
}

func verifyElectionSetupTime(createdAt, start, end int64) error {
	// verify created at is positive
	if createdAt < 0 {
		return xerrors.Errorf("election setup created at is %d, should be minimum 0",
			createdAt)
	}

	// verify start time is positive
	if start < 0 {
		return xerrors.Errorf("election setup start time is %d, should be minimum 0",
			start)
	}

	// verify end time is positive
	if end < 0 {
		return xerrors.Errorf("election setup end time is %d, should be minimum 0",
			end)
	}

	// verify start time after created at
	if start < createdAt {
		return xerrors.Errorf("election setup start time is %d, "+
			"should be greater or equal to created at %d",
			start, createdAt)
	}

	// verify end time after created at
	if end < createdAt {
		return xerrors.Errorf("election setup end time is %d, "+
			"should be greater or equal to created at %d",
			end, createdAt)
	}

	// verify end time after start time
	if end < start {
		return xerrors.Errorf("election end time is %d, "+
			"should be greater or equal to start time %d",
			end, start)
	}

	return nil
}

// verifyElectionSetupQuestion checks the question of an election setup message is valid.
func verifyElectionSetupQuestion(question messagedata.ElectionSetupQuestion,
	electionID string) error {

	// verify question id is base64URL encoded
	_, err := base64.URLEncoding.DecodeString(question.ID)
	if err != nil {
		return xerrors.Errorf("question id is %s, should be base64URL encoded", question.ID)
	}

	// verify question id
	expectedID := messagedata.Hash(
		questionFlag,
		electionID,
		question.Question,
	)
	if question.ID != expectedID {
		return xerrors.Errorf("question id is %s, should be %s", question.ID, expectedID)
	}

	// verify question not empty
	if len(question.Question) == 0 {
		return xerrors.Errorf("question should not be empty")
	}

	// verify voting method
	if question.VotingMethod != "Plurality" && question.VotingMethod != "Approval" {
		return xerrors.Errorf("voting method is %s, should be either Plurality or Approval",
			question.VotingMethod)
	}

	return nil
}
