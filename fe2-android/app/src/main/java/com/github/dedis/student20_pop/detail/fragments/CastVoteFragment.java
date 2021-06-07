package com.github.dedis.student20_pop.detail.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.student20_pop.databinding.FragmentCastVoteBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.detail.adapters.QuestionViewPagerAdapter;

import me.relex.circleindicator.CircleIndicator3;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CastVoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CastVoteFragment extends Fragment {


    private TextView laoNameView;
    private TextView electionNameView;
    private Button voteButton;
    private FragmentCastVoteBinding mCastVoteFragBinding;
    private LaoDetailViewModel mLaoDetailViewModel;


    private View.OnClickListener buttonListener = v -> {
        voteButton.setEnabled(false);
        // mLaoDetailViewModel.sendVote(election); This method is defined in Maxim's branch
    };



    public CastVoteFragment() {
        // Required empty public constructor
    }


    public static CastVoteFragment newInstance() {
        return new CastVoteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Inflate the layout for this fragment
        mCastVoteFragBinding =
                FragmentCastVoteBinding.inflate(inflater, container, false);
        mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

        laoNameView = mCastVoteFragBinding.castVoteLaoName;
        electionNameView = mCastVoteFragBinding.castVoteElectionName;

        //setUp the cast Vote button
        voteButton = mCastVoteFragBinding.castVoteButton;
        voteButton.setEnabled(false);

        //Getting election
      //  election = mLaoDetailViewModel.getCurrentElection();

        //Setting the Lao Name
        laoNameView.setText(mLaoDetailViewModel.getCurrentLaoName().getValue());

        //Setting election name
      //  electionNameView.setText(election.getName());


        ViewPager2 viewPager2 = mCastVoteFragBinding.castVotePager;
        QuestionViewPagerAdapter adapter = new QuestionViewPagerAdapter(mLaoDetailViewModel, mCastVoteFragBinding);
        viewPager2.setAdapter(adapter);

        //Setting the indicator for horizontal swipe
        CircleIndicator3 circleIndicator = mCastVoteFragBinding.swipeIndicator;
        circleIndicator.setViewPager(viewPager2);


        voteButton.setOnClickListener(buttonListener);
        return mCastVoteFragBinding.getRoot();
    }
}
