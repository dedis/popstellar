package com.github.dedis.popstellar.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoCreateFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
public final class LaoCreateFragment extends Fragment {

  public static final String TAG = LaoCreateFragment.class.getSimpleName();

  private LaoCreateFragmentBinding binding;
  private HomeViewModel viewModel;

  public static LaoCreateFragment newInstance() {
    return new LaoCreateFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = LaoCreateFragmentBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(getActivity());
    viewModel = HomeActivity.obtainViewModel(requireActivity());

    setupCreateButton();
    setupCancelButton();
    setupTextFieldWatcher();

    return binding.getRoot();
  }

  TextWatcher launchWatcher =
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
          String laoName =
              Objects.requireNonNull(binding.laoNameEntry.getEditText())
                  .getText()
                  .toString()
                  .trim();
          String serverUrl =
              Objects.requireNonNull(binding.serverUrlEntry.getEditText())
                  .getText()
                  .toString()
                  .trim();

          boolean areFieldsFilled = !laoName.isEmpty() && serverUrl.isEmpty();
          binding.buttonLaunch.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable editable) {
          // Do nothing
        }
      };

  private void setupTextFieldWatcher() {
    binding.serverUrlEntryEditText.addTextChangedListener(launchWatcher);
    binding.laoNameEntryEditText.addTextChangedListener(launchWatcher);
  }

  private void setupCreateButton() {
    binding.buttonLaunch.setOnClickListener(
        v -> {
          Context ctx = requireContext();
          viewModel.addDisposable(
              viewModel
                  .createLao(
                      Objects.requireNonNull(binding.laoNameEntryEditText.getText()).toString())
                  .subscribe(
                      laoId -> {
                        Log.d(TAG, "Opening lao detail activity on the home tab for lao " + laoId);
                        startActivity(LaoDetailActivity.newIntentForLao(ctx, laoId));
                      },
                      error -> ErrorUtils.logAndShow(ctx, TAG, error, R.string.error_create_lao)));
        });
  }

  private void setupCancelButton() {
    binding.buttonCancelLaunch.setOnClickListener(
        v -> {
          Objects.requireNonNull(binding.laoNameEntryEditText.getText()).clear();
          HomeActivity.setCurrentFragment(
              getParentFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);
        });
  }
}
