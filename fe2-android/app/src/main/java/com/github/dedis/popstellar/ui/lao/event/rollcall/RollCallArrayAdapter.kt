package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.github.dedis.popstellar.model.objects.security.PoPToken

class RollCallArrayAdapter(
        private val context: Context,
        private val layout: Int,
        private val attendeesList: List<String>,
        private val myToken: PoPToken?
) : ArrayAdapter<String>(context, layout, attendeesList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        //highlights our token in the list
        val currentToken = getItem(position)
        if (myToken != null && currentToken == myToken.publicKey.encoded) {
            (view as TextView).setTypeface(null, Typeface.BOLD)
            view.setTextColor(Color.BLUE)
        }

        return view
    }
}