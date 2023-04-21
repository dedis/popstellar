package com.github.dedis.popstellar.ui.lao.token;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.TokenListFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallViewModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;

@AndroidEntryPoint
public class TokenListFragment extends Fragment {

  private static final Logger logger = LogManager.getLogger(TokenListFragment.class);

  private TokenListFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private RollCallViewModel rollCallViewModel;
  private TokenListAdapter tokensAdapter;

  @Inject RollCallRepository rollCallRepo;

  public static TokenListFragment newInstance() {
    return new TokenListFragment();
  }

  public TokenListFragment() {
    // Required empty public constructor
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.tokens);
    laoViewModel.setIsTab(true);
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = TokenListFragmentBinding.inflate(inflater, container, false);
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    rollCallViewModel =
        LaoActivity.obtainRollCallViewModel(requireActivity(), laoViewModel.getLaoId());

    tokensAdapter = new TokenListAdapter(requireActivity());
    binding.tokensRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    binding.tokensRecyclerView.setAdapter(tokensAdapter);

    subscribeToAttendedRollCalls();
    handleBackNav();
    return binding.getRoot();
  }

  private void subscribeToAttendedRollCalls() {
    laoViewModel.addDisposable(
        rollCallViewModel
            .getAttendedRollCalls()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                attendedRollCalls -> {
                  RollCall lastRollCall =
                      rollCallRepo.getLastClosedRollCall(laoViewModel.getLaoId());

                  if (attendedRollCalls.contains(lastRollCall)) {
                    // We attended the last roll call
                    TextView validRcTitle =
                        binding.validTokenLayout.findViewById(R.id.token_layout_rc_title);
                    validRcTitle.setText(lastRollCall.getName());
                    binding.validTokenLayout.setVisibility(View.VISIBLE);
                    binding.validTokenCard.setOnClickListener(
                        v ->
                            LaoActivity.setCurrentFragment(
                                requireActivity().getSupportFragmentManager(),
                                R.id.fragment_token,
                                () -> TokenFragment.newInstance(lastRollCall.getPersistentId())));
                  } else {
                    binding.validTokenLayout.setVisibility(View.GONE);
                  }

                  // This handle the previous tokens list
                  // First we remove the last roll call from the list of attended roll calls
                  List<RollCall> previousRollCalls = new ArrayList<>(attendedRollCalls);
                  previousRollCalls.remove(lastRollCall);
                  if (previousRollCalls.isEmpty()) {
                    binding.previousTokenLayout.setVisibility(View.GONE);
                  } else {
                    binding.previousTokenLayout.setVisibility(View.VISIBLE);
                    tokensAdapter.replaceList(previousRollCalls);
                  }
                  binding.emptyTokenLayout.setVisibility(View.GONE);
                },
                error -> {
                  // In case of error, such as when no closed rc exists, we display an explanatory
                  // message to the user
                  binding.emptyTokenLayout.setVisibility(View.VISIBLE);
                  binding.validTokenLayout.setVisibility(View.GONE);
                  binding.previousTokenLayout.setVisibility(View.GONE);
                }));
  }

  public static void openFragment(FragmentManager manager) {
    LaoActivity.setCurrentFragment(manager, R.id.fragment_tokens, TokenListFragment::new);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(
        requireActivity(), getViewLifecycleOwner(), logger);
  }
}
