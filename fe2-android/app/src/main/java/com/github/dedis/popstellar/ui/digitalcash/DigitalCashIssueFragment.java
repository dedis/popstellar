package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.rollcall.AttendeesListAdapter;
import com.github.dedis.popstellar.ui.detail.event.rollcall.AttendeesListFragment;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where we issue money when we are the organiser */
@AndroidEntryPoint
public class DigitalCashIssueFragment extends Fragment {
  public static final String TAG = DigitalCashIssueFragment.class.getSimpleName();
  public static final String EXTRA_ID = "id";

  private DigitalCashIssueFragmentBinding mdigitalCashIssueFragmentBinding;
  private DigitalCashViewModel mdigitalCashViewModel;
  private LaoDetailViewModel mLaoDetailViewModel;
  private AttendeesListAdapter mAttendeesListAdapter;

  public static DigitalCashIssueFragment newInstance() {
    DigitalCashIssueFragment digitalCashIssueFragment = new DigitalCashIssueFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(EXTRA_ID,"0");
    digitalCashIssueFragment.setArguments(bundle);
    return digitalCashIssueFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Spinner spin = (Spinner)findViewById(R.id.digital_cash_issue_user);
    // spin.setOnItemSelectedListener(this);
  }

  // @Override
  // public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
  // Toast.makeText(getApplicationContext(),country[position] , Toast.LENGTH_LONG).show();
  // }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mdigitalCashIssueFragmentBinding =
        DigitalCashIssueFragmentBinding.inflate(inflater, container, false);
    mdigitalCashViewModel = DigitalCashMain.obtainViewModel(requireActivity());

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    mdigitalCashIssueFragmentBinding.setViewModel(mdigitalCashViewModel);
    mdigitalCashIssueFragmentBinding.setLifecycleOwner(getViewLifecycleOwner());
    return mdigitalCashIssueFragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSpinner();
    // Set up the spinner

  }

  private void setupSpinner() {
    RollCall rollCall = mLaoDetailViewModel.getCurrentLaoValue().getRollCalls().values().stream().max(Comparator.comparing(RollCall::getEnd));
    mAttendeesListAdapter =
        new AttendeesListAdapter(new ArrayList<>(rollCall.getAttendees()), getActivity());
    mdigitalCashIssueFragmentBinding.digitalCashIssueUser.setAdapter(mAttendeesListAdapter);
  }
}
