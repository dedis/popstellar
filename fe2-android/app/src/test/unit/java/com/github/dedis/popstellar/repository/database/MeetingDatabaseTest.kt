package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeetingDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var meetingDao: MeetingDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    meetingDao = appDatabase.meetingDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertTest() {
    val testObserver = meetingDao.insert(MEETING_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun insertAndGetTest() {
    val testObserver = meetingDao.insert(MEETING_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<Meeting>?> =
      meetingDao.getMeetingsByLaoId(LAO_ID).test().assertValue { meetings: List<Meeting> ->
        meetings.size == 1 && meetings[0] == MEETING
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  companion object {
    private val CREATION = Instant.now().epochSecond
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), CREATION, "Lao")
    private val MEETING_ID = generateCreateMeetingId(LAO_ID, CREATION, "name")
    private val MEETING =
      Meeting(
        MEETING_ID,
        "name",
        CREATION,
        CREATION + 10,
        CREATION + 20,
        "loc",
        CREATION,
        "modId",
        ArrayList()
      )
    private val MEETING_ENTITY = MeetingEntity(LAO_ID, MEETING)
  }
}
