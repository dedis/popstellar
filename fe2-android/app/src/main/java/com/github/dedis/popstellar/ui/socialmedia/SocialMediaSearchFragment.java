package com.github.dedis.popstellar.ui.socialmedia;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaSearchFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment that let us search for chirps and users */
@AndroidEntryPoint
public class SocialMediaSearchFragment extends Fragment {

  private LaoViewModel viewModel;

  public static SocialMediaSearchFragment newInstance() {
    return new SocialMediaSearchFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    SocialMediaSearchFragmentBinding binding;

    binding = SocialMediaSearchFragmentBinding.inflate(inflater, container, false);

    viewModel = LaoActivity.obtainViewModel(requireActivity());
    SocialMediaViewModel socialMediaViewModel =
        LaoActivity.obtainSocialMediaViewModel(requireActivity(), viewModel.getLaoId());

    binding.setViewModel(socialMediaViewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.search);
    viewModel.setIsTab(true);
  }
}
