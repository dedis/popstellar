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


/**
 * A simple [Fragment] subclass.
 * Use the [LinkedOrganizationsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LinkedOrganizationsFragment : Fragment() {

    private lateinit var binding: LinkedOrganizationsFragmentBinding
    private lateinit var laoViewModel: LaoViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = LinkedOrganizationsFragmentBinding.inflate(inflater, container, false)
        laoViewModel = obtainViewModel(requireActivity())

        // Sets the text depending on the user's role
        laoViewModel.role.observe(viewLifecycleOwner) {role: Role ->
            binding.noOrganizationsText.setText(
                    if(role == Role.ORGANIZER) R.string.no_organizations_organizer_text
                    else R.string.no_organizations_non_organizer_text
            )
        }

        return binding.root
        //return inflater.inflate(R.layout.linked_organizations_fragment, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(): LinkedOrganizationsFragment {
            return LinkedOrganizationsFragment()
        }
    }
}