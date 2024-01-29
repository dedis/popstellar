package com.github.dedis.popstellar.ui.lao.witness

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainWitnessingViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.google.android.material.floatingactionbutton.FloatingActionButton

class WitnessesFragment : Fragment() {

  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    val view = inflater.inflate(R.layout.witnesses_fragment, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val witnessingViewModel = obtainWitnessingViewModel(requireActivity(), laoViewModel.laoId)

    val fab = view.findViewById<FloatingActionButton>(R.id.add_witness_button)
    fab.setOnClickListener { openAddWitness() }

    val recyclerView = view.findViewById<RecyclerView>(R.id.witness_list)
    val adapter = WitnessListAdapter(witnessingViewModel.witnesses.value)
    val layoutManager = LinearLayoutManager(context)
    recyclerView.layoutManager = layoutManager

    val itemDecoration = DividerItemDecoration(requireContext(), layoutManager.orientation)
    recyclerView.layoutManager = layoutManager
    recyclerView.addItemDecoration(itemDecoration)
    recyclerView.adapter = adapter

    witnessingViewModel.witnesses.observe(viewLifecycleOwner) { witnesses: List<PublicKey>? ->
      adapter.replaceList(witnesses)
    }

    return view
  }

  private fun openAddWitness() {
    laoViewModel.setIsTab(false)
    setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
      newInstance(ScanningAction.ADD_WITNESS)
    }
  }

  companion object {
    fun newInstance(): WitnessesFragment {
      return WitnessesFragment()
    }
  }
}
