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
import com.github.dedis.student20_pop.model.network.method.message.data.ElectionQuestion;
import com.github.dedis.student20_pop.model.network.method.message.data.QuestionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElectionResultPagerAdapter  extends RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder> {
    private LaoDetailViewModel mLaoDetailViewModel;
    private ElectionResultListAdapter adapter;
    private final String TAG = ElectionResultPagerAdapter.class.getSimpleName();
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


        //setting the question
        ElectionQuestion electionQuestion = election.getElectionQuestions().get(position);
        String question = electionQuestion.getQuestion();
        if(holder == null){
            Log.d(TAG, "Holder is null");
        }
        if(holder.questionView == null){
            Log.d(TAG, "questionView is null");
        }
        holder.questionView.setText(question);


      List<QuestionResult> questionResults = election.getResultsForQuestionId(electionQuestion.getId());
        Map<String, List<QuestionResult>>results= election.getResults();
        Log.d(TAG, "whole "+results);
        Log.d(TAG, "Keys are "+results.keySet());
        Log.d(TAG,"Manually "+election.getResultsForQuestionId(electionQuestion.getId()).get(0).getBallot());
        Log.d(TAG, electionQuestion.getId());
        List<String> ballotOptions = questionResults.stream().map(QuestionResult::getBallot).collect(Collectors.toList());
        Log.d(TAG, ""+ballotOptions);
        List<Integer>votes = questionResults.stream().map(QuestionResult::getCount).collect(Collectors.toList());
        Log.d(TAG, ""+votes);


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
        return mLaoDetailViewModel.getCurrentElection().getElectionQuestions().size();
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
