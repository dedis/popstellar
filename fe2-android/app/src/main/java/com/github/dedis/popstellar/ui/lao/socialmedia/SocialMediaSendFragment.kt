package com.github.dedis.popstellar.ui.lao.socialmedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainSocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.keys.KeyException
import dagger.hilt.android.AndroidEntryPoint
import java.security.GeneralSecurityException
import java.time.Instant

/** Fragment where we can write and send a chirp */
@AndroidEntryPoint
class SocialMediaSendFragment : Fragment() {

  private lateinit var binding: SocialMediaSendFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var socialMediaViewModel: SocialMediaViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = SocialMediaSendFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    socialMediaViewModel = obtainSocialMediaViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.viewModel = socialMediaViewModel
    binding.lifecycleOwner = viewLifecycleOwner

    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupSendChirpButton()
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.send)
    laoViewModel.setIsTab(false)
  }

  private fun setupSendChirpButton() {
    binding.sendChirpButton.setOnClickListener { sendNewChirp() }
  }

  private fun sendNewChirp() {
    // Trying to send a chirp when no LAO has been chosen in the application will not send it, it
    // will make a toast appear and it will log the error
    if (laoViewModel.laoId == null) {
      logAndShow(requireContext(), TAG, R.string.error_no_lao)
    } else {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .sendChirp(binding.entryBoxChirp.text.toString(), null, Instant.now().epochSecond)
              .subscribe(
                  {
                    SocialMediaHomeFragment.setCurrentFragment(
                        parentFragmentManager, R.id.fragment_chirp_list) {
                          ChirpListFragment.newInstance()
                        }
                  },
                  { error: Throwable ->
                    if (error is KeyException || error is GeneralSecurityException) {
                      logAndShow(requireContext(), TAG, error, R.string.error_retrieve_own_token)
                    } else {
                      logAndShow(requireContext(), TAG, error, R.string.error_sending_chirp)
                    }
                  }))
      socialMediaViewModel.setBottomNavigationTab(SocialMediaTab.HOME)
    }
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "chirp list") {
          ChirpListFragment.openFragment(parentFragmentManager)
        })
  }

  companion object {
    val TAG: String = SocialMediaSendFragment::class.java.simpleName

    fun newInstance(): SocialMediaSendFragment {
      return SocialMediaSendFragment()
    }
  }
}
