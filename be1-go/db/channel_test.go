package db

//import (
//"bytes"
//"encoding/json"
//"io/ioutil"
//"log"
//"os"
//"testing"
//"time"
//)

// the package DB is very low-level. So it does no check on data validity (ID, hashes, etc...)
// theses tests only test with the following events : Lao, Meeting, RollCall, Poll

// TestWriteChannel tests if CreateChannel works, if CreateChannel returns an error if the channel already
// exists, if UpdateChannel works, and if UpdateChannel returns an error if the channel does not exists
//func TestWriteChannel(t *testing.T) {

//// error not logged : error occurs if file does not exists
//_ = os.Remove("test.db")

//// turn off logging for the tests
//log.SetFlags(0)
//log.SetOutput(ioutil.Discard)

//lao, meeting, rollCall, poll := getEvents()

//// produces errors errors as the channel already exists
//err1 := UpdateChannel(lao, "test.db")
//err2 := UpdateChannel(meeting, "test.db")
//err3 := UpdateChannel(rollCall, "test.db")
//err4 := UpdateChannel(poll, "test.db")
//if err1 == nil || err2 == nil || err3 == nil || err4 == nil {
//t.Errorf("able to update unexisting unsuccessful")
//}

////produces no error as the channels does not exist yet
//err1 = CreateChannel(lao, "test.db")
//err2 = CreateChannel(meeting, "test.db")
//err3 = CreateChannel(rollCall, "test.db")
//err4 = CreateChannel(poll, "test.db")
//if err1 != nil || err2 != nil || err3 != nil || err4 != nil {
//t.Errorf("Event creation in database unsuccessful")
//}

//// produces no errors as the channel already exists
//err1 = UpdateChannel(lao, "test.db")
//err2 = UpdateChannel(meeting, "test.db")
//err3 = UpdateChannel(rollCall, "test.db")
//err4 = UpdateChannel(poll, "test.db")
//if err1 != nil || err2 != nil || err3 != nil || err4 != nil {
//t.Errorf("Event creation in database unsuccessful")
//}

//err1 = CreateChannel(lao, "test.db")
//err2 = CreateChannel(meeting, "test.db")
//err3 = CreateChannel(rollCall, "test.db")
//err4 = CreateChannel(poll, "test.db")
//if err1 == nil || err2 == nil || err3 == nil || err4 == nil {
//t.Errorf("Event creation in database unsuccessful")
//}

//_ = os.Remove("test.db")
//}

//// TestGetChannel tests if retrieving a channel from the Database gives the wanted result. Does not test to retrieve
//// channels that do not exist.
//func TestGetChannel(t *testing.T) {
//// error not logged : error occurs if file does not exists
//_ = os.Remove("test.db")
//lao, meeting, rollCall, poll := getEvents()

//err1 := CreateChannel(lao, "test.db")
//err2 := CreateChannel(meeting, "test.db")
//err3 := CreateChannel(rollCall, "test.db")
//err4 := CreateChannel(poll, "test.db")
//if err1 != nil || err2 != nil || err3 != nil || err4 != nil {
//t.Errorf("Event creation in database unsuccessful")
//}

//dLao := GetChannel([]byte("1"), "test.db")
//dMeeting := GetChannel([]byte("2"), "test.db")
//dRollCall := GetChannel([]byte("3"), "test.db")
//dPoll := GetChannel([]byte("4"), "test.db")

//if dLao == nil || dMeeting == nil || dRollCall == nil || dPoll == nil {
//t.Errorf("could not retrieve entry in the database")
//}

//mLao, err1 := json.Marshal(lao)
//mMeeting, err2 := json.Marshal(meeting)
//mRollCall, err3 := json.Marshal(rollCall)
//mPoll, err4 := json.Marshal(poll)

//if !bytes.Equal(mLao, dLao) {
//t.Errorf("lao before and after storing in the database not the same")
//}
//if !bytes.Equal(mMeeting, dMeeting) {
//t.Errorf("meeting before and after storing in the database not the same")
//}
//if !bytes.Equal(mRollCall, dRollCall) {
//t.Errorf("before and after storing in the database not the same")
//}
//if !bytes.Equal(mPoll, dPoll) {
//t.Errorf("before and after storing in the database not the same")
//}

//_ = os.Remove("test.db")

//}

//// getEvents returns dummy events in order to factorize their generation
//func getEvents() (event.LAO, event.Meeting, event.RollCall, event.Poll) {
//lao := event.LAO{
//ID:            "1",
//Name:          "testLao",
//Creation:      time.Now().Unix(),
//OrganizerPKey: "oui",
//Witnesses:     nil,
//}

//meeting := event.Meeting{
//ID:           "2",
//Name:         "testMeeting",
//Creation:     time.Now().Unix(),
//LastModified: time.Now().Unix(),
//Location:     "there",
//Start:        0,
//End:          0,
//Extra:        "no extras",
//}

//rollCall := event.RollCall{
//ID:           "3",
//Name:         "testRollCall",
//Creation:     444,
//LastModified: 555,
//Location:     "nowhere",
//Start:        -1,
//}

//poll := event.Poll{
//ID:           "4",
//Name:         "test poll",
//Creation:     time.Now().Unix(),
//LastModified: time.Now().Unix(),
//Location:     "over there",
//Start:        12345,
//End:          678,
//Extra:        "still no extras",
//}

//return lao, meeting, rollCall, poll

//}
