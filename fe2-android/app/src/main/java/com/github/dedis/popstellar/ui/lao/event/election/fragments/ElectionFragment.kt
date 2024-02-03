package com.github.dedis.popstellar.ui.lao.event.election.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainElectionViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.Constants.DISABLED_ALPHA
import com.github.dedis.popstellar.utility.Constants.ENABLED_ALPHA
import com.github.dedis.popstellar.utility.Constants.ID_NULL
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumMap
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ElectionFragment : Fragment() {
  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH)

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var view: View
  private lateinit var managementButton: Button
  private lateinit var actionButton: Button
  private lateinit var managementVisibilityMap: EnumMap<EventState, Int>
  @Inject lateinit var electionRepository: ElectionRepository
  private lateinit var electionId: String

  private val managementTextMap = buildManagementTextMap()
  private val statusTextMap = buildStatusTextMap()
  private val statusIconMap = buildStatusIconMap()
  private val managementIconMap = buildManagementIconMap()
  private val statusColorMap = buildStatusColorMap()
  private val managementColorMap = buildManagementColorMap()
  private val actionIconMap = buildActionIconMap()
  private val actionTextMap = buildActionTextMap()
  private val actionEnablingMap = buildActionEnablingMap()
  private val disposables = CompositeDisposable()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    // Inflate the layout for this fragment
    view = inflater.inflate(R.layout.election_fragment, container, false)

    managementButton = view.findViewById(R.id.election_management_button)
    actionButton = view.findViewById(R.id.election_action_button)
    electionId = requireArguments().getString(ELECTION_ID)!!

    laoViewModel = obtainViewModel(requireActivity())
    val electionViewModel = obtainElectionViewModel(requireActivity(), laoViewModel.laoId!!)
    managementVisibilityMap = buildManagementVisibilityMap()
    if (!electionViewModel.canVote()) {
      resetEnablingMap()
    }

    managementButton.setOnClickListener {
      val election: Election =
          try {
            electionRepository.getElection(laoViewModel.laoId!!, electionId)
          } catch (e: UnknownElectionException) {
            logAndShow(requireContext(), TAG, e, R.string.generic_error)
            return@setOnClickListener
          }

      when (val state = election.state) {
        EventState
            .CREATED -> // When implemented across all subsystems go into start election fragment
            // which
            // implements consensus
            AlertDialog.Builder(context)
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.election_confirm_open)
                .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                  laoViewModel.addDisposable(
                      electionViewModel
                          .openElection(election)
                          .subscribe(
                              {},
                              { error: Throwable ->
                                logAndShow(
                                    requireContext(), TAG, error, R.string.error_open_election)
                              }))
                }
                .setNegativeButton(R.string.no, null)
                .show()
        EventState.OPENED ->
            AlertDialog.Builder(context)
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.election_confirm_close)
                .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                  laoViewModel.addDisposable(
                      electionViewModel
                          .endElection(election)
                          .subscribe(
                              {},
                              { error: Throwable ->
                                logAndShow(
                                    requireContext(), TAG, error, R.string.error_end_election)
                              }))
                }
                .setNegativeButton(R.string.no, null)
                .show()
        else ->
            throw IllegalStateException(
                "User should not be able to use the management button when in this state : $state")
      }
    }
    actionButton.setOnClickListener {
      val election: Election =
          try {
            electionRepository.getElection(laoViewModel.laoId!!, electionId)
          } catch (e: UnknownElectionException) {
            logAndShow(requireContext(), TAG, e, R.string.generic_error)
            return@setOnClickListener
          }

      when (val state = election.state) {
        EventState.OPENED ->
            setCurrentFragment(parentFragmentManager, R.id.fragment_cast_vote) {
              CastVoteFragment.newInstance(electionId)
            }
        EventState.RESULTS_READY ->
            setCurrentFragment(parentFragmentManager, R.id.fragment_election_result) {
              ElectionResultFragment.newInstance(electionId)
            }
        else ->
            throw IllegalStateException(
                "User should not be able to use the action button in this state :$state")
      }
    }

    handleBackNav()

    return view
  }

  /** Set to not enabled the button "Vote" */
  private fun resetEnablingMap() {
    actionEnablingMap[EventState.CREATED] = false
    actionEnablingMap[EventState.OPENED] = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    try {
      disposables.add(
          electionRepository
              .getElectionObservable(laoViewModel.laoId!!, electionId)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  { election: Election -> setupElectionContent(election) },
                  { err: Throwable ->
                    logAndShow(requireContext(), TAG, err, R.string.generic_error)
                  }))
    } catch (e: UnknownElectionException) {
      logAndShow(requireContext(), TAG, e, R.string.generic_error)
    }
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.election_title)
    laoViewModel.setIsTab(false)
  }

  override fun onDestroy() {
    disposables.dispose()
    super.onDestroy()
  }

  private fun setupElectionContent(election: Election) {
    val electionState = election.state
    val title = view.findViewById<TextView>(R.id.election_fragment_title)
    title.text = election.name

    // Fill action content
    val statusText = view.findViewById<TextView>(R.id.election_fragment_status)
    val imgAction = getDrawableFromContext(actionIconMap.getOrDefault(electionState, ID_NULL))

    actionButton.setCompoundDrawablesWithIntrinsicBounds(imgAction, null, null, null)
    setButtonEnabling(actionButton, actionEnablingMap.getOrDefault(electionState, false))
    actionButton.setText(actionTextMap.getOrDefault(electionState, ID_NULL))

    // Fill status content
    val statusIcon = view.findViewById<ImageView>(R.id.election_fragment_status_icon)
    val imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(electionState, ID_NULL))
    setImageColor(statusIcon, statusColorMap.getOrDefault(electionState, ID_NULL))
    statusText.setTextColor(
        resources.getColor(statusColorMap.getOrDefault(electionState, ID_NULL), null))
    statusText.setText(statusTextMap.getOrDefault(electionState, ID_NULL))
    statusIcon.setImageDrawable(imgStatus)

    // Fill management content
    val imgManagement =
        getDrawableFromContext(managementIconMap.getOrDefault(electionState, ID_NULL))
    setButtonColor(managementButton, managementColorMap.getOrDefault(electionState, ID_NULL))
    managementButton.setText(managementTextMap.getOrDefault(electionState, ID_NULL))
    managementButton.setCompoundDrawablesWithIntrinsicBounds(imgManagement, null, null, null)
    managementButton.visibility = managementVisibilityMap.getOrDefault(electionState, View.GONE)

    val startTimeDisplay = view.findViewById<TextView>(R.id.election_fragment_start_time)
    val endTimeDisplay = view.findViewById<TextView>(R.id.election_fragment_end_time)
    val startTime = Date(election.startTimestampInMillis)
    val endTime = Date(election.endTimestampInMillis)

    startTimeDisplay.text = dateFormat.format(startTime)
    endTimeDisplay.text = dateFormat.format(endTime)
  }

  private fun getDrawableFromContext(id: Int): Drawable? {
    return AppCompatResources.getDrawable(requireContext(), id)
  }

  private fun setButtonColor(v: View?, colorId: Int) {
    v?.backgroundTintList = resources.getColorStateList(colorId, null)
  }

  private fun setImageColor(imageView: ImageView, colorId: Int) {
    ImageViewCompat.setImageTintList(imageView, resources.getColorStateList(colorId, null))
  }

  private fun buildManagementTextMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.string.open
    map[EventState.OPENED] = R.string.close

    // Button will be invisible in those state
    map[EventState.CLOSED] = R.string.close
    map[EventState.RESULTS_READY] = R.string.close
    return map
  }

  private fun buildStatusTextMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.string.created_displayed_text
    map[EventState.OPENED] = R.string.open
    map[EventState.CLOSED] = R.string.waiting_for_results
    map[EventState.RESULTS_READY] = R.string.finished

    return map
  }

  private fun buildStatusIconMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.drawable.ic_lock
    map[EventState.OPENED] = R.drawable.ic_unlock
    map[EventState.CLOSED] = R.drawable.ic_wait
    map[EventState.RESULTS_READY] = R.drawable.ic_complete

    return map
  }

  private fun buildStatusColorMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.color.red
    map[EventState.OPENED] = R.color.green
    map[EventState.CLOSED] = R.color.colorPrimary
    map[EventState.RESULTS_READY] = R.color.green

    return map
  }

  private fun buildManagementIconMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.drawable.ic_unlock
    map[EventState.OPENED] = R.drawable.ic_lock

    // Button will be invisible in those state
    map[EventState.CLOSED] = R.drawable.ic_lock
    map[EventState.RESULTS_READY] = R.drawable.ic_lock

    return map
  }

  private fun buildManagementColorMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.color.green
    map[EventState.OPENED] = R.color.red

    // Button will be invisible in those state
    map[EventState.CLOSED] = R.color.red
    map[EventState.RESULTS_READY] = R.color.red

    return map
  }

  private fun buildActionIconMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.drawable.ic_voting_action
    map[EventState.OPENED] = R.drawable.ic_voting_action
    map[EventState.CLOSED] = R.drawable.ic_result
    map[EventState.RESULTS_READY] = R.drawable.ic_result

    return map
  }

  private fun buildActionTextMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = R.string.vote
    map[EventState.OPENED] = R.string.vote
    map[EventState.CLOSED] = R.string.results
    map[EventState.RESULTS_READY] = R.string.results

    return map
  }

  private fun buildManagementVisibilityMap(): EnumMap<EventState, Int> {
    // Only the organizer may start or end an election
    val organizerVisibility = if (laoViewModel.isOrganizer) View.VISIBLE else View.GONE
    val map = EnumMap<EventState, Int>(EventState::class.java)

    map[EventState.CREATED] = organizerVisibility
    map[EventState.OPENED] = organizerVisibility
    map[EventState.CLOSED] = View.GONE // Button is invisible regardless of user's role
    map[EventState.RESULTS_READY] = View.GONE // Button is invisible regardless of user's role

    return map
  }

  private fun buildActionEnablingMap(): EnumMap<EventState, Boolean> {
    val map = EnumMap<EventState, Boolean>(EventState::class.java)

    map[EventState.CREATED] = false
    map[EventState.OPENED] = true
    map[EventState.CLOSED] = false
    map[EventState.RESULTS_READY] = true

    return map
  }

  private fun setButtonEnabling(button: Button?, enabled: Boolean) {
    button?.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    button?.isEnabled = enabled
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    private val TAG = ElectionFragment::class.java.simpleName
    private const val ELECTION_ID = "election_id"

    @JvmStatic
    fun newInstance(electionId: String?): ElectionFragment {
      val fragment = ElectionFragment()
      val args = Bundle()
      args.putString(ELECTION_ID, electionId)
      fragment.arguments = args

      return fragment
    }

    fun openFragment(manager: FragmentManager, electionId: String) {
      setCurrentFragment(manager, R.id.fragment_election) { newInstance(electionId) }
    }
  }
}
