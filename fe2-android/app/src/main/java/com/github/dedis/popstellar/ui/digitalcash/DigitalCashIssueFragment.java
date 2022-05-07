package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashIssueFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashIssueFragment extends Fragment {

  public DigitalCashIssueFragment() {}

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
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.digital_cash_issue_fragment, container, false);
  }
}
