package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.time.Instant;
import java.util.List;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

public class ChirpListAdapter extends BaseAdapter {

  private static final String TAG = ChirpListAdapter.class.getSimpleName();

  private final LaoViewModel laoViewModel;
  private final SocialMediaViewModel socialMediaViewModel;
  private final Context context;
  private final LayoutInflater layoutInflater;
  private List<Chirp> chirps;

  public ChirpListAdapter(
      Context context, SocialMediaViewModel socialMediaViewModel, LaoViewModel viewModel) {
    this.context = context;
    this.socialMediaViewModel = socialMediaViewModel;
    this.laoViewModel = viewModel;

    layoutInflater = LayoutInflater.from(context);
    viewModel.addDisposable(
        socialMediaViewModel
            .getChirps()
            .subscribe(
                this::replaceList,
                err -> ErrorUtils.logAndShow(context, TAG, err, R.string.unknown_chirp_exception)));
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
      chirpView = layoutInflater.inflate(R.layout.chirp_card, viewGroup, false);
    }

    Chirp chirp = getItem(position);
    if (chirp == null) {
      throw new IllegalArgumentException("The chirp does not exist");
    }
    PublicKey sender = chirp.getSender();
    long timestamp = chirp.getTimestamp();
    String text;

    TextView itemUsername = chirpView.findViewById(R.id.social_media_username);
    TextView itemTime = chirpView.findViewById(R.id.social_media_time);
    TextView itemText = chirpView.findViewById(R.id.social_media_text);
    ImageButton deleteChirp = chirpView.findViewById(R.id.delete_chirp_button);

    if (socialMediaViewModel.isOwner(sender.getEncoded())) {
      deleteChirp.setVisibility(View.VISIBLE);
      deleteChirp.setOnClickListener(
          v ->
              laoViewModel.addDisposable(
                  socialMediaViewModel
                      .deleteChirp(chirp.getId(), Instant.now().getEpochSecond())
                      .subscribe(
                          msg ->
                              Toast.makeText(context, R.string.deleted_chirp, Toast.LENGTH_LONG)
                                  .show(),
                          error ->
                              ErrorUtils.logAndShow(
                                  context, TAG, error, R.string.error_delete_chirp))));
    } else {
      deleteChirp.setVisibility(View.GONE);
    }

    if (chirp.isDeleted()) {
      text = context.getString(R.string.deleted_chirp_2);
      deleteChirp.setVisibility(View.GONE);
      itemText.setTextColor(Color.GRAY);
    } else {
      text = chirp.getText();
    }

    itemUsername.setText(sender.getEncoded());
    itemTime.setText(getRelativeTimeSpanString(timestamp * 1000));
    itemText.setText(text);

    return chirpView;
  }
}
