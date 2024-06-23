package com.github.dedis.popstellar.ui.lao.digitalcash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import dagger.hilt.android.AndroidEntryPoint
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.Collections
import timber.log.Timber

/**
 * A simple [Fragment] subclass. Use the [DigitalCashIssueFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class DigitalCashIssueFragment : Fragment() {

  private lateinit var binding: DigitalCashIssueFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var digitalCashViewModel: DigitalCashViewModel

  private var selectOneMember = 0
  private var selectAllRollCallAttendees = 0
  private var selectAllLaoWitnesses = 0

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = DigitalCashIssueFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    selectOneMember = binding.radioButton.id
    selectAllRollCallAttendees = binding.radioButtonAttendees.id
    selectAllLaoWitnesses = binding.radioButtonWitnesses.id

    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupSendCoinButton()
    setTheAdapterRollCallAttendee()
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_issue)
    laoViewModel.setIsTab(false)
  }

  /** Function which call the view model post transaction when a post transaction event occur */
  private fun issueCoins() {
    /*Take the amount entered by the user*/
    val currentAmount = binding.digitalCashIssueAmount.text.toString()
    val currentPublicKeySelected =
        getPublicKeyFromUsername(binding.digitalCashIssueSpinner.editText!!.text.toString())
    val radioGroup = binding.digitalCashIssueSelect.checkedRadioButtonId

    if (digitalCashViewModel.canPerformTransaction(
        currentAmount, currentPublicKeySelected.encoded, radioGroup)) {
      try {
        val issueMap =
            computeMapForPostTransaction(
                currentAmount, currentPublicKeySelected.encoded, radioGroup)
        if (issueMap.isEmpty()) {
          displayToast(radioGroup)
        } else {
          postTransaction(issueMap)
        }
      } catch (r: NoRollCallException) {
        logAndShow(requireContext(), TAG, r, R.string.no_rollcall_exception)
      }
    }
  }

  private fun getPublicKeyFromUsername(username: String): PublicKey {
    return digitalCashViewModel.attendeesFromTheRollCallList.firstOrNull { it.getUsername() == username }
      ?: digitalCashViewModel.validToken.publicKey
  }

  private fun displayToast(radioGroup: Int) {
    if (radioGroup == selectAllLaoWitnesses) {
      Toast.makeText(requireContext(), R.string.digital_cash_no_witness, Toast.LENGTH_LONG).show()
    } else {
      Toast.makeText(requireContext(), R.string.digital_cash_no_attendees, Toast.LENGTH_LONG).show()
    }
  }

  @Throws(NoRollCallException::class)
  fun computeMapForPostTransaction(
      currentAmount: String,
      currentPublicKeySelected: String,
      radioGroup: Int
  ): Map<String, String> {
    return if (radioGroup == DigitalCashViewModel.NOTHING_SELECTED) {
      // In unlikely event that no radiobutton are selected, it do as if the first one was selected
      Collections.singletonMap(currentPublicKeySelected, currentAmount)
    } else {
      val attendees = attendeesPerRadioGroupButton(radioGroup, currentPublicKeySelected)
      val issueMap: MutableMap<String, String> = HashMap()
      if (attendees.isNotEmpty()) {
        for (publicKey in attendees) {
          issueMap.putIfAbsent(publicKey.encoded, currentAmount)
        }
      }

      issueMap
    }
  }

  /**
   * Function that return the give list of attendees Radio Group Button selected (a empty list if
   * nothing)
   */
  @Throws(NoRollCallException::class)
  private fun attendeesPerRadioGroupButton(
      radioGroup: Int,
      currentSelected: String
  ): Set<PublicKey> {
    val attendees: Set<PublicKey> =
        when {
          radioGroup == selectOneMember && currentSelected != "" -> {
            setOf(PublicKey(currentSelected))
          }
          radioGroup == selectAllRollCallAttendees -> {
            digitalCashViewModel.attendeesFromLastRollCall
          }
          radioGroup == selectAllLaoWitnesses -> {
            // Currently the button is not visible, as we only have the public keys of the
            // witnesses,
            // not
            // their pop tokens
            digitalCashViewModel.witnesses
          }
          else -> {
            emptySet()
          }
        }

    return attendees
  }

  /** Function that setup the Button */
  private fun setupSendCoinButton() {
    binding.digitalCashIssueIssue.setOnClickListener { issueCoins() }
  }

  /** Function that set the Adapter */
  private fun setTheAdapterRollCallAttendee() {
    /* Roll Call attendees to which we can send*/
    var myArray: List<String>
    try {
      myArray = digitalCashViewModel.attendeesFromTheRollCallList.map { it.getUsername() }
    } catch (e: NoRollCallException) {
      Timber.tag(TAG).e(getString(R.string.error_no_rollcall_closed_in_LAO))
      Toast.makeText(
              requireContext(),
              getString(R.string.digital_cash_please_enter_roll_call),
              Toast.LENGTH_SHORT)
          .show()

      myArray = ArrayList()
      setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_home) {
        DigitalCashHomeFragment.newInstance()
      }
    }

    val adapter = ArrayAdapter(requireContext(), R.layout.list_item, myArray)
    binding.digitalCashIssueSpinnerTv.setAdapter(adapter)
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param publicKeyAmount Map<String></String>, String> containing the Public Keys and the related
   *   amount to issue to
   */
  private fun postTransaction(publicKeyAmount: Map<String, String>) {
    laoViewModel.addDisposable(
        digitalCashViewModel
            .postTransaction(publicKeyAmount, Instant.now().epochSecond, true)
            .subscribe({
              Toast.makeText(
                      requireContext(), R.string.digital_cash_post_transaction, Toast.LENGTH_LONG)
                  .show()
              setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_home) {
                DigitalCashHomeFragment.newInstance()
              }
            }) { error: Throwable ->
              if (error is KeyException || error is GeneralSecurityException) {
                logAndShow(requireContext(), TAG, error, R.string.error_retrieve_own_token)
              } else {
                logAndShow(requireContext(), TAG, error, R.string.error_post_transaction)
              }
            })
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
    val TAG: String = DigitalCashIssueFragment::class.java.simpleName

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashIssueFragment.
     */
    fun newInstance(): DigitalCashIssueFragment {
      return DigitalCashIssueFragment()
    }
  }
}
