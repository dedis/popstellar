package com.github.dedis.popstellar.ui.lao.digitalcash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainDigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber

class DigitalCashHistoryFragment : Fragment() {

  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    val view = inflater.inflate(R.layout.digital_cash_history_fragment, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val digitalCashViewModel = obtainDigitalCashViewModel(requireActivity(), laoViewModel.laoId!!)

    val transactionList = view.findViewById<RecyclerView>(R.id.transaction_history_list)

    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
    val decoration: ItemDecoration =
        DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
    val adapter = HistoryListAdapter(digitalCashViewModel, requireActivity())

    transactionList.layoutManager = layoutManager
    transactionList.addItemDecoration(decoration)
    transactionList.adapter = adapter

    // Update dynamically the events in History
    laoViewModel.addDisposable(
        digitalCashViewModel.transactionsObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { transactions: List<TransactionObject> -> adapter.setList(transactions) },
                { error: Throwable -> Timber.tag(TAG).e(error, "error with history update") }))

    handleBackNav()

    return view
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.digital_cash_history)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "last fragment") {
          (requireActivity() as LaoActivity).resetLastFragment()
        })
  }

  companion object {
    private val TAG = DigitalCashHistoryFragment::class.java.simpleName

    fun newInstance(): DigitalCashHistoryFragment {
      return DigitalCashHistoryFragment()
    }
  }
}
