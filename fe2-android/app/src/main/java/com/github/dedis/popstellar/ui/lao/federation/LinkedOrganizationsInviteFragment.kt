package com.github.dedis.popstellar.ui.lao.federation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LinkedOrganizationsInviteFragmentBinding
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.qrcode.FederationDetails
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainLinkedOrganizationsViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.ActivityUtils
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class LinkedOrganizationsInviteFragment : Fragment() {
  @Inject lateinit var gson: Gson
  @Inject lateinit var networkManager: GlobalNetworkManager

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var linkedOrganizationsViewModel: LinkedOrganizationsViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    val binding = LinkedOrganizationsInviteFragmentBinding.inflate(inflater, container, false)
    laoViewModel = LaoActivity.obtainViewModel(requireActivity())
    linkedOrganizationsViewModel =
        obtainLinkedOrganizationsViewModel(requireActivity(), laoViewModel.laoId)

    if (createdInvitation) {
      // When the user creates the invitation
      createInvitationSetup(binding)
    } else {
      // When the user joins an invitation
      binding.loadingText.visibility = View.GONE
      binding.nextStepButton.setText(R.string.finish)
      binding.nextStepButton.setOnClickListener { finishButton() }
      displayQrCodeAndInfo(binding, null)
    }

    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(
        if (createdInvitation) R.string.invite_other_organization
        else R.string.join_other_organization_invitation)
    laoViewModel.setIsTab(false)
  }

  private fun displayQrCodeAndInfo(
      binding: LinkedOrganizationsInviteFragmentBinding,
      challenge: Challenge?,
  ) {
    try {
      val laoView = laoViewModel.lao
      val data =
          FederationDetails(
              laoView.id,
              networkManager.currentUrl!!,
              laoViewModel.getPublicKey().encoded,
              challenge)
      val myBitmap =
          QRCode.from(gson.toJson(data))
              .withSize(QR_SIDE, QR_SIDE)
              .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
              .bitmap()

      CoroutineScope(Dispatchers.Main).launch {
        binding.federationQrCode.setImageBitmap(myBitmap)
        binding.loadingText.visibility = View.GONE
        binding.scanQrText.visibility = View.VISIBLE
        binding.linkedOrganizationsNameTitle.visibility = View.VISIBLE
        binding.linkedOrganizationsServerTitle.visibility = View.VISIBLE
        binding.linkedOrganizationsNameText.text = laoView.name
        binding.linkedOrganizationsServerText.text = networkManager.currentUrl
      }
    } catch (e: UnknownLaoException) {
      logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception)
    }
  }

  private fun createInvitationSetup(binding: LinkedOrganizationsInviteFragmentBinding) {
    binding.nextStepButton.setText(R.string.next_step)
    binding.nextStepButton.visibility = View.GONE
    binding.nextStepButton.setOnClickListener { openScanner() }
    binding.scanQrText.visibility = View.GONE
    binding.loadingText.visibility = View.VISIBLE
    binding.linkedOrganizationsNameTitle.visibility = View.GONE
    binding.linkedOrganizationsServerTitle.visibility = View.GONE
    laoViewModel.addDisposable(
        linkedOrganizationsViewModel
            .sendChallengeRequest(Instant.now().epochSecond)
            .subscribe(
                { binding.nextStepButton.visibility = View.VISIBLE },
                { error: Throwable ->
                  logAndShow(requireContext(), TAG, error, R.string.error_sending_challenge_request)
                },
            ))
    linkedOrganizationsViewModel.doWhenChallengeIsReceived { challenge ->
      displayQrCodeAndInfo(binding, challenge)
    }
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

  private fun finishButton() {
    if (!linkedOrganizationsViewModel.isRepositoryValid() ||
        linkedOrganizationsViewModel.getChallenge() == null) {
      logAndShow(requireContext(), TAG, R.string.error_invalid_federation_info)
    } else {
      laoViewModel.addDisposable(
          linkedOrganizationsViewModel
              .sendFederationInit(
                  linkedOrganizationsViewModel.getOtherLao()!!,
                  linkedOrganizationsViewModel.getOtherServerAddress()!!,
                  linkedOrganizationsViewModel.getOtherPublicKey()!!,
                  linkedOrganizationsViewModel.getChallenge()!!,
              )
              .subscribe(
                  { logAndShow(requireContext(), TAG, R.string.init_sent) },
                  { error: Throwable ->
                    logAndShow(requireContext(), TAG, error, R.string.error_sending_federation_init)
                  },
              ))
    }
    LaoActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_linked_organizations_home) {
      LinkedOrganizationsFragment.newInstance()
    }
  }

  private fun openScanner() {
    laoViewModel.setIsTab(false)
    linkedOrganizationsViewModel.manager = parentFragmentManager
    LaoActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
      QrScannerFragment.newInstance(ScanningAction.FEDERATION_INVITE)
    }
  }

  companion object {
    private val TAG: String = LinkedOrganizationsInviteFragment::class.java.simpleName
    private const val QR_SIDE = 800
    private var createdInvitation = false

    @JvmStatic
    fun newInstance(createsInvitation: Boolean): LinkedOrganizationsInviteFragment {
      createdInvitation = createsInvitation
      return LinkedOrganizationsInviteFragment()
    }
  }
}
