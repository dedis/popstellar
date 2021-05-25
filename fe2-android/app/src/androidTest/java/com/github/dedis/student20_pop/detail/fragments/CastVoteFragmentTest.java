package com.github.dedis.student20_pop.detail.fragments;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.github.dedis.student20_pop.detail.LaoDetailActivity;

import org.junit.Before;
import org.junit.Rule;

import static androidx.test.espresso.Espresso.onView;
import static org.hamcrest.Matchers.allOf;

public class CastVoteFragmentTest {
    private View decorView;
    @Rule
    public ActivityScenarioRule<LaoDetailActivity> activityActivityScenarioRule =
            new ActivityScenarioRule<LaoDetailActivity>(LaoDetailActivity.class);

    @Before
    public void setUp(){
        activityActivityScenarioRule
                .getScenario()
                .onActivity(
                        new ActivityScenario.ActivityAction<LaoDetailActivity>() {
                            @Override
                            public void perform(LaoDetailActivity activity) {
                                decorView = activity.getWindow().getDecorView();
                            }
                        }
                );
        onView(
            allOf(

            )
        );
    }
}
