package com.github.dedis.popstellar.ui.socialmedia;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.List;

public class ChirpListAdapter extends BaseAdapter {

  private final Context context;
  private List<Chirp> chirps;
  private SocialMediaViewModel socialMediaViewModel;
  private LayoutInflater layoutInflater;

  public ChirpListAdapter(
      Context context, List<Chirp> chirps, SocialMediaViewModel socialMediaViewModel) {
    this.context = context;
    this.chirps = chirps;
    this.socialMediaViewModel = socialMediaViewModel;
    layoutInflater = LayoutInflater.from(context);
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
  public View getView(int position, View view, ViewGroup viewGroup) {

    view = layoutInflater.inflate(R.layout.chirp_card, null);

    Chirp chirp = getItem(position);
    PublicKey publicKey = chirp.getSender();
    long timestamp = chirp.getTimestamp();
    String text;
    if (chirp.getIsDeleted()) {
      text = "Chirp is deleted.";
    } else {
      text = chirp.getText();
    }

    TextView itemUsername = view.findViewById(R.id.social_media_username);
    itemUsername.setText(publicKey.getEncoded());

    TextView itemTime = view.findViewById(R.id.social_media_time);
    itemTime.setText(getRelativeTimeSpanString(timestamp));

    TextView itemText = view.findViewById(R.id.social_media_text);
    itemText.setText(text);

    return view;
  }
}
