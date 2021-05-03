/* package com.github.dedis.student20_pop.detail;


import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class LaoDetailViewModelTest {

    private LaoDetailViewModel mLaoDetailViewModel;

    @Rule
    public ActivityScenarioRule rule = new ActivityScenarioRule<>(LaoDetailActivity.class);

    @Before
    public void obtainingViewModel() {
        rule.getScenario().onActivity(activity -> {
            mLaoDetailViewModel = LaoDetailActivity.obtainViewModel((FragmentActivity) activity);
        });

    }

    @Test
    public void creatingNewElection() {
        mLaoDetailViewModel.createNewElection("my election", 0, 1, "Plurality", false,
                Arrays.asList("candidate1", "candidate2"), "my question");
        //TDO: test with backend
    }
} */
