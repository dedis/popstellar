package com.github.dedis.popstellar.ui.lao

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.InviteFragmentBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.model.qrcode.ConnectToLao
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.GeneralUtils
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class InviteFragment : Fragment() {
  @Inject lateinit var gson: Gson
  @Inject lateinit var networkManager: GlobalNetworkManager

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var binding: InviteFragmentBinding
  private lateinit var clipboardManager: GeneralUtils.ClipboardUtil

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = InviteFragmentBinding.inflate(inflater, container, false)
    laoViewModel = LaoActivity.obtainViewModel(requireActivity())
    clipboardManager = GeneralUtils.ClipboardUtil(requireActivity())

    // Display the LAO identifier, not the device public key
    binding.laoPropertiesIdentifierText.text = laoViewModel.laoId
    binding.laoPropertiesServerText.text = networkManager.currentUrl

    try {
      val laoView = laoViewModel.lao
      val data = ConnectToLao(networkManager.currentUrl!!, laoView.id)
      val myBitmap =
          QRCode.from(gson.toJson(data))
              .withSize(QR_SIDE, QR_SIDE)
              .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
              .bitmap()

      binding.channelQrCode.setImageBitmap(myBitmap)
      binding.laoPropertiesNameText.text = laoView.name

      laoViewModel.role.observe(viewLifecycleOwner) { role: Role ->
        binding.laoPropertiesRoleText.setText(role.stringId)
      }
    } catch (e: UnknownLaoException) {
      logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception)
      return null
    }

    handleBackNav()
    clipboardManager.setupCopyButton(
        binding.copyServerButton, binding.laoPropertiesServerText, "Server Address")
    clipboardManager.setupCopyButton(
        binding.copyIdentifierButton, binding.laoPropertiesIdentifierText, "LAO ID")

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.invite)
    laoViewModel.setIsTab(true)
  }

  private fun handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    private val TAG: String = InviteFragment::class.java.simpleName
    private const val QR_SIDE = 800

    @JvmStatic
    fun newInstance(): InviteFragment {
      return InviteFragment()
    }
  }
}
