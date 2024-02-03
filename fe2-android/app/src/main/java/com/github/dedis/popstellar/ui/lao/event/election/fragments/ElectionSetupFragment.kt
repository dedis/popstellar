package com.github.dedis.popstellar.ui.lao.event.election.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ElectionSetupFragmentBinding
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion.Companion.allElectionVersion
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainElectionViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.AbstractEventCreationFragment
import com.github.dedis.popstellar.ui.lao.event.election.ElectionViewModel
import com.github.dedis.popstellar.ui.lao.event.election.ZoomOutTransformer
import com.github.dedis.popstellar.ui.lao.event.election.adapters.ElectionSetupViewPagerAdapter
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors
import timber.log.Timber

@AndroidEntryPoint
class ElectionSetupFragment : AbstractEventCreationFragment() {
  // mandatory fields for submitting
  private lateinit var electionNameText: EditText
  private lateinit var viewPagerAdapter: ElectionSetupViewPagerAdapter
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var electionViewModel: ElectionViewModel

  // For election version choice
  private var electionVersion: ElectionVersion? = null

  // Enum of all voting methods, associated to a string desc for protocol and spinner display
  enum class VotingMethods(val desc: String) {
    PLURALITY("Plurality")
  }

  // Text watcher that checks if mandatory fields are filled for submitting each time the user
  // changes a field (with at least two valid ballot options)
  private val submitTextWatcher: TextWatcher =
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
          /* no check to make before text is changed */
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
          /* no check to make during the text is being changes */
        }

        override fun afterTextChanged(s: Editable) {
          // On each change of election level information, we check that at least one question is
          // complete to know if submit is allowed
          confirmButton!!.isEnabled =
              isElectionLevelInputValid &&
                  java.lang.Boolean.TRUE == viewPagerAdapter.isAnInputValid.value
        }
      }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val binding = ElectionSetupFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    electionViewModel = obtainElectionViewModel(requireActivity(), laoViewModel.laoId!!)

    // Set the view for the date and time
    setDateAndTimeView(binding.root)

    // Make the textWatcher listen to changes in the start and end date/time
    addEndDateAndTimeListener(submitTextWatcher)
    addStartDateAndTimeListener(submitTextWatcher)
    confirmButton = binding.electionSubmitButton
    electionNameText = binding.electionSetupName

    // Add text watchers on the fields that need to be filled
    electionNameText.addTextChangedListener(submitTextWatcher)

    // Set viewPager adapter
    viewPagerAdapter = ElectionSetupViewPagerAdapter()

    // Set ViewPager
    val viewPager2 = binding.electionSetupViewPager2
    viewPager2.adapter = viewPagerAdapter

    // Sets animation on swipe
    viewPager2.setPageTransformer(ZoomOutTransformer())

    // This sets the indicator of which page we are on
    val circleIndicator = binding.electionSetupSwipeIndicator
    circleIndicator.setViewPager(viewPager2)

    // This observes if at least one of the question has the minimal information
    viewPagerAdapter.isAnInputValid.observe(viewLifecycleOwner) { aBoolean: Boolean ->
      confirmButton!!.isEnabled = aBoolean && isElectionLevelInputValid
    }
    val addQuestion = binding.addQuestion
    addQuestion.setOnClickListener {
      addQuestion.isEnabled = false
      viewPagerAdapter.addQuestion()

      // This scales for a few dozens of questions but this is dangerous and  greedy in
      // resources
      // TODO delete this and find a way to keep data on left swipe
      viewPager2.offscreenPageLimit = viewPagerAdapter.numberOfQuestions

      // This swipes automatically to new question
      viewPager2.currentItem = viewPager2.currentItem + 1

      // Updates the number of circles in the indicator
      circleIndicator.setViewPager(viewPager2)
      addQuestion.isEnabled = true
    }

    // Create a listener that updates the user's choice for election (by default it's OPEN_BALLOT)
    // Then it set's up the spinner
    val versionSpinner = binding.electionSetupModeSpinner
    val listener: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(
              parent: AdapterView<*>?,
              view: View?,
              position: Int,
              id: Long
          ) {
            if (position == 0) {
              electionVersion = ElectionVersion.OPEN_BALLOT
            } else if (position == 1) {
              electionVersion = ElectionVersion.SECRET_BALLOT
            }
          }

          override fun onNothingSelected(parent: AdapterView<*>?) {
            electionVersion = ElectionVersion.OPEN_BALLOT
          }
        }

    setUpElectionVersionSpinner(versionSpinner, listener)
    createEvent()

    binding.lifecycleOwner = activity
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.election_setup_title)
    laoViewModel.setIsTab(false)
  }

  /** Setups the submit button that creates the new election */
  override fun createEvent() {
    confirmButton?.setOnClickListener {
      // We "deactivate" the button on click, to prevent the user from creating multiple
      // elections at once
      confirmButton?.isEnabled = false

      // When submitting, we compute the timestamps for the selected start and end time
      if (!computeTimesInSeconds()) {
        return@setOnClickListener
      }
      val validPositions = viewPagerAdapter.validInputs
      val votingMethod = viewPagerAdapter.getVotingMethod()
      val questions = viewPagerAdapter.getQuestions()
      val ballotsOptions: List<List<String>> = viewPagerAdapter.getBallotOptions()

      val filteredQuestions =
          validPositions
              .stream()
              .map { i: Int ->
                Question(
                    questions[i],
                    votingMethod[i],
                    ballotsOptions[i] // Filter out empty options
                        .stream()
                        .filter { ballotOption: String -> "" != ballotOption }
                        .collect(Collectors.toList()),
                    false // While write in is not implemented
                    )
              }
              .collect(Collectors.toList())

      val electionName = electionNameText.text.toString()

      Timber.tag(TAG)
          .d(
              "Creating election with version %s, name %s, creation time %d, start time %d, end time %d, questions %s",
              electionVersion,
              electionName,
              creationTimeInSeconds,
              startTimeInSeconds,
              endTimeInSeconds,
              filteredQuestions)

      laoViewModel.addDisposable(
          electionViewModel
              .createNewElection(
                  electionVersion!!,
                  electionName,
                  creationTimeInSeconds,
                  startTimeInSeconds,
                  endTimeInSeconds,
                  filteredQuestions)
              .subscribe(
                  {
                    setCurrentFragment(parentFragmentManager, R.id.fragment_event_list) {
                      EventListFragment.newInstance()
                    }
                  },
                  { error: Throwable ->
                    logAndShow(requireContext(), TAG, error, R.string.error_create_election)
                  }))
    }
  }

  private val isElectionLevelInputValid: Boolean
    /** @return true if the election name text, dates and times inputs are valid */
    get() =
        (electionNameText.text.toString().trim { it <= ' ' }.isNotEmpty() &&
            getStartDate().isNotEmpty() &&
            getStartTime().isNotEmpty() &&
            getEndDate().isNotEmpty() &&
            getEndTime().isNotEmpty())

  /**
   * Sets up the dropdown menu for election versions: open-ballot and secret-ballot
   *
   * @param spinner the spinner to modify
   * @param listener listener to spinner event
   */
  private fun setUpElectionVersionSpinner(
      spinner: Spinner,
      listener: AdapterView.OnItemSelectedListener
  ) {
    val versionsList = allElectionVersion
    val items: MutableList<String> = ArrayList()

    // Add items to version list
    for (v in versionsList) {
      items.add(v.stringBallotVersion)
    }

    // Set up the spinner with voting versions
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)

    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner.adapter = adapter
    spinner.onItemSelectedListener = listener
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = ElectionSetupFragment::class.java.simpleName

    fun newInstance(): ElectionSetupFragment {
      return ElectionSetupFragment()
    }
  }
}
