package com.github.dedis.popstellar.ui.lao.witness;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WitnessMessageFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class WitnessMessageFragment extends Fragment {

  public static final String TAG = WitnessMessageFragment.class.getSimpleName();
  private WitnessMessageFragmentBinding binding;
  private WitnessingViewModel witnessingViewModel;
  private WitnessMessageListViewAdapter adapter;

  public static WitnessMessageFragment newInstance() {
    return new WitnessMessageFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = WitnessMessageFragmentBinding.inflate(inflater, container, false);

    LaoViewModel viewModel = LaoActivity.obtainViewModel(requireActivity());
    witnessingViewModel =
        LaoActivity.obtainWitnessingViewModel(requireActivity(), viewModel.getLaoId());
    binding.setLifecycleOwner(getActivity());
    setupListAdapter();
    setupListUpdates();
    setupDeleteButton();
    return binding.getRoot();
  }

  private void setupListAdapter() {
    ListView listView = binding.witnessMessageList;

    adapter = new WitnessMessageListViewAdapter(new ArrayList<>(), getActivity());
    listView.setAdapter(adapter);
  }

  private void setupListUpdates() {
    witnessingViewModel
        .getWitnessMessages()
        .observe(
            requireActivity(),
            messages -> {
              Timber.tag(TAG).d("witness messages updated");
              adapter.replaceList(messages);
            });
  }

  private void setupDeleteButton() {
    binding.witnessDeleteSignedMessage.setOnClickListener(
        v ->
            new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.confirm_delete_witnessed_messages)
                .setPositiveButton(
                    R.string.yes, (dialogInterface, i) -> adapter.deleteSignedMessages())
                .setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                .show());
  }
}
