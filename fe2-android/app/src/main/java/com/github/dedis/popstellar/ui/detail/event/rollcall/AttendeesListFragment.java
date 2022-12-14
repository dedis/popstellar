package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.os.Bundle;
import android.view.*;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.AttendeesListFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AttendeesListFragment extends Fragment {

  public static final String TAG = AttendeesListFragment.class.getSimpleName();
  public static final String EXTRA_ID = "id";

  private AttendeesListFragmentBinding mAttendeesListBinding;
  private RollCall rollCall;

  public static AttendeesListFragment newInstance(String id) {
    AttendeesListFragment attendeesListFragment = new AttendeesListFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(EXTRA_ID, id);
    attendeesListFragment.setArguments(bundle);
    return attendeesListFragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mAttendeesListBinding = AttendeesListFragmentBinding.inflate(inflater, container, false);

    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    String id = requireArguments().getString(EXTRA_ID);

    RollCall rollCall = null;
    try {
      rollCall = viewModel.getRollCall(id);
    } catch (UnknownRollCallException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.no_rollcall_exception);
      LaoDetailActivity.setCurrentFragment(
          getParentFragmentManager(), R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
    }

    mAttendeesListBinding.rollcallName.setText("Roll Call: " + rollCall.getName());
    mAttendeesListBinding.setLifecycleOwner(getActivity());

    return mAttendeesListBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupAttendeesListAdapter();

    mAttendeesListBinding.backButton.setOnClickListener(
        clicked ->
            LaoDetailActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_lao_wallet,
                LaoWalletFragment::newInstance));
  }

  private void setupAttendeesListAdapter() {
    ListView listView = mAttendeesListBinding.attendeesList;

    AttendeesListAdapter mAttendeesListAdapter =
        new AttendeesListAdapter(new ArrayList<>(rollCall.getAttendees()), getActivity());
    listView.setAdapter(mAttendeesListAdapter);
  }
}
