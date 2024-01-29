package com.github.dedis.popstellar.ui.lao.token

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.TokenListFragmentBinding
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainRollCallViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class TokenListFragment : Fragment() {
  private lateinit var binding: TokenListFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var rollCallViewModel: RollCallViewModel
  private lateinit var tokensAdapter: TokenListAdapter

  @Inject lateinit var rollCallRepo: RollCallRepository

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.tokens)
    laoViewModel.setIsTab(true)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = TokenListFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    rollCallViewModel = obtainRollCallViewModel(requireActivity(), laoViewModel.laoId)
    tokensAdapter = TokenListAdapter(requireActivity())

    binding.tokensRecyclerView.layoutManager = LinearLayoutManager(context)
    binding.tokensRecyclerView.adapter = tokensAdapter

    subscribeToAttendedRollCalls()
    handleBackNav()

    return binding.root
  }

  private fun subscribeToAttendedRollCalls() {
    laoViewModel.addDisposable(
        rollCallViewModel.attendedRollCalls
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { attendedRollCalls: List<RollCall> ->
                  val lastRollCall = rollCallRepo.getLastClosedRollCall(laoViewModel.laoId!!)

                  if (attendedRollCalls.contains(lastRollCall)) {
                    // We attended the last roll call
                    val validRcTitle =
                        binding.validTokenLayout.findViewById<TextView>(R.id.token_layout_rc_title)
                    validRcTitle.text = lastRollCall.name

                    binding.validTokenLayout.visibility = View.VISIBLE
                    binding.validTokenCard.setOnClickListener {
                      setCurrentFragment(
                          requireActivity().supportFragmentManager, R.id.fragment_token) {
                            TokenFragment.newInstance(lastRollCall.persistentId)
                          }
                    }
                  } else {
                    binding.validTokenLayout.visibility = View.GONE
                  }

                  // This handle the previous tokens list
                  // First we remove the last roll call from the list of attended roll calls
                  val previousRollCalls: MutableList<RollCall> = ArrayList(attendedRollCalls)
                  previousRollCalls.remove(lastRollCall)
                  if (previousRollCalls.isEmpty()) {
                    binding.previousTokenLayout.visibility = View.GONE
                  } else {
                    binding.previousTokenLayout.visibility = View.VISIBLE
                    tokensAdapter.replaceList(previousRollCalls)
                  }
                  binding.emptyTokenLayout.visibility = View.GONE
                },
                { error: Throwable ->
                  // In case of error, such as when no closed rc exists, we display an explanatory
                  // message to the user
                  Timber.tag(TAG).e(error, "Impossible to get attended roll calls")
                  binding.emptyTokenLayout.visibility = View.VISIBLE
                  binding.validTokenLayout.visibility = View.GONE
                  binding.previousTokenLayout.visibility = View.GONE
                }))
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = TokenListFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): TokenListFragment {
      return TokenListFragment()
    }

    fun openFragment(manager: FragmentManager) {
      setCurrentFragment(manager, R.id.fragment_tokens) { TokenListFragment() }
    }
  }
}
