package com.github.dedis.popstellar.ui

import android.content.Context
import android.transition.Slide
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.github.dedis.popstellar.databinding.NetworkStatusBinding

class NetworkStatusView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

  private val binding = NetworkStatusBinding.inflate(LayoutInflater.from(context), this, true)

  fun setIsNetworkConnected(isVisible: Boolean) {
    setVisibility(isVisible)
  }

  private fun setVisibility(isVisible: Boolean) {
    TransitionManager.beginDelayedTransition(
        this.parent as ViewGroup,
        Slide(Gravity.TOP).apply {
          addTarget(binding.networkConnectionContainer)
          duration = VISIBILITY_CHANGE_DURATION
        })
    binding.networkConnectionContainer.isVisible = !isVisible
  }

  companion object {
    private const val VISIBILITY_CHANGE_DURATION = 400L
  }
}
