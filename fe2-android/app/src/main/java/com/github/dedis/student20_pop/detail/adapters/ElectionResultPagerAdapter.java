package com.github.dedis.student20_pop.detail.adapters;

import android.util.Log;
import android.view.LayoutInflater;
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
import java.util.Arrays;
import java.util.List;

public class ElectionResultPagerAdapter  extends RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder> {
    private LaoDetailViewModel mLaoDetailViewModel;
    private ElectionResultListAdapter adapter;
    private final String TAG = ElectionResultPagerAdapter.class.getSimpleName();
    ///// Setting up static data for testing //////////////////////////////////////////////////////
    List<String> questions = Arrays.asList("Who for 1st delegate", "Who for 2nd delegate");
    List<List<String>> ballotsOptions = Arrays.asList(Arrays.asList("A convincing first option", "Another too long proposition"), Arrays.asList("Fooo baar", "D", "E"));
    List<List<Integer>> voteLists = Arrays.asList(Arrays.asList(21, 6), Arrays.asList(2,7,9));

    public ElectionResultPagerAdapter(LaoDetailViewModel mLaoDetailViewModel){
        super();
        this.mLaoDetailViewModel = mLaoDetailViewModel;
    }

    @NonNull
    @Override
    public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        adapter = new ElectionResultListAdapter(parent.getContext(), R.layout.layout_election_result_listview, new ArrayList<>());
        return new Pager2ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_election_result_pager, parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
        Election election = mLaoDetailViewModel.getCurrentElection();
        if(election == null){
            Log.d(TAG, "Election is null");
        }
        else
            Log.d(TAG, election.getQuestions().get(0));
        if(election == null)
            election = new Election();

        //setting the question
        //String question = election.getQuestions().get(position);
        String question = questions.get(position);
        if(holder == null){
            Log.d(TAG, "Holder is null");
        }
        if(holder.questionView == null){
            Log.d(TAG, "questionView is null");
        }
        holder.questionView.setText(question);

//        List<String> ballotOptions = election.getBallotsOptions().get(position);
//        List<Integer> votes = election.getVotes().get(position);

        List<String> ballotOptions = ballotsOptions.get(position);
        List<Integer> votes = voteLists.get(position);

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
        return questions.size();
    }

    class Pager2ViewHolder extends RecyclerView.ViewHolder{
        private ListView resultListView;
        private TextView questionView;
        public Pager2ViewHolder (View itemView){
            super(itemView);
            resultListView = (ListView) itemView.findViewById(R.id.election_result_listView);
            questionView = (TextView) itemView.findViewById(R.id.election_result_question);
            if(questionView == null){
                Log.d(TAG, "questionView is null in viewHolder");
            }
        }
    }

}
