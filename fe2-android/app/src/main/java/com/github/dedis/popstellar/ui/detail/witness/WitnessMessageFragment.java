package com.github.dedis.popstellar.ui.detail.witness;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WitnessMessageFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.ArrayList;

public class WitnessMessageFragment extends Fragment {

  public static final String TAG = WitnessMessageFragment.class.getSimpleName();
  private WitnessMessageFragmentBinding mWitnessMessageFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;
  private WitnessMessageListViewAdapter mWitnessMessageListViewAdapter;

  public static WitnessMessageFragment newInstance() {
    return new WitnessMessageFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mWitnessMessageFragBinding = WitnessMessageFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    mWitnessMessageFragBinding.setViewmodel(mLaoDetailViewModel);
    mWitnessMessageFragBinding.setLifecycleOwner(requireActivity());

    return mWitnessMessageFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setupListAdapter();
    setupListUpdates();
    Button back = requireActivity().findViewById(R.id.tab_back);
    back.setOnClickListener(c -> mLaoDetailViewModel.openLaoDetail());
  }

  private void setupListAdapter() {
    ListView listView = mWitnessMessageFragBinding.witnessMessageList;

    mWitnessMessageListViewAdapter =
        new WitnessMessageListViewAdapter(new ArrayList<>(), mLaoDetailViewModel, getActivity());

    listView.setAdapter(mWitnessMessageListViewAdapter);
  }

  private void setupListUpdates() {
    mLaoDetailViewModel
        .getWitnessMessages()
        .observe(
            requireActivity(),
            messages -> {
              Log.d(TAG, "witness messages updated");
              mWitnessMessageListViewAdapter.replaceList(messages);
            });
  }
}
