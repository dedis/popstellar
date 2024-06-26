package com.github.dedis.popstellar.ui.lao.digitalcash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.SingleEvent
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.security.PublicKey.Companion.findPublicKeyFromUsername
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import io.reactivex.Completable
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Collections
import timber.log.Timber

/**
 * A simple [Fragment] subclass. Use the [DigitalCashSendFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DigitalCashSendFragment : Fragment() {

  private lateinit var binding: DigitalCashSendFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var digitalCashViewModel: DigitalCashViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = DigitalCashSendFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupSendCoinButton()

    digitalCashViewModel.getPostTransactionEvent().observe(viewLifecycleOwner) {
        booleanEvent: SingleEvent<Boolean> ->
      val event = booleanEvent.contentIfNotHandled
      if (event != null) {
        val currentAmount = binding.digitalCashSendAmount.text.toString()
        val currentPublicKeySelected =
            findPublicKeyFromUsername(binding.digitalCashSendSpinner.editText?.text.toString(), digitalCashViewModel.attendeesFromTheRollCallList, digitalCashViewModel.validToken.publicKey)

        if (digitalCashViewModel.canPerformTransaction(
            currentAmount, currentPublicKeySelected.encoded, -1)) {
          try {
            val token = digitalCashViewModel.validToken
            if (canPostTransaction(token.publicKey, currentAmount.toInt())) {
              laoViewModel.addDisposable(
                  postTransaction(
                          Collections.singletonMap(currentPublicKeySelected.encoded, currentAmount))
                      .subscribe(
                          {
                            digitalCashViewModel.updateReceiptAddressEvent(currentPublicKeySelected)
                            digitalCashViewModel.updateReceiptAmountEvent(currentAmount)
                            setCurrentFragment(
                                requireActivity().supportFragmentManager,
                                R.id.fragment_digital_cash_receipt) {
                                  DigitalCashReceiptFragment.newInstance()
                                }
                          },
                          { error: Throwable ->
                            Timber.tag(TAG).e(error, "error posting transaction")
                          }))
            }
          } catch (keyException: KeyException) {
            logAndShow(
                requireContext(), TAG, keyException, R.string.digital_cash_please_enter_a_lao)
          }
        }
      }
    }

    try {
      setUpTheAdapter()
    } catch (e: KeyException) {
      logAndShow(requireContext(), TAG, e, R.string.digital_cash_error_poptoken)
    }
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_send)
    laoViewModel.setIsTab(false)
  }

  private fun canPostTransaction(publicKey: PublicKey, amount: Int): Boolean {
    val currentBalance = digitalCashViewModel.getUserBalance(publicKey)

    return if (currentBalance < amount) {
      Timber.tag(TAG).d("Current Balance: %s amount: %s", currentBalance, amount)
      Toast.makeText(
              requireContext(), R.string.digital_cash_warning_not_enough_money, Toast.LENGTH_SHORT)
          .show()
      false
    } else {
      true
    }
  }

  /** Function that set up the Adapter for the dropdown selector menu (with the public key list) */
  @Throws(KeyException::class)
  private fun setUpTheAdapter() {
    /* Roll Call attendees to which we can send */
    var myArray: MutableList<String>
    try {
      myArray =
          digitalCashViewModel.attendeesFromTheRollCallList.map { it.getLabel() }.toMutableList()
    } catch (e: NoRollCallException) {
      Timber.tag(TAG).d(e)
      Toast.makeText(
              requireContext(), R.string.digital_cash_please_enter_roll_call, Toast.LENGTH_SHORT)
          .show()
      myArray = ArrayList()
      setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_home) {
        DigitalCashHomeFragment.newInstance()
      }
    }

    // Filter my pop token out: sending money to myself has no sense
    removeOwnToken(myArray)
    val adapter = ArrayAdapter(requireContext(), R.layout.list_item, myArray)

    // Display by default the first item in the list of tokens
    if (myArray.isNotEmpty()) {
      binding.digitalCashSendSpinner.editText?.setText(myArray[0])
    }

    binding.digitalCashSendSpinnerTv.setAdapter(adapter)
  }

  /**
   * Removes from the list of LAO members my pop token
   *
   * @param members list of tokens of the lao members
   */
  private fun removeOwnToken(members: MutableList<String>) {
    try {
      members.remove(digitalCashViewModel.validToken.publicKey.getLabel())
    } catch (e: KeyException) {
      Timber.tag(TAG).e(e, resources.getString(R.string.error_retrieve_own_token))
    }
  }

  /** Function that setup the Button */
  private fun setupSendCoinButton() {
    binding.digitalCashSendSend.setOnClickListener { digitalCashViewModel.postTransactionEvent() }
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param publicKeyAmount Map<String></String>, String> containing the Public Keys and the related
   *   amount to issue to
   */
  private fun postTransaction(publicKeyAmount: Map<String, String>): Completable {
    return digitalCashViewModel
        .postTransaction(publicKeyAmount, Instant.now().epochSecond, false)
        .doOnComplete {
          Toast.makeText(
                  requireContext(), R.string.digital_cash_post_transaction, Toast.LENGTH_SHORT)
              .show()
        }
        .doOnError { error: Throwable ->
          if (error is KeyException || error is GeneralSecurityException) {
            logAndShow(requireContext(), TAG, error, R.string.error_retrieve_own_token)
          } else {
            logAndShow(requireContext(), TAG, error, R.string.error_post_transaction)
          }
        }
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
    private val TAG = DigitalCashSendFragment::class.java.simpleName

    /**
     * Use this factory method to create a new instance of this fragment
     *
     * @return A new instance of fragment DigitalCashSendFragment.
     */
    fun newInstance(): DigitalCashSendFragment {
      return DigitalCashSendFragment()
    }
  }
}
