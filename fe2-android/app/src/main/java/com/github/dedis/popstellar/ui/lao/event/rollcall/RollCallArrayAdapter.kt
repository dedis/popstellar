package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PoPToken

class RollCallArrayAdapter(
    private val context: Context,
    private val layout: Int,
    private val attendeesList: List<String>,
    private val myToken: PoPToken?,
) : ArrayAdapter<String>(context, layout, attendeesList) {

  init {
    RollCallFragment.isAttendeeListSorted(attendeesList, context)
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view = super.getView(position, convertView, parent)

    // highlights our token in the list
    val currentToken = getItem(position)
    if (myToken != null && currentToken == myToken.publicKey.encoded) {
      val colorAccent = ContextCompat.getColor(context, R.color.colorAccent)
      (view as TextView).setTextColor(colorAccent)
    }

    return view
  }
}
