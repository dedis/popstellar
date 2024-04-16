package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PoPToken
import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RollCallArrayAdapterTest {

    @Mock
    private lateinit var mockView: View

    private lateinit var adapter: RollCallArrayAdapter

    private val MY_PRIVATE_KEY =
            Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")
    private val MY_PUBLIC_KEY =
            Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7")
    private val OTHER_PRIVATE_KEY =
            Utils.hexToBytes("cf74d353042400806ee94c3e77eef983d9a1434d21c0a7568f203f5b091dde1d")
    private val OTHER_PUBLIC_KEY =
            Utils.hexToBytes("6015ae4d770294f94e651a9fd6ba9c6a11e5c80803c63ee472ad525f4c3523a6")

    private lateinit var attendeesList: List<String>

    @Before
    fun setup() {
        // Setting up a list of two tokens and the view
        val myToken = PoPToken(MY_PRIVATE_KEY, MY_PUBLIC_KEY)
        val otherToken = PoPToken(OTHER_PRIVATE_KEY, OTHER_PUBLIC_KEY)
        attendeesList = listOf(myToken.publicKey.encoded, otherToken.publicKey.encoded)
        adapter = RollCallArrayAdapter(RuntimeEnvironment.application, R.id.valid_token_layout_text, attendeesList, myToken)
        mockView = TextView(RuntimeEnvironment.application)
        val colorAccent = ContextCompat.getColor(RuntimeEnvironment.application, R.color.textOnBackground)
        (mockView as TextView).setTextColor(colorAccent)
    }

    @Test
    fun verify_our_token_is_highlighted() {
        val view = adapter.getView(0, mockView, mock(ViewGroup::class.java)) as TextView
        val color = ContextCompat.getColor(RuntimeEnvironment.application, R.color.colorAccent)
        Assert.assertEquals(color, view.currentTextColor)
    }

    @Test
    fun verify_other_token_is_not_highlighted() {
        val view = adapter.getView(1, mockView, mock(ViewGroup::class.java)) as TextView
        val color = ContextCompat.getColor(RuntimeEnvironment.application, R.color.textOnBackground)
        Assert.assertEquals(color, view.currentTextColor)
    }

}
