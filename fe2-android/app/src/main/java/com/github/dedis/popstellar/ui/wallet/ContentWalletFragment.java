package com.github.dedis.popstellar.ui.wallet;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.WalletContentFragmentBinding;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.ui.home.*;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the content wallet UI */
@AndroidEntryPoint
public class ContentWalletFragment extends Fragment {

  public static final String TAG = ContentWalletFragment.class.getSimpleName();

  public static ContentWalletFragment newInstance() {
    return new ContentWalletFragment();
  }

  @Inject Wallet wallet;
  private WalletContentFragmentBinding mWalletContentBinding;
  private HomeViewModel mHomeViewModel;
  private LAOListAdapter mListAdapter;
  private AlertDialog logoutAlert;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mWalletContentBinding = WalletContentFragmentBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(requireActivity());

    mWalletContentBinding.setViewmodel(mHomeViewModel);
    mWalletContentBinding.setLifecycleOwner(requireActivity());

    return mWalletContentBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupListAdapter();
    setupListUpdates();

    if (wallet.isSetUp()) {
      mWalletContentBinding.logoutButton.setVisibility(View.VISIBLE);
      mWalletContentBinding.logoutButton.setOnClickListener(
          clicked -> {
            if (logoutAlert != null && logoutAlert.isShowing()) {
              logoutAlert.dismiss();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.logout_title);
            builder.setMessage(R.string.logout_message);
            builder.setPositiveButton(
                R.string.confirm, (dialog, which) -> mHomeViewModel.logoutWallet());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            logoutAlert = builder.create();
            logoutAlert.show();
          });
    }
  }

  private void setupListUpdates() {
    mHomeViewModel
        .getLAOs()
        .observe(
            requireActivity(),
            laos -> {
              Log.d(TAG, "Got a list update");

              mListAdapter.replaceList(laos);

              if (!laos.isEmpty()) {
                mWalletContentBinding.welcomeScreen.setVisibility(View.GONE);
                mWalletContentBinding.listScreen.setVisibility(View.VISIBLE);
              }
            });
  }

  private void setupListAdapter() {
    RecyclerView recyclerView = mWalletContentBinding.laoList;

    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    mListAdapter = new LAOListAdapter(new ArrayList<>(0), mHomeViewModel, false);

    recyclerView.setAdapter(mListAdapter);
  }
}
