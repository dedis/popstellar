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

        //Test with text TODO remove
        laoViewModel.role.observe(viewLifecycleOwner) {role: Role ->
            binding.testText.setText(
                    if(role == Role.ORGANIZER) "Welcome to linked org. You're the organizer"
                    else "Welcome to linked org. You're not the organiser"
            )
        }

        return binding.root
        //return inflater.inflate(R.layout.linked_organizations_fragment, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LinkedOrganizationsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(): LinkedOrganizationsFragment {
            return LinkedOrganizationsFragment()
        }
    }
}