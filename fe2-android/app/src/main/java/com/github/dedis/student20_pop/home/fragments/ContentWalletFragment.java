package com.github.dedis.student20_pop.home.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Wallet;

public class ContentWalletFragment extends Fragment {
  public static final String TAG = ContentWalletFragment.class.getSimpleName();
  public Wallet wallet;
  public static ContentWalletFragment newInstance() {
    return new ContentWalletFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    wallet = Wallet.getInstance();
    View view =  inflater.inflate(R.layout.fragment_content_wallet, container, false);
    return view;
  }
  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }
  
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }
}
