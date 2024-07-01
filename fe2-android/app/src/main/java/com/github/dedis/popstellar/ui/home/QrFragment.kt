package com.github.dedis.popstellar.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.QrFragmentBinding
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.GeneralUtils
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode

@AndroidEntryPoint
class QrFragment : Fragment() {
  // Dependencies
  @Inject lateinit var gson: Gson

  @Inject lateinit var keyManager: KeyManager

  private lateinit var binding: QrFragmentBinding
  private lateinit var viewModel: HomeViewModel
  private lateinit var clipboardManager: GeneralUtils.ClipboardUtil

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    binding = QrFragmentBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = activity
    clipboardManager = GeneralUtils.ClipboardUtil(requireActivity())

    viewModel = HomeActivity.obtainViewModel(requireActivity())

    // Add the public key in form of text and QR
    setQRFromPublicKey()
    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupCopyButton()
  }

  private fun setupCopyButton() {
    clipboardManager.setupCopyButton(binding.copyPublicKeyButton, binding.pkText, "Public Key")
  }

  override fun onResume() {
    super.onResume()
    viewModel.setPageTitle(R.string.witness_qr)
    viewModel.setIsHome(false)
  }

  private fun setQRFromPublicKey() {
    val pk = keyManager.mainPublicKey
    binding.pkText.text = pk.encoded

    // Display the QR code
    val data = MainPublicKeyData(pk)
    val myBitmap =
        QRCode.from(gson.toJson(data))
            .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
            .bitmap()
    binding.pkQrCode.setImageBitmap(myBitmap)
  }

  private fun handleBackNav() {
    HomeActivity.addBackNavigationCallbackToHome(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = QrFragment::class.java.simpleName

    fun newInstance(): QrFragment {
      return QrFragment()
    }
  }
}
