package com.github.dedis.popstellar.ui.lao.event.eventlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.EventListFragmentBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.model.objects.event.Event
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.EventType
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainEventsEventsViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.EventsViewModel
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.fadeIn
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.fadeOut
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.rotateFab
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showIn
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showOut
import com.github.dedis.popstellar.ui.lao.event.UpcomingEventsFragment
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionSetupFragment
import com.github.dedis.popstellar.ui.lao.event.meeting.MeetingCreationFragment
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallCreationFragment
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/** Fragment used to display the list of events */
@AndroidEntryPoint
class EventListFragment : Fragment() {
  @Inject lateinit var gson: Gson

  private lateinit var binding: EventListFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var eventsViewModel: EventsViewModel

  private var isRotated = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = EventListFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    eventsViewModel = obtainEventsEventsViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.lifecycleOwner = requireActivity()

    val addButton = binding.addEvent
    addButton.setOnClickListener(fabListener)

    laoViewModel.role.observe(requireActivity()) { role: Role ->
      addButton.visibility = if (role == Role.ORGANIZER) View.VISIBLE else View.GONE
    }

    binding.addElection.setOnClickListener(openCreateEvent(EventType.ELECTION))
    binding.addElectionText.setOnClickListener(openCreateEvent(EventType.ELECTION))
    binding.addRollCall.setOnClickListener(openCreateEvent(EventType.ROLL_CALL))
    binding.addRollCallText.setOnClickListener(openCreateEvent(EventType.ROLL_CALL))
    binding.addMeeting.setOnClickListener(openCreateEvent(EventType.MEETING))
    binding.addMeetingText.setOnClickListener(openCreateEvent(EventType.MEETING))

    // Observing events so that we know when to display the upcoming events card and displaying the
    // Empty events text
    laoViewModel.addDisposable(
        eventsViewModel.events.subscribe({ events: Set<Event> ->
          setupUpcomingEventsCard(events)
          setupEmptyEventsTextVisibility(events)
        }) {
          logAndShow(requireContext(), TAG, R.string.error_event_observed)
        })

    // Add listener to upcoming events card
    binding.upcomingEventsCard.setOnClickListener {
      setCurrentFragment(parentFragmentManager, R.id.fragment_upcoming_events) {
        UpcomingEventsFragment.newInstance()
      }
    }

    // Observe role to match empty event text to it
    laoViewModel.role.observe(viewLifecycleOwner) { role: Role ->
      binding.emptyEventsText.setText(
          if (role == Role.ORGANIZER) R.string.empty_events_organizer_text
          else R.string.empty_events_non_organizer_text)
    }
    return binding.root
  }

  private var fabListener =
      View.OnClickListener { view: View ->
        val laoContainer = binding.laoContainer
        isRotated = rotateFab(view, !isRotated)

        if (isRotated) {
          showIn(binding.addRollCall)
          showIn(binding.addMeeting)
          showIn(binding.addElection)
          showIn(binding.addRollCallText)
          showIn(binding.addMeetingText)
          showIn(binding.addElectionText)

          fadeOut(laoContainer, 1.0f, 0.2f, 300)
          laoContainer.isEnabled = false
        } else {
          showOut(binding.addRollCall)
          showOut(binding.addMeeting)
          showOut(binding.addElection)
          showOut(binding.addRollCallText)
          showOut(binding.addMeetingText)
          showOut(binding.addElectionText)

          fadeIn(laoContainer, 0.2f, 1.0f, 300)
          laoContainer.isEnabled = true
        }
      }

  private fun openCreateEvent(type: EventType): View.OnClickListener {
    return when (type) {
      EventType.ROLL_CALL ->
          View.OnClickListener {
            setCurrentFragment(parentFragmentManager, R.id.fragment_create_roll_call_event) {
              RollCallCreationFragment.newInstance()
            }
          }
      EventType.MEETING ->
          View.OnClickListener {
            setCurrentFragment(parentFragmentManager, R.id.fragment_create_meeting_event) {
              MeetingCreationFragment.newInstance()
            }
          }
      EventType.ELECTION ->
          View.OnClickListener {
            setCurrentFragment(parentFragmentManager, R.id.fragment_setup_election_event) {
              ElectionSetupFragment.newInstance()
            }
          }
      else -> View.OnClickListener { Timber.tag(TAG).d("unknown event type: %s", type) }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupEventListAdapter()
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.event_list)
    laoViewModel.setIsTab(true)
  }

  private fun setupEventListAdapter() {
    val eventList = binding.eventList
    val eventListAdapter = EventListAdapter(laoViewModel, eventsViewModel.events, requireActivity())

    Timber.tag(TAG).d("created adapter")

    val mLayoutManager = LinearLayoutManager(context)
    eventList.layoutManager = mLayoutManager
    eventList.adapter = eventListAdapter
  }

  private fun setupUpcomingEventsCard(events: Set<Event>) {
    binding.upcomingEventsCard.visibility =
        if (events.stream().anyMatch // We are looking for any event that is in future section
             { event: Event -> // We want created events that are in more than 24 hours
          event.state == EventState.CREATED && !event.isEventEndingToday
        })
            View.VISIBLE
        else View.GONE
  }

  private fun setupEmptyEventsTextVisibility(events: Set<Event>) {
    binding.emptyEventsLayout.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
  }

  companion object {
    val TAG: String = EventListFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): EventListFragment {
      return EventListFragment()
    }

    @JvmStatic
    fun openFragment(manager: FragmentManager) {
      setCurrentFragment(manager, R.id.fragment_event_list) { newInstance() }
    }
  }
}
