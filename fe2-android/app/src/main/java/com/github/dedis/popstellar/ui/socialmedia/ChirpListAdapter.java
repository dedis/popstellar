package com.github.dedis.popstellar.ui.socialmedia;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.time.Instant;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

public class ChirpListAdapter extends BaseAdapter {

  private static final String TAG = ChirpListAdapter.class.getSimpleName();

  private final SocialMediaViewModel socialMediaViewModel;
  private final Context context;
  private final LayoutInflater layoutInflater;
  private final CompositeDisposable disposables = new CompositeDisposable();
  private List<Chirp> chirps;

  public ChirpListAdapter(
      FragmentActivity activity, SocialMediaViewModel socialMediaViewModel, List<Chirp> chirps) {
    this.context = activity;
    this.socialMediaViewModel = socialMediaViewModel;
    this.chirps = chirps;
    layoutInflater = LayoutInflater.from(activity);

    activity
        .getLifecycle()
        .addObserver(
            (LifecycleEventObserver)
                (source, event) -> {
                  if (event.getTargetState() == Lifecycle.State.DESTROYED) {
                    disposables.dispose();
                  }
                });
  }

  public void replaceList(List<Chirp> chirps) {
    this.chirps = chirps;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return chirps != null ? chirps.size() : 0;
  }

  @Override
  public Chirp getItem(int position) {
    return chirps.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View chirpView, ViewGroup viewGroup) {
    if (chirpView == null) {
      chirpView = layoutInflater.inflate(R.layout.chirp_card, viewGroup);
    }

    Chirp chirp = getItem(position);
    if (chirp == null) {
      throw new IllegalArgumentException("The chirp does not exist");
    }
    PublicKey publicKey = chirp.getSender();
    long timestamp = chirp.getTimestamp();
    String text;

    TextView itemUsername = chirpView.findViewById(R.id.social_media_username);
    TextView itemTime = chirpView.findViewById(R.id.social_media_time);
    TextView itemText = chirpView.findViewById(R.id.social_media_text);

    if (socialMediaViewModel.isOwner(publicKey.getEncoded())) {
      ImageButton deleteChirp = chirpView.findViewById(R.id.delete_chirp_button);
      deleteChirp.setVisibility(View.VISIBLE);
      deleteChirp.setOnClickListener(
          v ->
              disposables.add(
                  socialMediaViewModel
                      .deleteChirp(chirp.getId(), Instant.now().getEpochSecond())
                      .subscribe(
                          msg ->
                              Toast.makeText(context, "Deleted chirp!", Toast.LENGTH_LONG).show(),
                          error ->
                              ErrorUtils.logAndShow(
                                  context, TAG, error, R.string.error_delete_chirp))));
    }

    if (chirp.getIsDeleted()) {
      text = "Chirp is deleted.";
      ImageButton deleteChirp = chirpView.findViewById(R.id.delete_chirp_button);
      deleteChirp.setVisibility(View.GONE);
      itemText.setTextColor(Color.GRAY);
    } else {
      text = chirp.getText();
    }

    itemUsername.setText(publicKey.getEncoded());
    itemTime.setText(getRelativeTimeSpanString(timestamp * 1000));
    itemText.setText(text);

    return chirpView;
  }
}
