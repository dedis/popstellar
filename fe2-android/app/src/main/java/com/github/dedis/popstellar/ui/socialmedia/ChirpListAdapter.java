package com.github.dedis.popstellar.ui.socialmedia;

import static android.text.format.DateUtils.getRelativeTimeSpanString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.List;
import java.util.Map;

public class ChirpListAdapter extends BaseAdapter {

  private final Context context;
  private SocialMediaViewModel socialMediaViewModel;
  private List<MessageID> chirpsId;
  private Map<MessageID, Chirp> allChirps;
  private LayoutInflater layoutInflater;

  public ChirpListAdapter(
      Context context,
      SocialMediaViewModel socialMediaViewModel,
      List<MessageID> chirpsId,
      Map<MessageID, Chirp> allChirps) {
    this.context = context;
    this.socialMediaViewModel = socialMediaViewModel;
    this.chirpsId = chirpsId;
    this.allChirps = allChirps;
    layoutInflater = LayoutInflater.from(context);
  }

  public void replaceList(List<MessageID> chirpsId) {
    this.chirpsId = chirpsId;
    notifyDataSetChanged();
  }

  public void replaceMap(Map<MessageID, Chirp> allChirps) {
    this.allChirps = allChirps;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return chirpsId != null ? chirpsId.size() : 0;
  }

  @Override
  public MessageID getItem(int position) {
    return chirpsId.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @SuppressLint({"ViewHolder", "InflateParams"})
  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    view = layoutInflater.inflate(R.layout.chirp_card, null);

    Chirp chirp = allChirps.get(getItem(position));
    if (chirp == null) {
      throw new IllegalArgumentException("The chirp does not exist");
    }
    PublicKey publicKey = chirp.getSender();
    long timestamp = chirp.getTimestamp();
    String text;

    TextView itemUsername = view.findViewById(R.id.social_media_username);
    TextView itemTime = view.findViewById(R.id.social_media_time);
    TextView itemText = view.findViewById(R.id.social_media_text);

    if (socialMediaViewModel.isOwner(publicKey.getEncoded())) {
      ImageButton deleteChirp = view.findViewById(R.id.delete_chirp_button);
      deleteChirp.setVisibility(View.VISIBLE);
      deleteChirp.setOnClickListener(v -> socialMediaViewModel.deleteChirpEvent(chirp.getId()));
    }

    if (chirp.getIsDeleted()) {
      text = "Chirp is deleted.";
      ImageButton deleteChirp = view.findViewById(R.id.delete_chirp_button);
      deleteChirp.setVisibility(View.GONE);
      itemText.setTextColor(Color.GRAY);
    } else {
      text = chirp.getText();
    }

    itemUsername.setText(publicKey.getEncoded());
    itemTime.setText(getRelativeTimeSpanString(timestamp * 1000));
    itemText.setText(text);

    return view;
  }
}
