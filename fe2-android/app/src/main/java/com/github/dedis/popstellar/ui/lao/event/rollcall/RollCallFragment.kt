package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.RollCallFragmentBinding
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.qrcode.PopTokenData
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainRollCallViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.event.AbstractEventFragment
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment.Companion.newInstance
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import com.github.dedis.popstellar.utility.ActivityUtils.getQRCodeColor
import com.github.dedis.popstellar.utility.ActivityUtils.handleExpandArrow
import com.github.dedis.popstellar.utility.Constants.ID_NULL
import com.github.dedis.popstellar.utility.Constants.ROLL_CALL_ID
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import dagger.hilt.android.AndroidEntryPoint
import java.util.EnumMap
import java.util.stream.Collectors
import javax.inject.Inject
import net.glxn.qrgen.android.QRCode
import timber.log.Timber

@AndroidEntryPoint
class RollCallFragment : AbstractEventFragment {
  @Inject lateinit var rollCallRepo: RollCallRepository

  private lateinit var binding: RollCallFragmentBinding
  private lateinit var rollCall: RollCall
  private lateinit var rollCallViewModel: RollCallViewModel

  private val managementTextMap = buildManagementTextMap()
  private val managementIconMap = buildManagementIconMap()

  constructor()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    binding = RollCallFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    rollCallViewModel = obtainRollCallViewModel(requireActivity(), laoViewModel.laoId)

    rollCall =
        try {
          rollCallRepo.getRollCallWithPersistentId(
              laoViewModel.laoId!!, requireArguments().getString(ROLL_CALL_ID)!!)
        } catch (e: UnknownRollCallException) {
          logAndShow(requireContext(), TAG, e, R.string.unknown_roll_call_exception)
          return null
        }

    setUpStateDependantContent()

    // Set the description dropdown
    binding.rollCallDescriptionCard.setOnClickListener {
      handleExpandArrow(binding.rollCallDescriptionArrow, binding.rollCallDescriptionText)
    }

    // Set the location dropdown
    binding.rollCallLocationCard.setOnClickListener {
      handleExpandArrow(binding.rollCallLocationArrow, binding.rollCallLocationText)
    }

    binding.rollCallManagementButton.setOnClickListener {
      when (val state = rollCall.state) {
        EventState.CLOSED,
        EventState.CREATED ->
            laoViewModel.addDisposable(
                rollCallViewModel
                    .openRollCall(rollCall.id)
                    .subscribe(
                        { /* Here the fragment is reopened as we want to have continuity between
                           * the list of attendees and the list of scanned tokens for the organizer.
                           * By reopening the fragment, the roll call attendees will be immediately
                           * displayed also in the list of scanned tokens. */
                          setCurrentFragment(parentFragmentManager, R.id.fragment_roll_call) {
                            newInstance(rollCall.persistentId)
                          }
                        },
                        { error: Throwable ->
                          logAndShow(requireContext(), TAG, error, R.string.error_open_rollcall)
                        }))
        EventState.OPENED -> // will add the scan to this fragment in the future
        laoViewModel.addDisposable(
                rollCallViewModel
                    .closeRollCall(rollCall.id)
                    .subscribe(
                        {
                          setCurrentFragment(parentFragmentManager, R.id.fragment_event_list) {
                            EventListFragment.newInstance()
                          }
                        },
                        { error: Throwable ->
                          logAndShow(requireContext(), TAG, error, R.string.error_close_rollcall)
                        }))
        else -> error("Roll-Call should not be in a $state state")
      }
    }

