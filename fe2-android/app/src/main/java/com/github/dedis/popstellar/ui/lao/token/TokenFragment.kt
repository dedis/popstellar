package com.github.dedis.popstellar.ui.lao.token

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.TokenFragmentBinding
import com.github.dedis.popstellar.model.qrcode.PopTokenData
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.ui.home.LaoCreateFragment
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.UIUtils
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode
import timber.log.Timber

@AndroidEntryPoint
class TokenFragment : Fragment() {
  @Inject lateinit var gson: Gson

  @Inject lateinit var rollCallRepo: RollCallRepository

  @Inject lateinit var keyManager: KeyManager

  private lateinit var laoViewModel: LaoViewModel

  private lateinit var clipboardManager: UIUtils.ClipboardUtil

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.token)
    laoViewModel.setIsTab(false)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    val binding = TokenFragmentBinding.inflate(inflater, container, false)
    laoViewModel = obtainViewModel(requireActivity())
    clipboardManager = UIUtils.ClipboardUtil(requireActivity())

    try {
      val laoId = laoViewModel.laoId!!
      val rollCall =
          rollCallRepo.getRollCallWithPersistentId(
              laoId, requireArguments().getString(Constants.ROLL_CALL_ID)!!)

      Timber.tag(TAG).d("token displayed from roll call: %s", rollCall)

      val poPToken = keyManager.getValidPoPToken(laoId, rollCall)
      val data = PopTokenData(poPToken.publicKey)
      val bitmap =
          QRCode.from(gson.toJson(data))
              .withSize(Constants.QR_SIDE, Constants.QR_SIDE)
              .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
              .bitmap()

      binding.tokenQrCode.setImageBitmap(bitmap)
      binding.tokenTextUsernameView.text = poPToken.publicKey.getLabel()
      binding.tokenTextView.text = poPToken.publicKey.encoded
      clipboardManager.setupCopyButton(binding.tokenCopyButton, binding.tokenTextView, "Token")
    } catch (e: Exception) {
      when (e) {
        is UnknownRollCallException,
        is KeyException -> {
          logAndShow(requireContext(), TAG, e, R.string.error_retrieve_own_token)
          setCurrentFragment(parentFragmentManager, R.id.fragment_event_list) {
            LaoCreateFragment()
          }
          return null
        }
        else -> throw e
      }
    }

    handleBackNav()

    return binding.root
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "token list") {
          TokenListFragment.openFragment(parentFragmentManager)
        })
  }

  companion object {
    private val TAG: String = TokenFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(rcId: String): TokenFragment {
      val fragment = TokenFragment()
      val args = Bundle()
      args.putString(Constants.ROLL_CALL_ID, rcId)
      fragment.arguments = args

      return fragment
    }
  }
}
