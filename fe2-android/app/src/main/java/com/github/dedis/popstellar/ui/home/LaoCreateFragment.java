package com.github.dedis.popstellar.ui.home;

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
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment used to display the Launch UI */
@AndroidEntryPoint
public final class LaoCreateFragment extends Fragment {

  public static final String TAG = LaoCreateFragment.class.getSimpleName();

  @Inject GlobalNetworkManager networkManager;

  private HomeViewModel viewModel;
  private LaoCreateFragmentBinding binding;
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
    initialUrl = networkManager.getCurrentUrl();
    viewModel = HomeActivity.obtainViewModel(requireActivity());

    setupCancelButton();
    setupTextFields();
    setupCreateButton();

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.lao_create_title);
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
        v -> {
          String serverAddress =
              Objects.requireNonNull(binding.serverUrlEntryEditText.getText()).toString();
          String laoName =
              Objects.requireNonNull(binding.laoNameEntryEditText.getText()).toString();
          Log.d(TAG, "creating lao with name " + laoName);

          networkManager.connect(serverAddress);
          requireActivity()
              .startActivity(
                  ConnectingActivity.newIntentForCreatingDetail(requireContext(), laoName));
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
