package com.github.dedis.popstellar.ui.detail.event;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;

public class EventViewHolder extends RecyclerView.ViewHolder {
  private final TextView eventTitle;
  private final ImageView eventIcon;
  private final CardView eventCard;

  public EventViewHolder(@NonNull View itemView) {
    super(itemView);
    eventTitle = itemView.findViewById(R.id.event_card_text_view);
    eventIcon = itemView.findViewById(R.id.event_type_image);
    eventCard = itemView.findViewById(R.id.event_card_view);
  }

  public TextView getEventTitle() {
    return eventTitle;
  }

  public ImageView getEventIcon() {
    return eventIcon;
  }

  public CardView getEventCard() {
    return eventCard;
  }
}
