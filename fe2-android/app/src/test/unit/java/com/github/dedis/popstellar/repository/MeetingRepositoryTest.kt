package com.github.dedis.popstellar.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class MeetingRepositoryTest {
  private val application = ApplicationProvider.getApplicationContext<Application>()

  @Mock private lateinit var appDatabase: AppDatabase

  @Mock private lateinit var meetingDao: MeetingDao
  private lateinit var meetingRepository: MeetingRepository

  @JvmField @Rule(order = 0) val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Before
  fun setUp() {
    Mockito.`when`(appDatabase.meetingDao()).thenReturn(meetingDao)
    meetingRepository = MeetingRepository(appDatabase, application)

    Mockito.`when`(meetingDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(meetingDao.getMeetingsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))

    meetingRepository.updateMeeting(LAO_ID, meeting)
  }

  @Test
  @Throws(UnknownMeetingException::class)
  fun addMeetingAddsMeetingToRepository() {
    val retrievedMeeting = meetingRepository.getMeetingWithId(LAO_ID, ID)
    Assert.assertEquals(meeting, retrievedMeeting)
  }

  @Test(expected = IllegalArgumentException::class)
  fun addMeetingThrowsExceptionWhenLaoIdIsNull() {
    meetingRepository.updateMeeting(null, meeting)
  }

  @Test(expected = IllegalArgumentException::class)
  fun addMeetingThrowsExceptionWhenMeetingIsNull() {
    meetingRepository.updateMeeting(LAO_ID, null)
  }

  @Throws(UnknownMeetingException::class)
  @Test(expected = UnknownMeetingException::class)
  fun meetingWithIdThrowsExceptionWhenMeetingDoesNotExist() {
    meetingRepository.getMeetingWithId(LAO_ID, ID + "2")
  }

  @Test
  fun meetingsObservableInLaoReturnsObservableOfMeetings() {
    val observer = TestObserver<Set<Meeting>>()
    meetingRepository.getMeetingsObservableInLao(LAO_ID).subscribe(observer)

    observer.awaitCount(1)
    val meetings = observer.values()[0]

    Assert.assertTrue(meetings.contains(meeting))
  }

  companion object {
    private const val LAO_ID = "LAO_ID"
    private const val ID = "ID"
    private const val NAME = "MEETING_NAME"
    private const val LOCATION = "Test Location"
    private val CREATION = System.currentTimeMillis()
    private val START = CREATION + 1000
    private val END = START + 1000
    private val LAST_MODIFIED = CREATION
    private const val MODIFICATION_ID = "MOD_ID"
    private val MODIFICATION_SIGNATURES: List<String> = ArrayList()
    private val meeting =
      Meeting(
        ID,
        NAME,
        CREATION,
        START,
        END,
        LOCATION,
        LAST_MODIFIED,
        MODIFICATION_ID,
        MODIFICATION_SIGNATURES
      )
  }
}
