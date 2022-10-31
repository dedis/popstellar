package com.github.dedis.popstellar.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoCreateFragmentBinding;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.Constants;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
public final class LaoCreateFragment extends Fragment {

  public static final String TAG = LaoCreateFragment.class.getSimpleName();

  @Inject GlobalNetworkManager globalNetworkManager;

  private LaoCreateFragmentBinding binding;
  private HomeViewModel viewModel;
  private String initialUrl;

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
    initialUrl = globalNetworkManager.getCurrentUrl();

    setupCancelButton();
    setupTextFields();
    setupCreateButton();

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

          boolean areFieldsFilled = !laoName.isEmpty() && !serverUrl.isEmpty();
          binding.buttonCreate.setEnabled(areFieldsFilled);
        }

        @Override
        public void afterTextChanged(Editable editable) {
          // Do nothing
        }
      };

  private void setupTextFields() {
    binding.serverUrlEntryEditText.setText(initialUrl);
    binding.serverUrlEntryEditText.addTextChangedListener(launchWatcher);
    binding.laoNameEntryEditText.addTextChangedListener(launchWatcher);
  }

  private void setupCreateButton() {
    binding.buttonCreate.setOnClickListener(
        v ->
            viewModel.createLao(
                Objects.requireNonNull(binding.laoNameEntryEditText.getText()).toString(),
                Objects.requireNonNull(binding.serverUrlEntryEditText.getText()).toString()));
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
