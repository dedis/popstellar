package com.github.dedis.popstellar.ui.lao.digitalcash

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.DigitalCashReceiveFragmentBinding
import com.github.dedis.popstellar.model.qrcode.PopTokenData
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode

/**
 * A simple [Fragment] subclass. Use the [DigitalCashReceiveFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class DigitalCashReceiveFragment : Fragment() {
  @Inject lateinit var gson: Gson

  private lateinit var binding: DigitalCashReceiveFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var digitalCashViewModel: DigitalCashViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = DigitalCashReceiveFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    setHomeInterface()
    handleBackNav()

    return binding.root
  }

  private fun setHomeInterface() {
    // Subscribe to roll calls so that our own address is kept updated in case a new rc is closed
    laoViewModel.addDisposable(
        digitalCashViewModel.rollCallsObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                  val token = digitalCashViewModel.validToken
                  val publicKey = token.publicKey

                  binding.digitalCashReceiveAddress.text = publicKey.getLabel()

                  val tokenData = PopTokenData(token.publicKey)
                  val myBitmap =
                      QRCode.from(gson.toJson(tokenData))
                          .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
                          .bitmap()

                  binding.digitalCashReceiveQr.setImageBitmap(myBitmap)
                },
                { error: Throwable ->
                  logAndShow(requireContext(), TAG, error, R.string.error_retrieve_own_token)
                }))
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_receive)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(
            viewLifecycleOwner,
            buildBackButtonCallback(TAG, "digital cash home") {
              setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_home) {
                DigitalCashHomeFragment()
              }
            })
  }

  companion object {
    val TAG: String = DigitalCashReceiveFragment::class.java.simpleName

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashReceiveFragment.
     */
    fun newInstance(): DigitalCashReceiveFragment {
      return DigitalCashReceiveFragment()
    }
  }
}
