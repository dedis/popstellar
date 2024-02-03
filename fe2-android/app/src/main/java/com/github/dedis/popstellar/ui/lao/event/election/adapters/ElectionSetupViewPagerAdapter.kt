package com.github.dedis.popstellar.ui.lao.event.election.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.CastVoteBallotOptionLayoutBinding
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionSetupFragment.VotingMethods
import java.util.Arrays
import java.util.stream.Collectors
import timber.log.Timber

/** This is where whe define behaviour of the ViewPager of election setup */
class ElectionSetupViewPagerAdapter :
    RecyclerView.Adapter<ElectionSetupViewPagerAdapter.ViewHolder>() {
  private val votingMethod: MutableList<String> = ArrayList()
  private val ballotOptions: MutableList<MutableList<String>> = ArrayList()
  private val numberBallotOptions: MutableList<Int> = ArrayList()
  private val questions: MutableList<String> = ArrayList()
  var numberOfQuestions = 0
    private set

  private val listOfValidQuestions: MutableSet<Int> = HashSet()
  private val listOfValidBallots: MutableSet<Int> = HashSet()
  @JvmField val isAnInputValid: MutableLiveData<Boolean> = MutableLiveData(java.lang.Boolean.FALSE)
  private lateinit var context: Context

  init {
    addQuestion()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    context = parent.context

    return ViewHolder(
        LayoutInflater.from(context)
            .inflate(R.layout.election_setup_question_layout, parent, false))
  }

  override fun getItemViewType(position: Int): Int {
    return position
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    // The holder represents the view of one page of the pager

    // This is bad practice and should be removed in the future
    // The problem for now is that reused view messes up the data intake
    holder.setIsRecyclable(false)
    val electionQuestionText = holder.electionQuestionText
    ballotOptions[position] = ArrayList()
    electionQuestionText.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            /* no check to make before text is changed */
          }

          override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            /* no check to make before text is changed */
          }

          override fun afterTextChanged(s: Editable) {
            // On each text change we edit the list of questions
            // and we add or remove the question from the list of filled question
            val questionText = s.toString()

            // Prevents the user from creating two different questions with the same name
            if (questionText.isNotEmpty() && questions.contains(questionText)) {
              electionQuestionText.error =
                  context.getString(R.string.error_election_duplicate_question_name)
            }
            if (electionQuestionText.text.toString().trim { it <= ' ' }.isNotEmpty()) {
              listOfValidQuestions.add(holder.adapterPosition)
            } else {
              listOfValidQuestions.remove(holder.adapterPosition)
            }

            questions[holder.adapterPosition] = questionText
            checkIfAnInputIsValid()
          }
        })

    // Setting the spinner and its listener
    val spinner = holder.spinner
    val spinnerListener: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(parent: AdapterView<*>, view: View, i: Int, id: Long) {
            val elementToAdd = parent.getItemAtPosition(i).toString()
            if (votingMethod.size <= holder.adapterPosition) {
              votingMethod.add(elementToAdd)
            }
            votingMethod[holder.adapterPosition] = elementToAdd
          }

          override fun onNothingSelected(parent: AdapterView<*>?) {
            votingMethod[holder.adapterPosition] = context.getString(R.string.voting_method)
          }
        }

    setupElectionSpinner(spinner, spinnerListener)
    val addBallotOptionButton = holder.addOptionButton
    val linearLayout = holder.linearLayout
    addBallotOptionButton.setOnClickListener {
      addBallotOption(linearLayout, holder.adapterPosition)
    }

    // Minimum for each question is two ballots
    addBallotOption(linearLayout, position)
    addBallotOption(linearLayout, position)
  }

  /** On each question we add, we expand the lists appropriately */
  fun addQuestion() {
    numberOfQuestions++
    votingMethod.add("")
    questions.add("")
    ballotOptions.add(ArrayList())
    numberBallotOptions.add(0)
    notifyItemInserted(numberOfQuestions - 1)
  }

  override fun getItemCount(): Int {
    // This defines the number of page we will have
    return numberOfQuestions
  }

  /**
   * Checks if an input is valid by crosschecking the list of election questions where the question
   * and the ballots are filled
   */
  fun checkIfAnInputIsValid() {
    for (ballotsList in ballotOptions) {
      // Check if there is a duplicate ballot, if true invalidate the input
      for (i in ballotsList.indices) {
        val ballot = ballotsList[i]
        if (ballotsList.subList(i + 1, ballotsList.size).contains(ballot)) {
          isAnInputValid.value = false
          return
        }
      }
    }

    // Check if there's a duplicate question, if true invalidate the input
    if (hasDuplicate(questions)) {
      isAnInputValid.value = false
      return
    }

    for (i in listOfValidQuestions) {
      if (listOfValidBallots.contains(i)) {
        isAnInputValid.value = true
        return
      }
    }

    isAnInputValid.value = false
  }

  val validInputs: List<Int>
    /** @return the indexes of election question where input is valid */
    get() =
        listOfValidQuestions
            .stream()
            .filter { o: Int -> listOfValidBallots.contains(o) }
            .collect(Collectors.toList())

  /** Adds a view of ballot option to the layout when user clicks the button */
  private fun addBallotOption(linearLayout: LinearLayout, position: Int) {
    // Adds the view for a new ballot option, from the corresponding layout
    val ballotOptionView: View =
        CastVoteBallotOptionLayoutBinding.inflate(LayoutInflater.from(context)).newBallotOptionLl
    val ballotOptionText = ballotOptionView.findViewById<EditText>(R.id.new_ballot_option_text)
    linearLayout.addView(ballotOptionView)

    // Gets the index associated to the ballot option's view
    val ballotIndex = linearLayout.indexOfChild(ballotOptionView)
    val questionBallotOption: List<String> = ballotOptions[position]
    ballotOptions[position].add("")
    ballotOptionText.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            /* no check to make before text is changed */
          }

          override fun onTextChanged(
              charSequence: CharSequence,
              start: Int,
              before: Int,
              count: Int
          ) {
            /*no check to make during the text is being changed */
          }

          override fun afterTextChanged(editable: Editable) {
            val text = editable.toString()
            // Prevents the user from creating two different ballot options with the same name
            if (text.isNotEmpty() && questionBallotOption.contains(text)) {
              ballotOptionText.error =
                  context.getString(R.string.error_election_duplicate_ballot_name)
            }
            // Counts the number of non-empty ballot options, to know when the user can create the
            // election (at least 2 non-empty)
            if (questionBallotOption.isEmpty() ||
                questionBallotOption[ballotIndex].isEmpty() && text.isNotEmpty()) {
              numberBallotOptions[position] = numberBallotOptions[position] + 1
            } else if (questionBallotOption[ballotIndex].isNotEmpty() && text.isEmpty()) {
              numberBallotOptions[position] = numberBallotOptions[position] - 1
            }
            // Keeps the list of string updated when the user changes the text
            ballotOptions[position][ballotIndex] = editable.toString()
            Timber.tag(TAG).d("Position is %s ballot options are %s", position, ballotOptions)

            val areFieldsFilled = numberBallotOptions[position] >= 2
            if (areFieldsFilled) {
              listOfValidBallots.add(position)
            } else {
              listOfValidBallots.remove(position)
            }
            checkIfAnInputIsValid()
          }
        })
  }

  private fun setupElectionSpinner(spinner: Spinner, listener: AdapterView.OnItemSelectedListener) {
    val items = Arrays.stream(VotingMethods.values()).map(VotingMethods::desc).toArray()
    val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)

    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner.adapter = adapter
    spinner.onItemSelectedListener = listener
  }

  fun getVotingMethod(): List<String> {
    return votingMethod
  }

  fun getBallotOptions(): List<MutableList<String>> {
    return ballotOptions
  }

  fun getQuestions(): List<String> {
    return questions
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val electionQuestionText: EditText = itemView.findViewById(R.id.election_question)
    val spinner: Spinner = itemView.findViewById(R.id.election_setup_spinner)
    val addOptionButton: Button = itemView.findViewById(R.id.add_ballot_option)
    val linearLayout: LinearLayout = itemView.findViewById(R.id.election_setup_ballot_options_ll)
  }

  companion object {
    val TAG: String = ElectionSetupViewPagerAdapter::class.java.simpleName

    /**
     * This function checks if a given list of strings has at least a duplicate. The comparison
     * performed is case-sensitive.
     *
     * @param strings List of Strings
     * @return true if the list of strings has at least a duplicate, false otherwise
     */
    fun hasDuplicate(strings: List<String>): Boolean {
      val uniqueStrings: MutableSet<String> = HashSet()
      return strings.stream().anyMatch { s: String -> !uniqueStrings.add(s) }
    }
  }
}
