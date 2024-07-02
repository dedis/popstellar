package com.github.dedis.popstellar.ui.lao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LaoActivityBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.ui.home.HomeActivity.Companion.newIntent
import com.github.dedis.popstellar.ui.lao.MainMenuTab.Companion.findByMenu
import com.github.dedis.popstellar.ui.lao.digitalcash.DigitalCashHistoryFragment
import com.github.dedis.popstellar.ui.lao.digitalcash.DigitalCashHomeFragment
import com.github.dedis.popstellar.ui.lao.digitalcash.DigitalCashViewModel
import com.github.dedis.popstellar.ui.lao.event.EventsViewModel
import com.github.dedis.popstellar.ui.lao.event.consensus.ConsensusViewModel
import com.github.dedis.popstellar.ui.lao.event.election.ElectionViewModel
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment
import com.github.dedis.popstellar.ui.lao.event.meeting.MeetingViewModel
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallViewModel
import com.github.dedis.popstellar.ui.lao.federation.LinkedOrganizationsFragment
import com.github.dedis.popstellar.ui.lao.federation.LinkedOrganizationsViewModel
import com.github.dedis.popstellar.ui.lao.popcha.PoPCHAHomeFragment
import com.github.dedis.popstellar.ui.lao.popcha.PoPCHAViewModel
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaHomeFragment
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaViewModel
import com.github.dedis.popstellar.ui.lao.token.TokenListFragment
import com.github.dedis.popstellar.ui.lao.witness.WitnessingFragment
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.ActivityUtils.setFragmentInContainer
import com.github.dedis.popstellar.utility.Constants
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Deque
import java.util.LinkedList
import java.util.function.Supplier
import timber.log.Timber

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class LaoActivity : AppCompatActivity() {
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var witnessingViewModel: WitnessingViewModel
  private lateinit var binding: LaoActivityBinding

  private val fragmentStack: Deque<Fragment?> = LinkedList()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = LaoActivityBinding.inflate(layoutInflater)
    setContentView(binding.root)

    laoViewModel = obtainViewModel(this)
    val laoId = intent.extras?.getString(Constants.LAO_ID_EXTRA)!!

    laoViewModel.laoId = laoId
    laoViewModel.observeLao(laoId)
    laoViewModel.observeRollCalls(laoId)
    laoViewModel.observeInternetConnection()

    witnessingViewModel = obtainWitnessingViewModel(this, laoId)

    // At creation of the lao activity the connections of the lao are restored from the persistent
    // storage, such that the client resubscribes to each previous subscribed channel
    laoViewModel.restoreConnections()

    observeRoles()
    observeToolBar()
    observeInternetConnection()
    observeDrawer()
    setupDrawerHeader()
    observeWitnessPopup()

    // Open Event list on activity creation
    setEventsTab()

    // Display witnessing tab only if enabled
    setWitnessingTabVisibility()
  }

  /*
   Normally the saving routine should be called onStop, such as is done in other activities,
   Yet here for unknown reasons the subscriptions set in LAONetworkManager is empty when going
   to HomeActivity. This fixes it. Since the persisted data is light for now (20.05.2023) - i.e.
   server address and channel list - and not computationally intensive this will not
   be a problem at the moment
  */
  public override fun onPause() {
    super.onPause()
    laoViewModel.saveSubscriptionsData()
  }

  private fun observeRoles() {
    // Observe any change in the following variable to update the role
    laoViewModel.isWitness.observe(this) { laoViewModel.updateRole() }
    laoViewModel.isAttendee.observe(this) { laoViewModel.updateRole() }

    // Update the user's role in the drawer header when it changes
    laoViewModel.role.observe(this) { role: Role -> setupHeaderRole(role) }
  }

  private fun observeInternetConnection() {
    laoViewModel.isInternetConnected.observe(this) {
      binding.networkStatusView.setIsNetworkConnected(it)
    }
  }

  private fun observeToolBar() {
    // Listen to click on left icon of toolbar
    binding.laoAppBar.setNavigationOnClickListener {
      if (java.lang.Boolean.TRUE == laoViewModel.isTab.value) {
        // If it is a tab open menu
        binding.laoDrawerLayout.openDrawer(GravityCompat.START)
      } else {
        // Press back arrow
        onBackPressedDispatcher.onBackPressed()
      }
    }

    // Observe whether the menu icon or back arrow should be displayed
    laoViewModel.isTab.observe(this) { isTab: Boolean ->
      binding.laoAppBar.setNavigationIcon(
          if (java.lang.Boolean.TRUE == isTab) R.drawable.menu_icon else R.drawable.back_arrow_icon)
    }

    // Observe the toolbar title to display
    laoViewModel.pageTitle.observe(this) { resId: Int ->
      if (resId != 0) {
        binding.laoAppBar.setTitle(resId)
      }
    }

    // Listener for the transaction button
    binding.laoAppBar.setOnMenuItemClickListener { menuItem: MenuItem ->
      if (menuItem.itemId == R.id.history_menu_toolbar) {
        // If the user clicks on the button when the transaction history is
        // already displayed, then consider it as a back button pressed
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_lao)
        if (fragment !is DigitalCashHistoryFragment) {
          // Push onto the stack the current fragment to restore it upon exit
          fragmentStack.push(fragment)
          setCurrentFragment(supportFragmentManager, R.id.fragment_digital_cash_history) {
            DigitalCashHistoryFragment.newInstance()
          }
        } else {
          // Restore the fragment pushed on the stack before opening the transaction history
          resetLastFragment()
        }
        return@setOnMenuItemClickListener true
      }

      false
    }
  }

  private fun observeDrawer() {
    // Observe changes to the tab selected
    laoViewModel.currentTab.observe(this) { tab: MainMenuTab ->
      laoViewModel.setIsTab(true)
      binding.laoNavigationDrawer.setCheckedItem(tab.menuId)
    }

    binding.laoNavigationDrawer.setNavigationItemSelectedListener { item: MenuItem ->
      val tab = findByMenu(item.itemId)
      Timber.tag(TAG).i("Opening tab : %s", tab.name)

      val selected = openTab(tab)
      if (selected) {
        Timber.tag(TAG).d("The tab was successfully opened")
        laoViewModel.setCurrentTab(tab)
      } else {
        Timber.tag(TAG).d("The tab wasn't opened")
      }

      binding.laoDrawerLayout.close()
      selected
    }
  }

  private fun setupDrawerHeader() {
    try {
      val laoNameView =
          binding.laoNavigationDrawer
              .getHeaderView(0) // We have only one header
              .findViewById<TextView>(R.id.drawer_header_lao_title)

      laoNameView.text = laoViewModel.lao.name
    } catch (e: UnknownLaoException) {
      logAndShow(this, TAG, e, R.string.unknown_lao_exception)
      startActivity(newIntent(this))
    }
  }

  private fun observeWitnessPopup() {
    witnessingViewModel.showPopup.observe(this) { showPopup: Boolean ->
      // Ensure that the current fragment is not already witnessing
      if (java.lang.Boolean.FALSE == showPopup ||
          supportFragmentManager.findFragmentById(R.id.fragment_container_lao) is
              WitnessingFragment) {
        return@observe
      }

      // Display the Snackbar popup
      val snackbar =
          Snackbar.make(
              findViewById(R.id.fragment_container_lao),
              R.string.witness_message_popup_text,
              BaseTransientBottomBar.LENGTH_SHORT)
      snackbar.setAction(R.string.witness_message_popup_action) {
        witnessingViewModel.disableShowPopup()
        setWitnessTab()
      }
      snackbar.show()
    }
  }

  private fun setupHeaderRole(role: Role) {
    val roleView =
        binding.laoNavigationDrawer
            .getHeaderView(0) // We have only one header
            .findViewById<TextView>(R.id.drawer_header_role)

    roleView.setText(role.stringId)
  }

  private fun openTab(tab: MainMenuTab?): Boolean {
    return when (tab) {
      MainMenuTab.INVITE -> {
        openInviteTab()
        true
      }
      MainMenuTab.EVENTS -> {
        openEventsTab()
        true
      }
      MainMenuTab.TOKENS -> {
        openTokensTab()
        true
      }
      MainMenuTab.WITNESSING -> {
        openWitnessTab()
        true
      }
      MainMenuTab.DIGITAL_CASH -> {
        openDigitalCashTab()
        true
      }
      MainMenuTab.POPCHA -> {
        openPoPCHATab()
        true
      }
      MainMenuTab.SOCIAL_MEDIA -> {
        openSocialMediaTab()
        true
      }
      MainMenuTab.LINKED_ORGANIZATIONS -> {
        openLinkedOrganizationsTab()
        true
      }
      MainMenuTab.DISCONNECT -> {
        startActivity(newIntent(this))
        true
      }
      else -> {
        Timber.tag(TAG).w("Unhandled tab type : %s", tab)
        false
      }
    }
  }

  private fun openInviteTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_invite) {
      InviteFragment.newInstance()
    }
  }

  private fun openEventsTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_event_list) {
      EventListFragment.newInstance()
    }
  }

  private fun openTokensTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_tokens) {
      TokenListFragment.newInstance()
    }
  }

  private fun openWitnessTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_witnessing) {
      WitnessingFragment.newInstance()
    }
  }

  private fun openDigitalCashTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_digital_cash_home) {
      DigitalCashHomeFragment.newInstance()
    }
  }

  private fun openPoPCHATab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_popcha_home) {
      PoPCHAHomeFragment.newInstance()
    }
  }

  private fun openSocialMediaTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_social_media_home) {
      SocialMediaHomeFragment.newInstance()
    }
  }

  private fun openLinkedOrganizationsTab() {
    setCurrentFragment(supportFragmentManager, R.id.fragment_linked_organizations_home) {
      LinkedOrganizationsFragment.newInstance()
    }
  }

  /** Open Event list and select item in drawer menu */
  private fun setEventsTab() {
    binding.laoNavigationDrawer.setCheckedItem(MainMenuTab.EVENTS.menuId)
    openEventsTab()
  }

  /**
   * Set to invisible in the drawer menu the 'Witness' tab if it has been disabled upon LAO creation
   */
  private fun setWitnessingTabVisibility() {
    binding.laoNavigationDrawer.menu.findItem(R.id.main_menu_witnessing).isVisible =
        laoViewModel.isWitnessingEnabled
  }

  /** Opens Witness tab and select it in the drawer menu */
  private fun setWitnessTab() {
    binding.laoNavigationDrawer.setCheckedItem(MainMenuTab.WITNESSING.menuId)
    openWitnessTab()
  }

  /** Restore the fragment contained in the stack as container of the current lao */
  fun resetLastFragment() {
    val fragment = fragmentStack.pop()
    supportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container_lao, fragment!!)
        .commit()
  }

  companion object {
    val TAG: String = LaoActivity::class.java.simpleName

    @JvmStatic
    fun obtainViewModel(activity: FragmentActivity): LaoViewModel {
      return ViewModelProvider(activity)[LaoViewModel::class.java]
    }

    @JvmStatic
    fun obtainEventsEventsViewModel(activity: FragmentActivity, laoId: String): EventsViewModel {
      val eventsViewModel = ViewModelProvider(activity)[EventsViewModel::class.java]
      eventsViewModel.setId(laoId)
      return eventsViewModel
    }

    @JvmStatic
    fun obtainConsensusViewModel(activity: FragmentActivity, laoId: String): ConsensusViewModel {
      val consensusViewModel = ViewModelProvider(activity)[ConsensusViewModel::class.java]
      consensusViewModel.setLaoId(laoId)
      return consensusViewModel
    }

    @JvmStatic
    fun obtainElectionViewModel(activity: FragmentActivity, laoId: String): ElectionViewModel {
      val electionViewModel = ViewModelProvider(activity)[ElectionViewModel::class.java]
      electionViewModel.setLaoId(laoId)
      return electionViewModel
    }

    @JvmStatic
    fun obtainRollCallViewModel(activity: FragmentActivity, laoId: String?): RollCallViewModel {
      val rollCallViewModel = ViewModelProvider(activity)[RollCallViewModel::class.java]
      rollCallViewModel.setLaoId(laoId)
      return rollCallViewModel
    }

    @JvmStatic
    fun obtainMeetingViewModel(activity: FragmentActivity, laoId: String): MeetingViewModel {
      val meetingViewModel = ViewModelProvider(activity)[MeetingViewModel::class.java]
      meetingViewModel.setLaoId(laoId)
      return meetingViewModel
    }

    @JvmStatic
    fun obtainWitnessingViewModel(activity: FragmentActivity, laoId: String?): WitnessingViewModel {
      val witnessingViewModel = ViewModelProvider(activity)[WitnessingViewModel::class.java]

      return try {
        witnessingViewModel.initialize(laoId)
      } catch (e: UnknownLaoException) {
        Timber.tag(TAG)
            .e(e, "Unable to initialize the witnessing model: not found lao with lao id=%s", laoId)
        witnessingViewModel
      }
    }

    @JvmStatic
    fun obtainSocialMediaViewModel(
        activity: FragmentActivity,
        laoId: String
    ): SocialMediaViewModel {
      val socialMediaViewModel = ViewModelProvider(activity)[SocialMediaViewModel::class.java]
      socialMediaViewModel.setLaoId(laoId)
      return socialMediaViewModel
    }

    @JvmStatic
    fun obtainDigitalCashViewModel(
        activity: FragmentActivity,
        laoId: String
    ): DigitalCashViewModel {
      val digitalCashViewModel = ViewModelProvider(activity)[DigitalCashViewModel::class.java]
      digitalCashViewModel.laoId = laoId
      return digitalCashViewModel
    }

    @JvmStatic
    fun obtainPoPCHAViewModel(activity: FragmentActivity, laoId: String?): PoPCHAViewModel {
      val popCHAViewModel = ViewModelProvider(activity)[PoPCHAViewModel::class.java]
      popCHAViewModel.laoId = laoId
      return popCHAViewModel
    }

    @JvmStatic
    fun obtainLinkedOrganizationsViewModel(
        activity: FragmentActivity,
        laoId: String?
    ): LinkedOrganizationsViewModel {
      val linkedOrganizationsViewModel =
          ViewModelProvider(activity)[LinkedOrganizationsViewModel::class.java]
      if (laoId != null) {
        linkedOrganizationsViewModel.setLaoId(laoId)
      }
      return linkedOrganizationsViewModel
    }

    /**
     * Set the current fragment in the container of the activity
     *
     * @param id of the fragment
     * @param fragmentSupplier provides the fragment if it is missing
     */
    @JvmStatic
    fun setCurrentFragment(
        manager: FragmentManager,
        @IdRes id: Int,
        fragmentSupplier: Supplier<Fragment>
    ) {
      setFragmentInContainer(manager, R.id.fragment_container_lao, id, fragmentSupplier)
    }

    fun newIntentForLao(ctx: Context, laoId: String): Intent {
      val intent = Intent(ctx, LaoActivity::class.java)
      intent.putExtra(Constants.LAO_ID_EXTRA, laoId)

      return intent
    }

    /**
     * Adds a callback that describes the action to take the next time the back button is pressed
     */
    @JvmStatic
    fun addBackNavigationCallback(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        callback: OnBackPressedCallback
    ) {
      activity.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
    }

    /** Adds a specific callback for the back button that opens the events tab */
    @JvmStatic
    fun addBackNavigationCallbackToEvents(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        tag: String
    ) {
      addBackNavigationCallback(
          activity,
          lifecycleOwner,
          buildBackButtonCallback(tag, "event list") { (activity as LaoActivity).setEventsTab() })
    }
  }
}
