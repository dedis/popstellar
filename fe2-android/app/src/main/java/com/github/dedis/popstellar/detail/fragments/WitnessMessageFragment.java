package com.github.dedis.popstellar.detail.fragments;

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
import com.github.dedis.popstellar.databinding.FragmentWitnessMessageBinding;
import com.github.dedis.popstellar.detail.LaoDetailActivity;
import com.github.dedis.popstellar.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.detail.adapters.WitnessMessageListViewAdapter;
import java.util.ArrayList;

public class WitnessMessageFragment extends Fragment {

  public static final String TAG = WitnessMessageFragment.class.getSimpleName();
  private FragmentWitnessMessageBinding mWitnessMessageFragBinding;
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
    mWitnessMessageFragBinding = FragmentWitnessMessageBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    mWitnessMessageFragBinding.setViewmodel(mLaoDetailViewModel);
    mWitnessMessageFragBinding.setLifecycleOwner(getActivity());

    return mWitnessMessageFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setupListAdapter();
    setupListUpdates();
    Button back = (Button) getActivity().findViewById(R.id.tab_back);
    back.setOnClickListener(c ->
        mLaoDetailViewModel.openLaoDetail());
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
            getActivity(),
            messages -> {
              Log.d(TAG, "witness messages updated");
              mWitnessMessageListViewAdapter.replaceList(messages);
            });
  }

}
