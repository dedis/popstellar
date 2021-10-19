package com.github.dedis.popstellar.ui.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.matcher.BoundedMatcher;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class HomeFragmentTest {

  /**
   * Matches a Lao with a given lao name
   *
   * @param title
   * @return
   */
  public static Matcher<Object> titleMatches(String title) {
    return new BoundedMatcher<Object, Lao>(Lao.class) {

      @Override
      public void describeTo(Description description) {
        description.appendText("with title " + title + "'");
      }

      @Override
      protected boolean matchesSafely(Lao item) {
        return item.getName().equals(title);
      }
    };
  }

  @Test
  public void homeFragmentIsDisplayed() {
    onView(withId(R.id.fragment_home)).check(matches(isDisplayed()));
  }

  @Test
  public void listOfLaosIsDisplayed() {
    onView(withId(R.id.lao_list)).check(matches(isDisplayed()));
  }
}
