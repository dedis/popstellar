package com.github.dedis.popstellar.ui.lao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.InviteFragmentPageObject
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule

@SmallTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class InviteFragmentTest {
  @Inject lateinit var laoRepository: LAORepository

  @BindValue @Mock lateinit var keyManager: KeyManager

  @JvmField @Rule var rule = InstantTaskExecutorRule()

  @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

  @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

  @JvmField
  @Rule(order = 2)
  val setupRule: ExternalResource =
    object : ExternalResource() {
      override fun before() {
        hiltRule.inject()
        laoRepository.updateLao(LAO)
        Mockito.`when`(keyManager.mainKeyPair).thenReturn(KEY_PAIR)
        Mockito.`when`(keyManager.mainPublicKey).thenReturn(PK)
      }
    }

  @JvmField
  @Rule(order = 3)
  var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, InviteFragment> =
    ActivityFragmentScenarioRule.launchIn(
      LaoActivity::class.java,
      BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO.id).build(),
      LaoActivityPageObject.containerId(),
      InviteFragment::class.java
    ) {
      InviteFragment.newInstance()
    }

  @Test
  fun displayedInfoIsCorrect() {
    InviteFragmentPageObject.roleText()
      .check(ViewAssertions.matches(ViewMatchers.withText("Organizer")))
    InviteFragmentPageObject.laoNameText()
      .check(ViewAssertions.matches(ViewMatchers.withText(LAO_NAME)))
    InviteFragmentPageObject.identifierText()
      .check(ViewAssertions.matches(ViewMatchers.withText(LAO.id)))
  }

  companion object {
    private const val LAO_NAME = "LAO"
    private val KEY_PAIR = Base64DataUtils.generateKeyPair()
    private val PK = KEY_PAIR.publicKey
    private val LAO = Lao(LAO_NAME, PK, 44444444)
  }
}
