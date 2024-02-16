package com.github.dedis.popstellar.ui.home.wallet

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.WalletSeedFragmentBinding
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.ui.home.HomeActivity
import com.github.dedis.popstellar.ui.home.HomeFragment
import com.github.dedis.popstellar.ui.home.HomeViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException
import dagger.hilt.android.AndroidEntryPoint
import java.security.GeneralSecurityException
import javax.inject.Inject
import timber.log.Timber

/** Fragment used to display the new seed UI */
@AndroidEntryPoint
class SeedWalletFragment : Fragment() {
  private lateinit var binding: WalletSeedFragmentBinding
  private lateinit var viewModel: HomeViewModel
  private var seedAlert: AlertDialog? = null

  @Inject lateinit var wallet: Wallet

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = WalletSeedFragmentBinding.inflate(inflater, container, false)

    val activity = activity as HomeActivity
    viewModel = HomeActivity.obtainViewModel(activity)
    binding.lifecycleOwner = activity

    handleBackNav()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.seedWalletText.text = wallet.newSeed()

    setupConfirmSeedButton()
    setupImportPart()
  }

  override fun onResume() {
    super.onResume()
    viewModel.setPageTitle(R.string.wallet_setup)
    viewModel.setIsHome(false)
  }

  private fun setupConfirmSeedButton() {
    binding.buttonConfirmSeed.setOnClickListener {
      if (seedAlert?.isShowing == true) {
        seedAlert?.dismiss()
      }

      val builder = AlertDialog.Builder(requireActivity())
      builder.setTitle(R.string.wallet_confirm_text)
      builder.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
        try {
          viewModel.importSeed(binding.seedWalletText.text.toString())

          HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_home) {
            HomeFragment.newInstance()
          }
        } catch (e: Exception) {
          when (e) {
            is GeneralSecurityException,
            is SeedValidationException -> {
              Timber.tag(TAG).e(e, "Error importing key")
              Toast.makeText(
                      requireContext().applicationContext,
                      String.format(resources.getString(R.string.error_importing_key), e.message),
                      Toast.LENGTH_LONG)
                  .show()
            }
            else -> throw e
          }
        }
      }
      builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
        dialog.cancel()
      }

      seedAlert = builder.create()
      seedAlert?.show()
    }
  }

  private fun setupImportPart() {
    val importSeedWatcher: TextWatcher =
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // Do nothing
          }

          override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            binding.importSeedButton.isEnabled = s.toString().isNotEmpty()
          }

          override fun afterTextChanged(s: Editable) {
            // Do nothing
          }
        }
    binding.importSeedEntryEditText.addTextChangedListener(importSeedWatcher)
    binding.importSeedButton.setOnClickListener {
      try {
        viewModel.importSeed(binding.importSeedEntryEditText.text!!.toString())
      } catch (e: Exception) {
        when (e) {
          is GeneralSecurityException,
          is SeedValidationException -> {
            logAndShow(requireContext(), TAG, e, R.string.seed_validation_exception)
            return@setOnClickListener
          }
          else -> throw e
        }
      }

      Toast.makeText(requireContext(), R.string.seed_import_success, Toast.LENGTH_SHORT).show()
      HomeActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_home) { HomeFragment() }
    }
  }

  private fun handleBackNav() {
    HomeActivity.addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "put the app in background") {
          requireActivity().moveTaskToBack(true)
        })
  }

  companion object {
    val TAG: String = SeedWalletFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): SeedWalletFragment {
      return SeedWalletFragment()
    }
  }
}
