package com.github.dedis.popstellar.testutils.pages.detail.event.election;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.github.dedis.popstellar.testutils.MatcherUtils.childAtPosition;
import static org.hamcrest.Matchers.allOf;

public class ElectionSetupPageObject {

  private ElectionSetupPageObject() {
    throw new IllegalStateException("Page object");
  }

  public static ViewInteraction electionName() {
    return onView(
        allOf(
            withId(R.id.election_setup_name),
            withHint(R.string.election_setup_name_hint),
            isDisplayed()));
  }

  public static ViewInteraction addBallot() {
    return onView(
        allOf(
            withId(R.id.add_ballot_option),
            withText(R.string.add_ballot_options_button_text),
            isClickable(),
            isDisplayed()));
  }

  public static ViewInteraction questionText() {
    return onView(
        allOf(
            withId(R.id.election_question),
            withHint(R.string.election_question_hint),
            isDisplayed()));
  }

  public static ViewInteraction ballotOptionAtPosition(int i) {
    return onView(
        allOf(
            withParent(childAtPosition(withId(R.id.election_setup_ballot_options_ll), i)),
            withId(R.id.new_ballot_option_text),
            isDisplayed()));
  }

  public static ViewInteraction addQuestion() {
    return onView(
        allOf(
            withId(R.id.add_question),
            withText(R.string.add_question_button_text),
            isClickable(),
            isDisplayed()));
  }

  public static ViewInteraction writeIn() {
    return onView(allOf(withId(R.id.write_in), isClickable(), isDisplayed()));
  }

  public static ViewInteraction submit() {
    return onView(withId(R.id.election_submit_button));
  }

  public static ViewInteraction versionChoice() {
    return onView(allOf(withId(R.id.election_setup_mode_spinner), isClickable(), isDisplayed()));
  }
}
