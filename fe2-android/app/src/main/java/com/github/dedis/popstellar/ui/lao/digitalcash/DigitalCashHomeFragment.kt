package com.github.dedis.popstellar.ui.lao.digitalcash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.DigitalCashHomeFragmentBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

/**
 * A simple [Fragment] subclass. Use the [DigitalCashHomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DigitalCashHomeFragment : Fragment() {
  private lateinit var binding: DigitalCashHomeFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var digitalCashViewModel: DigitalCashViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = DigitalCashHomeFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    subscribeToTransactions()
    subscribeToRole()
    setupReceiveButton()
    setupSendButton()
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_home)
    laoViewModel.setIsTab(true)
  }

  private fun subscribeToTransactions() {
    laoViewModel.addDisposable(
        digitalCashViewModel.transactionsObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { transactions: List<TransactionObject> ->
                  Timber.tag(TAG).d("updating transactions %s", transactions)
                  val totalAmount = digitalCashViewModel.ownBalance
                  binding.coinAmountText.text = totalAmount.toString()
                },
                { error: Throwable ->
                  logAndShow(requireContext(), TAG, error, R.string.error_retrieve_own_token)
                }))
  }

  private fun setupReceiveButton() {
    val receiveListener =
        View.OnClickListener {
          setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_receive) {
            DigitalCashReceiveFragment.newInstance()
          }
        }

    binding.digitalCashReceiveButton.setOnClickListener(receiveListener)
    binding.digitalCashReceiveText.setOnClickListener(receiveListener)
  }

  private fun setupSendButton() {
    val sendListener =
        View.OnClickListener {
          setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_send) {
            DigitalCashSendFragment.newInstance()
          }
        }

    binding.digitalCashSendButton.setOnClickListener(sendListener)
    binding.digitalCashSendText.setOnClickListener(sendListener)
  }

  private fun subscribeToRole() {
    laoViewModel.role.observe(viewLifecycleOwner) { role: Role ->
      if (role === Role.ORGANIZER) {
        binding.issueButton.visibility = View.VISIBLE
        binding.issueButton.setOnClickListener {
          setCurrentFragment(parentFragmentManager, R.id.fragment_digital_cash_issue) {
            DigitalCashIssueFragment.newInstance()
          }
        }
      } else {
        binding.issueButton.visibility = View.GONE
      }
    }
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    /**
     * Use this factory method to create a new instance of this fragment
     *
     * @return A new instance of fragment DigitalCashHomeFragment.
     */
    fun newInstance(): DigitalCashHomeFragment {
      return DigitalCashHomeFragment()
    }

    val TAG: String = DigitalCashHomeFragment::class.java.simpleName

    fun openFragment(manager: FragmentManager) {
      setCurrentFragment(manager, R.id.fragment_digital_cash_home) { DigitalCashHomeFragment() }
    }
  }
}
