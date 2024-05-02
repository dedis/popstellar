package com.github.dedis.popstellar.ui.lao.federation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LinkedOrganizationsInviteFragmentBinding
import com.github.dedis.popstellar.model.qrcode.ConnectToLao
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainLinkedOrganizationsViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import java.time.Instant
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class LinkedOrganizationsInviteFragment : Fragment() {
  @Inject lateinit var gson: Gson
  @Inject lateinit var networkManager: GlobalNetworkManager

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var linkedOrganizationsViewModel: LinkedOrganizationsViewModel
  private lateinit var challengeRequest: Completable

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val binding = LinkedOrganizationsInviteFragmentBinding.inflate(inflater, container, false)
    laoViewModel = LaoActivity.obtainViewModel(requireActivity())
    linkedOrganizationsViewModel =
        obtainLinkedOrganizationsViewModel(requireActivity(), laoViewModel.laoId)

    binding.linkedOrganizationsServerText.text = networkManager.currentUrl

    if (CREATES_INVITATION) {
      challengeRequest =
          linkedOrganizationsViewModel.sendChallengeRequest(Instant.now().epochSecond).doOnError {
            binding.nextStepButton.setText(R.string.finish)
          }
      binding.nextStepButton.setText(R.string.next_step)
    } else {
      binding.nextStepButton.setText(R.string.finish)
    }

    // TODO adapt this to real QR code data
    linkedOrganizationsViewModel.doWhenChallengeIsReceived { challenge ->
      try {
        val laoView = laoViewModel.lao
        val data = ConnectToLao(networkManager.currentUrl!!, laoView.id)
        val myBitmap =
            QRCode.from(gson.toJson(data))
                .withSize(QR_SIDE, QR_SIDE)
                .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
                .bitmap()

        binding.federationQrCode.setImageBitmap(myBitmap)
        binding.linkedOrganizationsNameText.text = laoView.name
      } catch (e: UnknownLaoException) {
        logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception)
      }
    }

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.invite_other_organization)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    val activity = requireActivity()
    LaoActivity.addBackNavigationCallback(
        activity,
        viewLifecycleOwner,
        ActivityUtils.buildBackButtonCallback(TAG, "Linked organizations") {
          LaoActivity.setCurrentFragment(
              parentFragmentManager,
              R.id.fragment_linked_organizations_home,
          ) {
            LinkedOrganizationsFragment()
          }
        },
    )
  }

  companion object {
    private val TAG: String = LinkedOrganizationsInviteFragment::class.java.simpleName
    private const val QR_SIDE = 800
    private var CREATES_INVITATION = false

    @JvmStatic
    fun newInstance(createsInvitation: Boolean): LinkedOrganizationsInviteFragment {
      CREATES_INVITATION = createsInvitation
      return LinkedOrganizationsInviteFragment()
    }
  }
}
