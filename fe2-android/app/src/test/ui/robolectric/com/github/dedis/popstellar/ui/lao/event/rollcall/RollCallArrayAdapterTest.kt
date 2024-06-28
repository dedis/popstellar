package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RollCallArrayAdapterTest {

    @Mock
    private lateinit var mockView: View

    private lateinit var adapter: RollCallArrayAdapter

    val context = ApplicationProvider.getApplicationContext<Context>()

    private val MY_PRIVATE_KEY =
        Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")
    private val MY_PUBLIC_KEY =
        Utils.hexToBytes("e5cdb393fe6e0abacd99d521400968083a982400b6ac3e0a1e8f6018d1554bd7")
    private val OTHER_PRIVATE_KEY =
        Utils.hexToBytes("cf74d353042400806ee94c3e77eef983d9a1434d21c0a7568f203f5b091dde1d")
    private val OTHER_PUBLIC_KEY =
        Utils.hexToBytes("6015ae4d770294f94e651a9fd6ba9c6a11e5c80803c63ee472ad525f4c3523a6")

    private lateinit var attendeesList: List<PublicKey>

    @Before
    fun setup() {
        // Setting up a list of two tokens and the view
        val myToken = PoPToken(MY_PRIVATE_KEY, MY_PUBLIC_KEY)
        val otherToken = PoPToken(OTHER_PRIVATE_KEY, OTHER_PUBLIC_KEY)
        attendeesList = listOf(myToken.publicKey, otherToken.publicKey)
        adapter = RollCallArrayAdapter(context, R.layout.list_item_attendee, attendeesList, myToken, mock(RollCallFragment::class.java))

        // Use the correct layout for mockView
        val inflater = LayoutInflater.from(context)
        val parent = LinearLayout(context)
        mockView = inflater.inflate(R.layout.list_item_attendee, parent, false)
    }

    @Test
    fun verifyOurTokenIsHighlighted() {
        val parent = LinearLayout(context)
        val view = adapter.getView(0, null, parent)
        val usernameTextView = view.findViewById<TextView>(R.id.username_text_view)
        val hashTextView = view.findViewById<TextView>(R.id.hash_text_view)
        val color = ContextCompat.getColor(context, R.color.colorAccent)

        Assert.assertEquals(color, usernameTextView.currentTextColor)
        Assert.assertEquals(color, hashTextView.currentTextColor)
    }

    @Test
    fun verifyOtherTokenIsNotHighlighted() {
        val parent = LinearLayout(context)
        val view = adapter.getView(1, null, parent)
        val usernameTextView = view.findViewById<TextView>(R.id.username_text_view)
        val hashTextView = view.findViewById<TextView>(R.id.hash_text_view)
        val color = ContextCompat.getColor(context, R.color.textOnBackground)

        Assert.assertEquals(color, usernameTextView.currentTextColor)
        Assert.assertEquals(color, hashTextView.currentTextColor)
    }
}




