package com.github.dedis.popstellar.ui.detail.event.election.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.dedis.popstellar.R;

import java.util.List;

public class ElectionResultListAdapter
    extends ArrayAdapter<ElectionResultListAdapter.ElectionResult> {

  private final Context mContext;
  private final int mResource;

  public ElectionResultListAdapter(Context context, int resource, List<ElectionResult> objects) {
    super(context, resource, objects);
    this.mContext = context;
    this.mResource = resource;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(mContext);
    View view = inflater.inflate(mResource, parent, false);
    String ballotOption = getItem(position).getBallotOption();
    TextView ballotView = view.findViewById(R.id.election_result_ballot_option);
    ballotView.setText(ballotOption);
    TextView votesView = view.findViewById(R.id.election_result_vote_number);
    String numberOfVotes = String.valueOf(getItem(position).getVotes());
    votesView.setText(numberOfVotes);
    return view;
  }

  public static class ElectionResult {

    private String ballotOption;
    private int votes;

    public ElectionResult(String ballotOption, int votes) {
      this.ballotOption = ballotOption;
      this.votes = votes;
    }

    public String getBallotOption() {
      return ballotOption;
    }

    public void setBallotOption(String ballotOption) {
      if (ballotOption == null) {
        throw new IllegalArgumentException();
      }
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
