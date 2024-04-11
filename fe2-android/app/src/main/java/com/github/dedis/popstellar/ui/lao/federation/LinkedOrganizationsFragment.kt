package com.github.dedis.popstellar.ui.lao.federation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.LinkedOrganizationsFragmentBinding
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.rotateFab
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showIn
import com.github.dedis.popstellar.ui.lao.event.LaoDetailAnimation.showOut


/**
 * A simple [Fragment] subclass.
 * Use the [LinkedOrganizationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LinkedOrganizationsFragment : Fragment() {

    private lateinit var binding: LinkedOrganizationsFragmentBinding
    private lateinit var laoViewModel: LaoViewModel

    private var buttonClicked = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = LinkedOrganizationsFragmentBinding.inflate(inflater, container, false)
        laoViewModel = obtainViewModel(requireActivity())

        // Sets the text and the button depending on the user's role
        laoViewModel.role.observe(viewLifecycleOwner) {role: Role ->
            if(role == Role.ORGANIZER) {
                binding.noOrganizationsText.setText(R.string.no_organizations_organizer_text)
                binding.addLinkedOrganization.visibility = View.VISIBLE
            } else {
                binding.noOrganizationsText.setText(R.string.no_organizations_non_organizer_text)
                binding.addLinkedOrganization.visibility = View.GONE
            }
        }

        binding.addLinkedOrganization.setOnClickListener(observeButton)

        return binding.root
        //return inflater.inflate(R.layout.linked_organizations_fragment, container, false)
    }

    override fun onResume() {
        super.onResume()
        laoViewModel.setPageTitle(R.string.linked_organizations)
        laoViewModel.setIsTab(true)
    }

    //Changes the display when the + button is pressed
    private var observeButton =
            View.OnClickListener { v: View ->
                buttonClicked = rotateFab(v, !buttonClicked)
                if(buttonClicked) {
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

    companion object {
        @JvmStatic
        fun newInstance(): LinkedOrganizationsFragment {
            return LinkedOrganizationsFragment()
        }
    }
}