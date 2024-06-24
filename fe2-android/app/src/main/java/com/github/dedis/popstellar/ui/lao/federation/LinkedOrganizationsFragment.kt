package com.github.dedis.popstellar.ui.lao.federation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LinkedOrganizationsFragmentBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainLinkedOrganizationsViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.rotateFab
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showIn
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showOut
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment
import com.github.dedis.popstellar.ui.qrcode.ScanningAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass. Use the [LinkedOrganizationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LinkedOrganizationsFragment : Fragment() {

  private lateinit var binding: LinkedOrganizationsFragmentBinding
  private lateinit var laoViewModel: LaoViewModel
  private lateinit var linkedOrganizationsViewModel: LinkedOrganizationsViewModel

  private var buttonClicked = false

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    // Inflate the layout for this fragment
    binding = LinkedOrganizationsFragmentBinding.inflate(inflater, container, false)
    laoViewModel = obtainViewModel(requireActivity())
    linkedOrganizationsViewModel =
        obtainLinkedOrganizationsViewModel(requireActivity(), laoViewModel.laoId)

    // Sets the text and the button depending on the user's role
    laoViewModel.role.observe(viewLifecycleOwner) { role: Role ->
      if (role == Role.ORGANIZER) {
        binding.noOrganizationsText.setText(R.string.no_organizations_organizer_text)
        binding.addLinkedOrganization.visibility = View.VISIBLE
      } else {
        binding.noOrganizationsText.setText(R.string.no_organizations_non_organizer_text)
        binding.addLinkedOrganization.visibility = View.GONE
      }
    }

    binding.addLinkedOrganization.setOnClickListener(observeButton)
    binding.inviteOtherOrganization.setOnClickListener(invitationPage)
    binding.joinOtherOrganizationInvitation.setOnClickListener(joinButton)

    // Displaying the linked organizations
    val laos = linkedOrganizationsViewModel.getLinkedLaosMap().keys
    displayLinkedOrganizations(laos)
    linkedOrganizationsViewModel.doWhenLinkedLaosIsUpdated { laoMap ->
      CoroutineScope(Dispatchers.Main).launch {
        val currentLaos = laoMap.keys
        displayLinkedOrganizations(currentLaos)
      }
    }

    linkedOrganizationsViewModel.setLinkedLaosNotifyFunction()
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.linked_organizations)
    laoViewModel.setIsTab(true)
  }

  // Changes the display when the + button is pressed
  private var observeButton =
      View.OnClickListener { v: View ->
        buttonClicked = rotateFab(v, !buttonClicked)
        if (buttonClicked) {
          showIn(binding.inviteOtherOrganization)
          showIn(binding.inviteOtherOrganizationText)
          showIn(binding.joinOtherOrganizationInvitation)
          showIn(binding.joinOtherOrganizationInvitationText)
        } else {
          showOut(binding.inviteOtherOrganization)
          showOut(binding.inviteOtherOrganizationText)
          showOut(binding.joinOtherOrganizationInvitation)
          showOut(binding.joinOtherOrganizationInvitationText)
        }
      }

  private var invitationPage =
      View.OnClickListener {
        LaoActivity.setCurrentFragment(
            parentFragmentManager, R.id.fragment_linked_organizations_invite) {
              LinkedOrganizationsInviteFragment.newInstance(true)
            }
      }

  private var joinButton =
      View.OnClickListener {
        linkedOrganizationsViewModel.flushRepository()
        laoViewModel.setIsTab(false)
        linkedOrganizationsViewModel.manager = parentFragmentManager
        LaoActivity.setCurrentFragment(parentFragmentManager, R.id.fragment_qr_scanner) {
          QrScannerFragment.newInstance(ScanningAction.FEDERATION_JOIN)
        }
      }

  private fun displayLinkedOrganizations(laos: Set<String>) {
    if (laos.isNotEmpty()) {
      val laosText = laos.joinToString(separator = "\n\n")
      val textToDisplay = context?.getString(R.string.list_organizations, laosText)
      binding.noOrganizationsText.visibility = View.GONE
      binding.listOrganizationsText.visibility = View.VISIBLE
      binding.listOrganizationsText.text = textToDisplay
    } else {
      binding.listOrganizationsText.visibility = View.GONE
      binding.noOrganizationsText.visibility = View.VISIBLE
    }
  }

  private fun handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    private val TAG: String = LinkedOrganizationsInviteFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): LinkedOrganizationsFragment {
      return LinkedOrganizationsFragment()
    }
  }
}