    binding.rollCallScanningButton.setOnClickListener {
      setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
        newInstance(
            ScanningAction.ADD_ROLL_CALL_ATTENDEE, requireArguments().getString(ROLL_CALL_ID)!!)
      }
    }

    laoViewModel.addDisposable(
        rollCallViewModel
            .getRollCallObservable(rollCall.persistentId)
            .subscribe(
                { rc: RollCall ->
                  Timber.tag(TAG).d("Received rc update: %s", rc)
                  rollCall = rc
                  setUpStateDependantContent()
                },
                { error: Throwable ->
                  logAndShow(requireContext(), TAG, error, R.string.unknown_roll_call_exception)
                }))

    handleBackNav(TAG)

    return binding.root
  }

  override fun onResume() {
    super.onResume()

    setTab(R.string.roll_call_title)
    try {
      rollCall =
          rollCallRepo.getRollCallWithPersistentId(
              laoViewModel.laoId!!, requireArguments().getString(ROLL_CALL_ID)!!)
    } catch (e: UnknownRollCallException) {
      logAndShow(requireContext(), TAG, e, R.string.unknown_roll_call_exception)
    }
  }

  private val popToken: PoPToken?
    get() =
        try {
          laoViewModel.getCurrentPopToken(rollCall)
        } catch (e: KeyException) {
          logAndShow(requireContext(), TAG, e, R.string.key_generation_exception)
          null
        } catch (e: UnknownLaoException) {
          logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception)
          null
        }

  private fun setUpStateDependantContent() {
    setupTime(
        rollCall,
        binding.rollCallStartTime,
        binding
            .rollCallEndTime) // Suggested time is updated in case of early/late close/open/reopen
    val rcState = rollCall.state
    val isOrganizer = laoViewModel.isOrganizer
    binding.rollCallFragmentTitle.text = rollCall.name

    // Set the description and location visible if the QR is not displayed
    // (i.e. I'm the organizer or the roll call is open)
    if (rollCall.isOpen && !isOrganizer) {
      binding.rollCallMetadataContainer.visibility = View.GONE
    } else {
      binding.rollCallMetadataContainer.visibility = View.VISIBLE
      // Set the description invisible if it's empty
      if (rollCall.description.isEmpty()) {
        binding.rollCallDescriptionCard.visibility = View.GONE
      }
    }
    binding.rollCallLocationText.text = rollCall.location
    binding.rollCallDescriptionText.text = rollCall.description

    // Set visibility of management button as Gone by default
    binding.rollCallManagementButton.visibility = View.GONE

    // The management button is only visible to the organizer under the following conditions:
    if (isOrganizer) {
      // If the roll call is the last closed roll call or it's not closed (either opened or created)
      try {
        if (!rollCall.isClosed ||
            rollCallRepo.getLastClosedRollCall(laoViewModel.laoId!!) == rollCall) {
          binding.rollCallManagementButton.visibility = View.VISIBLE
        }
      } catch (ignored: NoRollCallException) {}
    }
    binding.rollCallManagementButton.setText(managementTextMap.getOrDefault(rcState, ID_NULL))
    val imgManagement =
        AppCompatResources.getDrawable(
            requireContext(), managementIconMap.getOrDefault(rcState, ID_NULL))
    binding.rollCallManagementButton.setCompoundDrawablesWithIntrinsicBounds(
        imgManagement, null, null, null)
    setStatus(rcState, binding.rollCallStatusIcon, binding.rollCallStatus)

    // Show scanning button only if the current state is Opened
    if (rcState === EventState.OPENED && isOrganizer) {
      binding.rollCallScanningButton.visibility = View.VISIBLE
    } else {
      binding.rollCallScanningButton.visibility = View.GONE
    }

    setupListOfAttendees()
    retrieveAndDisplayPublicKey()
    handleRotation()
  }

  /**
   * This function sets the visibility logic of both the header and the list of attendees/scanned
   * tokens, depending on the roll call state and whether the user is the organizer. The adapter of
   * the ViewList is set accordingly, as the proper content is displayed.
   */
  private fun setupListOfAttendees() {
    val isOrganizer = laoViewModel.isOrganizer
    // Set the visibility of the list:
    // It is set to visible only if the roll call is closed
    // Or also if the user is the organizer and roll call is opened
    // Otherwise do not display the list
    val visibility =
        if (rollCall.isClosed || isOrganizer && rollCall.isOpen) View.VISIBLE else View.INVISIBLE
    binding.rollCallAttendeesText.visibility = visibility
    binding.listViewAttendees.visibility = visibility

    var attendeesList: List<String>? = null
    if (isOrganizer && rollCall.isOpen) {
      // Show the list of all time scanned attendees if the roll call is opened
      // and the user is the organizer
      attendeesList =
          rollCallViewModel
              .getAttendees()
              .stream()
              .map(PublicKey::encoded)
              .sorted(compareBy(String::toString))
              .collect(Collectors.toList())

      binding.rollCallAttendeesText.text =
          String.format(
              resources.getString(R.string.roll_call_scanned),
              rollCallViewModel.getAttendees().size)
    } else if (rollCall.isClosed) {
      attendeesList =
          rollCall.attendees.stream().map(PublicKey::encoded).collect(Collectors.toList())

      // Show the list of attendees if the roll call has ended
      binding.rollCallAttendeesText.text =
          String.format(resources.getString(R.string.roll_call_attendees), rollCall.attendees.size)
    }

    if (attendeesList != null) {
      binding.listViewAttendees.adapter =
          RollCallArrayAdapter(
              requireContext(),
              android.R.layout.simple_list_item_1,
              attendeesList,
              popToken,
          )
    }
  }

  @SuppressLint("SourceLockedOrientationActivity")
  private fun handleRotation() {
    val activity = activity ?: return
    if (rollCall.isOpen && !laoViewModel.isOrganizer) {
      // If the qr is visible, then the activity rotation should be locked,
      // as the QR could not fit in the screen in landscape
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }
  }

  private fun retrieveAndDisplayPublicKey() {
    val popToken = popToken ?: return
    val pk = popToken.publicKey.encoded
    Timber.tag(TAG).d("key displayed is %s", pk)

    // Set the QR visible only if the rollcall is opened and the user isn't the organizer
    if (rollCall.isOpen) {
      binding.rollCallPopTokenText.text = pk
      binding.rollCallPkQrCode.visibility = View.VISIBLE
      binding.rollCallPopTokenText.visibility = View.VISIBLE
    } else {
      binding.rollCallPkQrCode.visibility = View.INVISIBLE
      binding.rollCallPopTokenText.visibility = View.INVISIBLE
    }

    // Don't lose time generating the QR code if it's not visible
    if (laoViewModel.isOrganizer || rollCall.isClosed) {
      return
    }

    val data = PopTokenData(PublicKey(pk))
    val myBitmap =
        QRCode.from(gson.toJson(data))
            .withColor(getQRCodeColor(requireContext()), Color.TRANSPARENT)
            .bitmap()
    binding.rollCallPkQrCode.setImageBitmap(myBitmap)
  }

  private fun buildManagementTextMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)
    map[EventState.CREATED] = R.string.open
    map[EventState.OPENED] = R.string.close
    map[EventState.CLOSED] = R.string.reopen_rollcall
    return map
  }

  private fun buildManagementIconMap(): EnumMap<EventState, Int> {
    val map = EnumMap<EventState, Int>(EventState::class.java)
    map[EventState.CREATED] = R.drawable.ic_unlock
    map[EventState.OPENED] = R.drawable.ic_lock
    map[EventState.CLOSED] = R.drawable.ic_unlock
    return map
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  constructor(rollCall: RollCall) {
    this.rollCall = rollCall
  }

  companion object {
    val TAG: String = RollCallFragment::class.java.simpleName
    private val deAnonymizationWarned = MutableLiveData(false)

    @JvmStatic
    fun newInstance(persistentId: String?): RollCallFragment {
      deAnonymizationWarned.value = false
      val fragment = RollCallFragment()
      val bundle = Bundle(1)
      bundle.putString(ROLL_CALL_ID, persistentId)
      fragment.arguments = bundle
      return fragment
    }

    fun isAttendeeListSorted(attendeesList: List<String>, context: Context): Boolean {
      if (attendeesList != attendeesList.sorted() && deAnonymizationWarned.value == false) {
        deAnonymizationWarned.value = true
        logAndShow(context, TAG, R.string.roll_call_attendees_list_not_sorted)
        return false
      }
      return true
    }

    /**
     * The following is only for testing purposes. Production should never pass arguments to a
     * fragment instantiation but should rather use arguments
     */
    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun newInstance(rollCall: RollCall): RollCallFragment {
      return RollCallFragment(rollCall)
    }
  }
}
