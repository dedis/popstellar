package com.github.dedis.popstellar.ui.lao.event

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.Constants.ID_NULL
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import javax.inject.Inject

abstract class AbstractEventFragment : Fragment() {
  @Inject lateinit var gson: Gson
  protected lateinit var laoViewModel: LaoViewModel

  private val statusTextMap = buildStatusTextMap()
  private val statusIconMap = buildStatusIconMap()
  private val statusColorMap = buildStatusColorMap()
  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH)

  protected fun setTab(@StringRes pageTitle: Int) {
    laoViewModel.setIsTab(false)
    laoViewModel.setPageTitle(pageTitle)
  }

  protected fun setupTime(event: Event?, startTime: TextView, endTime: TextView) {
    if (event == null) {
      return
    }
    val start = Date(event.startTimestampInMillis)
    val end = Date(event.endTimestampInMillis)

    startTime.text = dateFormat.format(start)
    endTime.text = dateFormat.format(end)
  }

  protected fun setStatus(state: EventState, statusIcon: ImageView, statusText: TextView) {
    val imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(state, ID_NULL))

    statusIcon.setImageDrawable(imgStatus)
    setImageColor(statusIcon, statusColorMap.getOrDefault(state, ID_NULL))
    statusText.setText(statusTextMap.getOrDefault(state, ID_NULL))
    statusText.setTextColor(resources.getColor(statusColorMap.getOrDefault(state, ID_NULL), null))
  }

  private fun getDrawableFromContext(id: Int): Drawable? {
    return AppCompatResources.getDrawable(requireContext(), id)
  }

  private fun setImageColor(imageView: ImageView, colorId: Int) {
    ImageViewCompat.setImageTintList(imageView, resources.getColorStateList(colorId, null))
  }

  private fun buildStatusTextMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)
    map[EventState.CREATED] = R.string.created_displayed_text
    map[EventState.OPENED] = R.string.open
    map[EventState.CLOSED] = R.string.closed
    return map
  }

  private fun buildStatusIconMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)
    map[EventState.CREATED] = R.drawable.ic_lock
    map[EventState.OPENED] = R.drawable.ic_unlock
    map[EventState.CLOSED] = R.drawable.ic_lock
    return map
  }

  private fun buildStatusColorMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)
    map[EventState.CREATED] = R.color.red
    map[EventState.OPENED] = R.color.green
    map[EventState.CLOSED] = R.color.red
    return map
  }

  protected fun handleBackNav(tag: String) {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, tag)
  }
}
