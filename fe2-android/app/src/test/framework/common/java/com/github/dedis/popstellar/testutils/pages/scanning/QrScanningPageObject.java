package com.github.dedis.popstellar.testutils.pages.scanning;

import androidx.test.espresso.ViewInteraction;

import com.github.dedis.popstellar.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class QrScanningPageObject {

    public static ViewInteraction manualAddConfirm(){
        return onView(withId(R.id.manual_add_confirm));
    }

    public static ViewInteraction manualAddEditText(){
        return onView(withId(R.id.manual_add_edit_text));
    }

    public static ViewInteraction attendeeCount(){
        return onView(withId(R.id.add_attendee_number_text));
    }
}
