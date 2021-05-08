package com.github.dedis.student20_pop.detail.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionViewPagerAdapter extends RecyclerView.Adapter<QuestionViewPagerAdapter.Pager2ViewHolder>{


    List<List<String>> ballotsOptions = Arrays.asList(
            Arrays.asList("Alan Turing", "John von Neumann", "Claude Shannon", "Linus Torvalds", "Ken Thompson", "Tim Berners-Lee", "Charles Babbage", "Barbara Liskov", "Ronald Rivest", "Adi Shamir", "Len Adleman"),
            Arrays.asList("a", "b", "c", "d"));
    List<String> questions = Arrays.asList("Favourite C.S guy ?", "Best letter ?");
    private List<Integer> numbersOfChoices;
    private  final String VOTES_FULL = "You cannot select more candidates";
    private ArrayAdapter<String> ballotAdapter;
    private LaoDetailViewModel mLaoDetailViewModel;
    private List<List<Integer>> allVotes;
    public QuestionViewPagerAdapter (LaoDetailViewModel mLaoDetailViewModel){
        super();

        this.numbersOfChoices = Arrays.asList(3,1);
        this.mLaoDetailViewModel = mLaoDetailViewModel;
        allVotes = Arrays.asList(new ArrayList<>(), new ArrayList<>());
    }
    @NonNull
    @Override
    public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ballotAdapter = new ArrayAdapter<>(parent.getContext(), android.R.layout.simple_list_item_single_choice, new ArrayList<>());
        return new Pager2ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_cast_vote_listview,parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
        System.out.println("Changed page " + position);
//        ElectionQuestion electionQuestion = mLaoDetailViewModel.getCurrentElection().getElectionQuestions().get(position);
//        Election election = mLaoDetailViewModel.getCurrentElection();
//        String question = electionQuestion.getQuestion();
//        List<String> ballotOptions = electionQuestion.getBallotOptions();
        List<String> ballotOptions = ballotsOptions.get(position);
        String question = questions.get(position);
        holder.questionView.setText(question);
        //todo change when implemented in the setup
        int numberOfChoices = numbersOfChoices.get(position);
        ballotAdapter.clear();
        ballotAdapter.addAll(ballotOptions);
        ListView lvBallots = holder.ballotsListView;
        List<Integer> votes =allVotes.get(position);
//        List<Integer> votes = election.getVotes().get(position);
        lvBallots.setAdapter(ballotAdapter);
        lvBallots.setChoiceMode(
                numberOfChoices > 1
                        ? AbsListView.CHOICE_MODE_MULTIPLE
                        : AbsListView.CHOICE_MODE_SINGLE);


        AdapterView.OnItemClickListener itemListener = (parent, view, listPosition, id) -> {
            lvBallots.setClickable(false);
            if (numberOfChoices > 1) {
                if (votes.contains(listPosition)) {
                    votes.remove((Integer)listPosition);
                    lvBallots.setItemChecked(listPosition, false);
                } else if (votes.size() < numberOfChoices) {
                    votes.add(listPosition);
                    lvBallots.setItemChecked(listPosition, true);
                } else {
                    lvBallots.setItemChecked(listPosition, false);
                    //    Toast.makeText(context, VOTES_FULL, Toast.LENGTH_LONG).show();
                }
            } else {
                System.out.println("Size of votes is " + votes.size());
                System.out.println(votes);
                if (votes.contains( listPosition)) {
                    votes.clear();
                    lvBallots.setItemChecked(listPosition, false);
                } else {
                    votes.clear();
                    votes.add(listPosition);
                }
            }
            lvBallots.setClickable(true);
            //   voteButton.setEnabled(votes.size() == numberOfChoices);
        };
        lvBallots.setOnItemClickListener(itemListener);
    }

    @Override
    public int getItemCount() {
        return questions.size();// mLaoDetailViewModel.getCurrentElection().getElectionQuestions().size();
    }
    class Pager2ViewHolder extends RecyclerView.ViewHolder{
        private ListView ballotsListView;
        private TextView questionView;
        public Pager2ViewHolder (View itemView){
            super(itemView);
            ballotsListView = itemView.findViewById(R.id.list_view_pager2);
            questionView = itemView.findViewById(R.id.cast_vote_question);
        }
    }


}
