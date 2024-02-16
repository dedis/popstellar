package com.github.dedis.popstellar.ui.home

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.HomeActivityBinding
import com.github.dedis.popstellar.model.network.serializer.JsonUtils
import com.github.dedis.popstellar.model.network.serializer.JsonUtils.loadSchema
import com.github.dedis.popstellar.ui.home.wallet.SeedWalletFragment
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.ActivityUtils.setFragmentInContainer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.security.GeneralSecurityException
import java.util.function.Supplier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** HomeActivity represents the entry point for the application. */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
  private val TAG: String = HomeActivity::class.java.simpleName

  private lateinit var viewModel: HomeViewModel
  private lateinit var binding: HomeActivityBinding

  @OptIn(DelicateCoroutinesApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = HomeActivityBinding.inflate(layoutInflater)

    setContentView(binding.root)
    viewModel = obtainViewModel(this)

    // When back to the home activity set connecting in view model to false
    viewModel.disableConnectingFlag()

    handleTopAppBar()

    // Load all the json schemas in background when the app is started.
    GlobalScope.launch {
      loadSchema(JsonUtils.ROOT_SCHEMA)
      loadSchema(JsonUtils.DATA_SCHEMA)
      loadSchema(JsonUtils.GENERAL_MESSAGE_SCHEMA)
    }

    // At start of Activity we display home fragment
    setCurrentFragment(supportFragmentManager, R.id.fragment_home) { HomeFragment.newInstance() }

    // Try to restore the wallet if persisted in the database
    if (!viewModel.restoreWallet()) {
      // If the state restore fails it means that no wallet is set up
      setCurrentFragment(supportFragmentManager, R.id.fragment_seed_wallet) {
        SeedWalletFragment.newInstance()
      }

      MaterialAlertDialogBuilder(this)
          .setMessage(R.string.wallet_init_message)
          .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
          .show()
    }
  }

  private fun handleTopAppBar() {
    viewModel.pageTitle.observe(this) { resId: Int -> binding.topAppBar.setTitle(resId) }

    setNavigation()
    setMenuItemListener()
    observeWallet()
  }

  private fun setNavigation() {
    // Observe whether the home icon or back arrow should be displayed
    viewModel.isHome.observe(this) { isHome: Boolean ->
      if (java.lang.Boolean.TRUE == isHome) {
        binding.topAppBar.setNavigationIcon(R.drawable.home_icon)
        binding.topAppBar.menu.setGroupVisible(0, true)
      } else {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_home)

        // If the fragment is not the seed wallet then make the back arrow appear
        if (fragment is SeedWalletFragment) {
          binding.topAppBar.navigationIcon = null
        } else {
          binding.topAppBar.setNavigationIcon(R.drawable.back_arrow_icon)
        }

        // Disable the overflow menu
        binding.topAppBar.menu.setGroupVisible(0, false)
      }
    }

    // Listen to click on left icon of toolbar
    binding.topAppBar.setNavigationOnClickListener {
      if (java.lang.Boolean.FALSE == viewModel.isHome.value) {
        Timber.tag(TAG).d("Going back to home")
        // Press back arrow
        onBackPressedDispatcher.onBackPressed()
      } else {
        // If the user presses on the home button display the general info about the app
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.app_info)
            .setNeutralButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
      }
    }
  }

  private fun setMenuItemListener() {
    // Set menu items behaviour
    binding.topAppBar.setOnMenuItemClickListener { item: MenuItem ->
      when (item.itemId) {
        R.id.wallet_init_logout -> {
          handleWalletSettings()
        }
        R.id.clear_storage -> {
          handleClearing()
        }
        R.id.home_settings -> {
          handleSettings()
        }
      }
      true
    }
  }

  private fun observeWallet() {
    // Listen to wallet status to adapt the menu item title
    viewModel.isWalletSetUpEvent.observe(this) { isSetUp: Boolean ->
      binding.topAppBar.menu
          .getItem(0)
          .setTitle(
              if (java.lang.Boolean.TRUE == isSetUp) R.string.logout_title
              else R.string.wallet_setup)
    }
  }

  public override fun onStop() {
    super.onStop()

    // On stop persist the wallet
    try {
      viewModel.saveWallet()
    } catch (e: GeneralSecurityException) {
      // We do not display the security error to the user
      Timber.tag(TAG).d(e, "Storage was unsuccessful due to wallet error")
      Toast.makeText(this, R.string.error_storage_wallet, Toast.LENGTH_SHORT).show()
    }
  }

  private fun handleWalletSettings() {
    if (viewModel.isWalletSetUp) {
      MaterialAlertDialogBuilder(this)
          .setTitle(R.string.logout_title)
          .setMessage(R.string.logout_message)
          .setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int ->
            viewModel.logoutWallet()
            setCurrentFragment(supportFragmentManager, R.id.fragment_seed_wallet) {
              SeedWalletFragment.newInstance()
            }
          }
          .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
          }
          .show()
    } else {
      setCurrentFragment(supportFragmentManager, R.id.fragment_seed_wallet) {
        SeedWalletFragment.newInstance()
      }
    }
  }

  private fun handleClearing() {
    AlertDialog.Builder(this)
        .setTitle(R.string.confirm_title)
        .setMessage(R.string.clear_confirmation_text)
        .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
          viewModel.clearStorage()

          Toast.makeText(this, R.string.clear_success, Toast.LENGTH_LONG).show()

          // Restart activity
          val intent = newIntent(this)

          // Flags to clear data structures and free memory
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          startActivity(intent)

          finish()
        }
        .setNegativeButton(R.string.no, null)
        .show()
  }

  private fun handleSettings() {
    setFragmentInContainer(supportFragmentManager, R.id.fragment_container_home) {
      SettingsFragment.newInstance()
    }
  }

  companion object {
    @JvmStatic
    fun obtainViewModel(activity: FragmentActivity): HomeViewModel {
      return ViewModelProvider(activity)[HomeViewModel::class.java]
    }

    fun obtainSettingsViewModel(activity: FragmentActivity): SettingsViewModel {
      return ViewModelProvider(activity)[SettingsViewModel::class.java]
    }

    fun obtainWitnessingViewModel(activity: FragmentActivity): WitnessingViewModel {
      return ViewModelProvider(activity)[WitnessingViewModel::class.java]
    }

    /** Factory method to create a fresh Intent that opens an HomeActivity */
    @JvmStatic
    fun newIntent(ctx: Context?): Intent {
      return Intent(ctx, HomeActivity::class.java)
    }

    /**
     * Set the current fragment in the container of the home activity
     *
     * @param manager the manager of the activity
     * @param id of the fragment
     * @param fragmentSupplier provides the fragment if it is missing
     */
    fun setCurrentFragment(
        manager: FragmentManager,
        @IdRes id: Int,
        fragmentSupplier: Supplier<Fragment>
    ) {
      setFragmentInContainer(manager, R.id.fragment_container_home, id, fragmentSupplier)
    }

    /**
     * Adds a callback that describes the action to take the next time the back button is pressed
     */
    fun addBackNavigationCallback(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        callback: OnBackPressedCallback
    ) {
      activity.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
    }

    /** Adds a specific callback for the back button that opens the home fragment */
    fun addBackNavigationCallbackToHome(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        tag: String
    ) {
      addBackNavigationCallback(
          activity,
          lifecycleOwner,
          buildBackButtonCallback(tag, "home fragment") {
            setCurrentFragment(activity.supportFragmentManager, R.id.fragment_home) {
              HomeFragment()
            }
          })
    }
  }
}
