package com.github.dedis.student20_pop.detail.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.model.Election;

import java.util.ArrayList;
import java.util.List;

public class ElectionResultPagerAdapter  extends RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder> {
    private LaoDetailViewModel mLaoDetailViewModel;
    private ElectionResultListAdapter adapter;
    public ElectionResultPagerAdapter(LaoDetailViewModel mLaoDetailViewModel){
        super();
        this.mLaoDetailViewModel = mLaoDetailViewModel;
    }

    @NonNull
    @Override
    public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        adapter = new ElectionResultListAdapter(parent.getContext(), R.layout.layout_election_result_listview, new ArrayList<>());
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
        Election election = mLaoDetailViewModel.getCurrentElection();

        //setting the question
        String question = election.getQuestions().get(position);
        holder.questionView.setText(question);

        List<String> ballotOptions = election.getBallotsOptions().get(position);
        List<Integer> votes = election.getVotes().get(position);

        List<ElectionResultListAdapter.ElectionResult> electionResults = new ArrayList<>();
        for(int i = 0; i< ballotOptions.size(); i++){
            electionResults.add(new ElectionResultListAdapter.ElectionResult(ballotOptions.get(i), votes.get(i)));
        }
        adapter.clear();
        adapter.addAll(electionResults);

        holder.resultListView.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class Pager2ViewHolder extends RecyclerView.ViewHolder{
        private ListView resultListView;
        private TextView questionView;
        public Pager2ViewHolder (View itemView){
            super(itemView);
            resultListView = itemView.findViewById(R.id.election_result_listView);
            questionView = itemView.findViewById(R.id.election_result_question);
        }
    }

}
