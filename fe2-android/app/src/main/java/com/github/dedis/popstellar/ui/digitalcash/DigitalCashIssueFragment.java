package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashIssueFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashIssueFragment extends Fragment {
  private DigitalCashIssueFragmentBinding mBinding;
  private DigitalCashViewModel mViewModel;

  public DigitalCashIssueFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashIssueFragment.
   */
  public static DigitalCashIssueFragment newInstance() {
    return new DigitalCashIssueFragment();
  }


  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.mViewModel = DigitalCashMain.obtainViewModel(getActivity());
    mBinding = DigitalCashIssueFragmentBinding.inflate(inflater, container, false);
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendCoinButton();

    mViewModel.getpostTransactionEvent().observe(
         getViewLifecycleOwner(),
         booleanEvent -> {
           Boolean event = booleanEvent.getContentIfNotHandled();
           if (event != null){
             postTransaction();
           }
         }
    );

    List<String> myArray =
            Collections.singletonList(
            mViewModel
                .getCurrentLao().getOrganizer().getEncoded());
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashIssueSpinnerTv.setAdapter(adapter);
  }

  private void setupSendCoinButton(){
    mBinding.digitalCashIssueIssue.setOnClickListener(
            v -> mViewModel.postTransactionEvent()
    );
  }

  ///** Function that permits to post transaction */
  private void postTransaction() {
    long amount = Long.getLong(mBinding.digitalCashIssueAmount.getText().toString());
    mViewModel.postTransaction(Collections.singletonMap(mViewModel.getCurrentLao().getOrganizer(),amount), Instant.now().getEpochSecond());
  }
}
