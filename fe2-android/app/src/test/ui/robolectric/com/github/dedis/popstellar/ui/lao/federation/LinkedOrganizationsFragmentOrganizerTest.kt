package com.github.dedis.popstellar.ui.lao.federation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.BundleBuilder
import com.github.dedis.popstellar.testutils.fragment.ActivityFragmentScenarioRule
import com.github.dedis.popstellar.testutils.pages.lao.LaoActivityPageObject
import com.github.dedis.popstellar.testutils.pages.lao.federation.LinkedOrganizationsFragmentPageObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoTestRule
import javax.inject.Inject

@SmallTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LinkedOrganizationsFragmentOrganizerTest {

    @Inject
    lateinit var laoRepository: LAORepository

    @BindValue @Mock
    lateinit var keyManager: KeyManager

    @JvmField @Rule var rule = InstantTaskExecutorRule()

    @JvmField @Rule(order = 0) val mockitoRule: MockitoTestRule = MockitoJUnit.testRule(this)

    @JvmField @Rule(order = 1) val hiltRule = HiltAndroidRule(this)

    // Loads the repository but no repository for now
    @JvmField
    @Rule(order = 2)
    val setupRule: ExternalResource =
            object : ExternalResource() {
                override fun before() {
                    hiltRule.inject()
                    laoRepository.updateLao(LAO)
                    Mockito.`when`(keyManager.mainKeyPair).thenReturn(KEY_PAIR)
                    Mockito.`when`(keyManager.mainPublicKey).thenReturn(KEY_PAIR.publicKey)
                }
            }

    @JvmField
    @Rule(order = 3)
    var activityScenarioRule: ActivityFragmentScenarioRule<LaoActivity, LinkedOrganizationsFragment> =
            ActivityFragmentScenarioRule.launchIn(
                    LaoActivity::class.java,
                    BundleBuilder().putString(LaoActivityPageObject.laoIdExtra(), LAO_ID).build(),
                    LaoActivityPageObject.containerId(),
                    LinkedOrganizationsFragment::class.java
            ) {
                LinkedOrganizationsFragment.newInstance()
            }

    @Test
    fun testButtonVisibilityForOrganizerRole() {
        LinkedOrganizationsFragmentPageObject.createLinkButton()
                .check(matches(isDisplayed()))
    }

    @Test
    fun testButtonClick() {
        LinkedOrganizationsFragmentPageObject.inviteButton()
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        LinkedOrganizationsFragmentPageObject.joinInvitationButton()
                .check(matches(withEffectiveVisibility(Visibility.GONE)))

        LinkedOrganizationsFragmentPageObject.createLinkButton().perform(ViewActions.click())

        LinkedOrganizationsFragmentPageObject.inviteButton()
                .check(matches(isDisplayed()))
        LinkedOrganizationsFragmentPageObject.joinInvitationButton()
                .check(matches(isDisplayed()))
    }

    companion object {
        private val KEY_PAIR = Base64DataUtils.generateKeyPair()
        private const val LAO_NAME = "LAO"
        private val LAO = Lao(LAO_NAME, KEY_PAIR.publicKey, 10223421)
        private val LAO_ID = LAO.id
    }
}
