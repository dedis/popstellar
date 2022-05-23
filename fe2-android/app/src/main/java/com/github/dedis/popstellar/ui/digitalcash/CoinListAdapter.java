package com.github.dedis.popstellar.ui.digitalcash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class CoinListAdapter extends BaseAdapter {
  private final DigitalCashViewModel digitalCashViewModel;
  private List<PublicKey> receivers;
  private final LayoutInflater layoutInflater;

  public CoinListAdapter(
      Context context, DigitalCashViewModel digitalCashViewModel, List<PublicKey> receivers) {
    this.digitalCashViewModel = digitalCashViewModel;
    this.receivers = receivers;
    layoutInflater = LayoutInflater.from(context);
  }

  public void replaceList(List<PublicKey> publicKeys) {
    this.receivers = publicKeys;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return receivers != null ? receivers.size() : 0;
  }

  @Override
  public PublicKey getItem(int position) {
    return receivers.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View coinView, ViewGroup viewGroup) {
    if (coinView == null) {
      coinView = layoutInflater.inflate(R.layout.coin_card, null);
    }

    PublicKey publicKey = getItem(position);
    if (publicKey == null) {
      throw new IllegalArgumentException("The coin does not exist");
    }

    TextView itemUsername = coinView.findViewById(R.id.attendee_username);
    itemUsername.setText(publicKey.getEncoded());
    EditText e = coinView.findViewById(R.id.attendee_amount);

    Button button = coinView.findViewById(R.id.attendee_send);
    button.setOnClickListener(
        v -> {
          if (e.getText() != null) {
            digitalCashViewModel.postTransaction(
                publicKey.getEncoded(),
                Collections.singletonList(Integer.getInteger(e.getText().toString())),
                Instant.now().getEpochSecond());
          }
        });
    return coinView;
  }
}
