package com.github.dedis.student20_pop.detail.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;

import java.util.ArrayList;

public class ElectionResultListAdapter extends ArrayAdapter<ElectionResultListAdapter.ElectionResult> {

    private Context mContext;
    private int mResource;
    public ElectionResultListAdapter(Context context, int resource, ArrayList<ElectionResult> objects){
        super(context, resource, objects);
        this.mContext = context;
        this.mResource = resource;
    }




    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);
        String ballotOption = getItem(position).getBallotOption();
        TextView ballotView = (TextView) convertView.findViewById(R.id.election_result_ballot_option);
        ballotView.setText(ballotOption);
        TextView votesView = (TextView) convertView.findViewById(R.id.election_result_vote_number);
        String numberOfVotes = String.valueOf(getItem(position).getVotes());
        votesView.setText(numberOfVotes);
        return convertView;
    }

    public static class ElectionResult {
        private String ballotOption;
        private int votes;
        public ElectionResult(String ballotOption, int votes){
            this.ballotOption = ballotOption;
            this.votes = votes;
        }

        public String getBallotOption() {
            return ballotOption;
        }

        public void setBallotOption(String ballotOption) {
            if (ballotOption == null) throw new IllegalArgumentException();
            this.ballotOption = ballotOption;
        }

        public int getVotes() {
            return votes;
        }

        public void setVotes(int votes) {
            this.votes = votes;
        }
    }
}
