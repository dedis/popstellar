package com.github.dedis.popstellar.ui.lao.event;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.inject.Inject;

import static com.github.dedis.popstellar.utility.Constants.ID_NULL;

public abstract class AbstractEventFragment extends Fragment {

  protected @Inject Gson gson;

  protected LaoViewModel laoViewModel;

  private final EnumMap<EventState, Integer> statusTextMap = buildStatusTextMap();
  private final EnumMap<EventState, Integer> statusIconMap = buildStatusIconMap();
  private final EnumMap<EventState, Integer> statusColorMap = buildStatusColorMap();

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("dd/MM/yyyy HH:mm z", Locale.ENGLISH);

  protected void setTab(@StringRes int pageTitle) {
    laoViewModel.setIsTab(false);
    laoViewModel.setPageTitle(pageTitle);
  }

  protected void setupTime(Event event, TextView startTime, TextView endTime) {
    if (event == null) {
      return;
    }
    Date start = new Date(event.getStartTimestampInMillis());
    Date end = new Date(event.getEndTimestampInMillis());

    startTime.setText(dateFormat.format(start));
    endTime.setText(dateFormat.format(end));
  }

  protected void setStatus(EventState state, ImageView statusIcon, TextView statusText) {
    Drawable imgStatus = getDrawableFromContext(statusIconMap.getOrDefault(state, ID_NULL));
    statusIcon.setImageDrawable(imgStatus);
    setImageColor(statusIcon, statusColorMap.getOrDefault(state, ID_NULL));

    statusText.setText(statusTextMap.getOrDefault(state, ID_NULL));
    statusText.setTextColor(
        getResources().getColor(statusColorMap.getOrDefault(state, ID_NULL), null));
  }

  private Drawable getDrawableFromContext(int id) {
    return AppCompatResources.getDrawable(requireContext(), id);
  }

  private void setImageColor(ImageView imageView, int colorId) {
    ImageViewCompat.setImageTintList(imageView, getResources().getColorStateList(colorId, null));
  }

  private EnumMap<EventState, Integer> buildStatusTextMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.string.created_displayed_text);
    map.put(EventState.OPENED, R.string.open);
    map.put(EventState.CLOSED, R.string.closed);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusIconMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.drawable.ic_lock);
    map.put(EventState.OPENED, R.drawable.ic_unlock);
    map.put(EventState.CLOSED, R.drawable.ic_lock);
    return map;
  }

  private EnumMap<EventState, Integer> buildStatusColorMap() {
    EnumMap<EventState, Integer> map = new EnumMap<>(EventState.class);
    map.put(EventState.CREATED, R.color.red);
    map.put(EventState.OPENED, R.color.green);
    map.put(EventState.CLOSED, R.color.red);
    return map;
  }

  protected void handleBackNav(String tag) {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), getViewLifecycleOwner(), tag);
  }
}
