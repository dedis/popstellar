package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.testutils.Base64DataUtils
import java.time.Instant
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class LaoTest {
  @Test
  fun createLaoNullParametersTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { Lao(null, ORGANIZER, 2L) }
    Assert.assertThrows(IllegalArgumentException::class.java) { Lao(null as String?) }
  }

  @Test
  fun createLaoEmptyNameTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { Lao("", ORGANIZER, 2L) }
  }

  @Test
  fun createLaoEmptyIdTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { Lao("") }
  }

  @Test
  fun setAndGetNameTest() {
    MatcherAssert.assertThat(LAO_1.name, CoreMatchers.`is`(LAO_NAME_1))
    LAO_1.setName("New Name")
    MatcherAssert.assertThat(LAO_1.name, CoreMatchers.`is`("New Name"))
  }

  @Test
  fun setAndGetOrganizerTest() {
    LAO_1.organizer = ORGANIZER
    MatcherAssert.assertThat(LAO_1.organizer, CoreMatchers.`is`(ORGANIZER))
  }

  @Test
  fun setNullNameTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { LAO_1.setName(null) }
  }

  @Test
  fun setEmptyNameTest() {
    Assert.assertThrows(IllegalArgumentException::class.java) { LAO_1.setName("") }
  }

  @Test
  fun setAndGetModificationIdTest() {
    val id = Base64DataUtils.generateMessageID()
    LAO_1.modificationId = id
    MatcherAssert.assertThat(LAO_1.modificationId, CoreMatchers.`is`(id))
  }

  @Test
  fun setAndGetCreation() {
    LAO_1.creation = 0xFFL
    MatcherAssert.assertThat(LAO_1.creation, CoreMatchers.`is`(0xFFL))
  }

  @Test
  fun setAndGetLastModified() {
    LAO_1.lastModified = 0xFFL
    MatcherAssert.assertThat(LAO_1.lastModified, CoreMatchers.`is`(0xFFL))
  }

  @Test
  fun setAndGetId() {
    LAO_1.id = "New_Id"
    MatcherAssert.assertThat(LAO_1.id, CoreMatchers.`is`("New_Id"))
  }

  @Test
  fun setEmptyIdThrowsException() {
    Assert.assertThrows(IllegalArgumentException::class.java) { LAO_1.id = "" }
  }

  @Test
  fun setPendingUpdatesTest() {
    val update = PendingUpdate(1L, MessageID("foo"))
    LAO_1.pendingUpdates = mutableSetOf(update)
    Assert.assertTrue(LAO_1.pendingUpdates.contains(update))
  }

  companion object {
    private const val LAO_NAME_1 = "LAO name 1"
    private val ORGANIZER = Base64DataUtils.generatePublicKey()
    private val LAO_1 = Lao(LAO_NAME_1, ORGANIZER, Instant.now().epochSecond)
  }
}
