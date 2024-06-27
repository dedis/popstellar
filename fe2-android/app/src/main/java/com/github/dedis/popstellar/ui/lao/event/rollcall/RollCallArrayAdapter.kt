package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey

class RollCallArrayAdapter(
    private val context: Context,
    private val layout: Int,
    private val attendeesList: List<PublicKey>,
    private val myToken: PoPToken?,
    private val fragment: RollCallFragment
) : ArrayAdapter<PublicKey>(context, layout, attendeesList) {

  init {
    fragment.isAttendeeListSorted(attendeesList.map { it.encoded }, context)
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view: View
    val holder: ViewHolder

    if (convertView == null) {
      view = LayoutInflater.from(context).inflate(R.layout.list_item_attendee, parent, false)
      holder = ViewHolder(view)
      view.tag = holder
    } else {
      view = convertView
      holder = view.tag as ViewHolder
    }

    val publicKey = getItem(position)
    if (publicKey != null) {
      holder.usernameTextView.text = publicKey.getLabel()
      holder.hashTextView.text = publicKey.encoded

      // Set the default color
      val defaultColor = ContextCompat.getColor(context, R.color.textOnBackground)
      holder.usernameTextView.setTextColor(defaultColor)
      holder.hashTextView.setTextColor(defaultColor)

      // highlights our token in the list
      if (myToken != null && publicKey.encoded == myToken.publicKey.encoded) {
        val colorAccent = ContextCompat.getColor(context, R.color.colorAccent)
        holder.usernameTextView.setTextColor(colorAccent)
        holder.hashTextView.setTextColor(colorAccent)
      }
    }

    return view
  }

  private class ViewHolder(view: View) {
    val usernameTextView: TextView = view.findViewById(R.id.username_text_view)
    val hashTextView: TextView = view.findViewById(R.id.hash_text_view)
  }
}
