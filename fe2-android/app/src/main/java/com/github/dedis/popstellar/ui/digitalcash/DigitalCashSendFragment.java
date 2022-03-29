package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.dedis.popstellar.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment where you can send a coin
 */
@AndroidEntryPoint
public class DigitalCashSendFragment extends Fragment {
  private DigitalCashSendFragmentBinding mDigitalCashSendFragBinding;
  private DigitalCashViewModel mDigitalCashViewModel;

  public DigitalCashSendFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashSendFragment.
   */
  public static DigitalCashSendFragment newInstance() {
    DigitalCashSendFragment fragment = new DigitalCashSendFragment();
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.digital_cash_send_fragment, container, false);
  }
}
