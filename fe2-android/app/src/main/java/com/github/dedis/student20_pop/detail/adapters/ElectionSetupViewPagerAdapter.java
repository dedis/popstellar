package com.github.dedis.student20_pop.detail.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.LayoutBallotOptionBinding;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.detail.fragments.event.creation.ElectionSetupFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElectionSetupViewPagerAdapter
        extends RecyclerView.Adapter<ElectionSetupViewPagerAdapter.ViewHolder>
       {

    public static final String TAG = ElectionSetupViewPagerAdapter.class.getSimpleName();
    LaoDetailViewModel mLaoDetailViewModel;
    private List<String> votingMethod;
    private List<List<String>> ballotOptions;
    private List<Integer> numberBallotOptions;
    private List<String> questions;
    private int numberOfQuestions;
    private Context context;
    private List<Integer> listOfValidQuestions;
    private List<Integer> listOfValidBallots;
    //Enum of all voting methods, associated to a string desc for protocol and spinner display
    public enum VotingMethods { PLURALITY("Plurality");
        private String desc;
        VotingMethods(String desc) { this.desc=desc; }
        public String getDesc() { return desc; }
    }

    private void expandAllLists(int position){
        if (ballotOptions.size() <= position){
            votingMethod.add("");
            questions.add("");
            ballotOptions.add(new ArrayList<>());
            numberBallotOptions.add(0);
        }
    }

    public ElectionSetupViewPagerAdapter(LaoDetailViewModel mLaoDetailViewModel) {
        super();
        this.mLaoDetailViewModel = mLaoDetailViewModel;
        numberOfQuestions = 1;
        votingMethod = new ArrayList<>();
        ballotOptions = new ArrayList<>();
        numberBallotOptions = new ArrayList<>();
        questions = new ArrayList<>();
        listOfValidBallots = new ArrayList<>();
        listOfValidQuestions = new ArrayList<>();

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_election_setup_question, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "Size of ballots is " + ballotOptions.size());

        expandAllLists(position);
        EditText electionQuestionText = holder.electionQuestionText;

        Log.d(TAG, "Size of ballots is afterwards " + ballotOptions.size());
        ballotOptions.set(position, new ArrayList<>());
        electionQuestionText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String questionText = s.toString();
                if(!electionQuestionText.getText().toString().trim().isEmpty()){
                    questions.set(position, questionText);
                    listOfValidQuestions.add((Integer) position);
                }
                else
                    listOfValidQuestions.remove((Integer) position);


            }
        });
        Spinner spinner = holder.spinner;
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                System.out.println("VotingMethod size is " + votingMethod.size() + "Other is " + parent.getCount() + " i is " + i);
                String elementToAdd = parent.getItemAtPosition(i).toString();
                if(votingMethod.size() <= position){
                    votingMethod.add(elementToAdd);
                }
                votingMethod.set(position, elementToAdd);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                votingMethod.set(position, "Plurality");
            }
        };
        setupElectionSpinner(spinner, spinnerListener);

        FloatingActionButton addBallotOptionButton = holder.addOptionButton;

        LinearLayout linearLayout = holder.linearLayout;
        if(linearLayout == null)
            System.out.println("Linear layout is null");
        addBallotOptionButton.setOnClickListener(v -> addBallotOption(linearLayout, position));
        addBallotOption(linearLayout, position);
        addBallotOption(linearLayout, position);

    }

    public void addQuestion(){
        numberOfQuestions++;
        expandAllLists(numberOfQuestions - 1);
        notifyItemChanged(numberOfQuestions-1);
    }

    @Override
    public int getItemCount() {
        return numberOfQuestions;
    }

    public boolean isAnInputValid(){
        for(Integer i : listOfValidQuestions){
            if (listOfValidBallots.contains(i))
                return true;
        }
        return false;
    }

    public List<Integer> getValidInputs(){
        List<Integer> listOfValidInputs = new ArrayList<>();
        for (Integer i : listOfValidQuestions){
            if(listOfValidBallots.contains(i))
                listOfValidInputs.add(i);
        }
        return listOfValidInputs;
    }

    /**
     * Adds a view of ballot option to the layout when user clicks the button
     */
    private void addBallotOption(LinearLayout linearLayout, int position) {
        //Adds the view for a new ballot option, from the corresponding layout
        View ballotOptionView = LayoutBallotOptionBinding.inflate(LayoutInflater.from(context)).newBallotOptionLl;
        EditText ballotOptionText = ballotOptionView.findViewById(R.id.new_ballot_option_text);
        linearLayout.addView(ballotOptionView);

        //Gets the index associated to the ballot option's view
        int ballotIndex = linearLayout.indexOfChild(ballotOptionView);
        List<String> questionBallotOption = ballotOptions.get(position);
        ballotOptionText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {/* no check to make before text is changed */}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) { /*no check to make during the text is being changed */}
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                //Prevents the user from creating two different ballot options with the same name
                if (!text.isEmpty() && questionBallotOption.contains(text)) ballotOptionText.setError("Two different ballot options can't have the same name");
                //Counts the number of non-empty ballot options, to know when the user can create the election (at least 2 non-empty)
                if (questionBallotOption.isEmpty() || (questionBallotOption.get(ballotIndex).isEmpty() && !text.isEmpty()))
                    numberBallotOptions.set(position, numberBallotOptions.get(position) + 1);
                else if (!questionBallotOption.get(ballotIndex).isEmpty() && text.isEmpty())
                    numberBallotOptions.set(position, numberBallotOptions.get(position) - 1);
                //Keeps the list of string updated when the user changes the text
                ballotOptions.get(position).set(ballotIndex, editable.toString());
            }
        });
        TextWatcher submitTextWatcher = new TextWatcher() {
            //Text watcher that checks if mandatory fields are filled for submitting each time the user changes a field (with at least two valid ballot options)

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/* no check to make before text is changed */}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {/* no check to make during the text is being changes */}

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "ballot options is" + ballotOptions.toString());
                boolean areFieldsFilled =
                        numberBallotOptions.get(position) >= 2;
                if (areFieldsFilled)
                    listOfValidBallots.add((Integer) position);
                else
                    listOfValidBallots.remove((Integer) position);
            }
        };
        ballotOptionText.addTextChangedListener(submitTextWatcher);

        ballotOptions.get(position).add(ballotOptionText.getText().toString());
    }




    private void setupElectionSpinner(Spinner spinner, AdapterView.OnItemSelectedListener listener) {

        String[] items = Arrays.stream(ElectionSetupFragment.VotingMethods.values()).map(ElectionSetupFragment.VotingMethods::getDesc).toArray(String[]::new);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(listener);
    }






    public static class ViewHolder extends RecyclerView.ViewHolder {
        private EditText electionQuestionText;
        private Spinner spinner;
        private FloatingActionButton addOptionButton;
        private LinearLayout linearLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            electionQuestionText = itemView.findViewById(R.id.election_question);
            spinner = itemView.findViewById(R.id.election_setup_spinner);
            addOptionButton = itemView.findViewById(R.id.add_ballot_option);
            linearLayout = itemView.findViewById(R.id.election_setup_ballot_options_ll);
        }
    }
}
