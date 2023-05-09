package com.github.dedis.popstellar.ui.lao.socialmedia;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownChirpException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import timber.log.Timber;

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

    // If the user has no valid pop token then it's not possible to react
    try {
      socialMediaViewModel.getValidPoPToken();
      chirpView.findViewById(R.id.chirp_card_buttons).setVisibility(View.VISIBLE);
    } catch (KeyException e) {
      chirpView.findViewById(R.id.chirp_card_buttons).setVisibility(View.GONE);
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
    ImageButton upvoteChirp = chirpView.findViewById(R.id.upvote_button);
    ImageButton downvoteChirp = chirpView.findViewById(R.id.downvote_button);
    ImageButton loveChirp = chirpView.findViewById(R.id.love_button);
    TextView upvoteCounter = chirpView.findViewById(R.id.upvote_counter);
    TextView downvoteCounter = chirpView.findViewById(R.id.downvote_counter);
    TextView loveCounter = chirpView.findViewById(R.id.love_counter);

    // Set dynamically the counter of reactions
    try {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .getReactions(chirp.getId())
              .subscribe(
                  reactions -> {
                    Map<String, Long> codepointToCountMap =
                        reactions.stream()
                            // Filter just non deleted reactions
                            .filter(((Predicate<Reaction>) Reaction::isDeleted).negate())
                            .collect(
                                Collectors.groupingBy(
                                    Reaction::getCodepoint, Collectors.counting()));
                    long upVotes =
                        codepointToCountMap.getOrDefault(Reaction.Emoji.UPVOTE.getUnicode(), 0l);
                    long downVotes =
                        codepointToCountMap.getOrDefault(Reaction.Emoji.DOWNVOTE.getUnicode(), 0l);
                    long loves =
                        codepointToCountMap.getOrDefault(Reaction.Emoji.LOVE.getUnicode(), 0l);

                    upvoteCounter.setText(String.format(Locale.US, "%d", upVotes));
                    downvoteCounter.setText(String.format(Locale.US, "%d", downVotes));
                    loveCounter.setText(String.format(Locale.US, "%d", loves));
                  },
                  err ->
                      ErrorUtils.logAndShow(context, TAG, err, R.string.unknown_chirp_exception)));
    } catch (UnknownChirpException e) {
      throw new IllegalArgumentException("The chirp does not exist");
    }

    // Set the selection of the button based on my own previous reactions
    upvoteChirp.setSelected(
        socialMediaViewModel.isReactionPresent(chirp.getId(), Reaction.Emoji.UPVOTE));
    setItemSelection(
        upvoteChirp, R.drawable.ic_social_media_upvote_selected, R.drawable.ic_social_media_upvote);
    downvoteChirp.setSelected(
        socialMediaViewModel.isReactionPresent(chirp.getId(), Reaction.Emoji.DOWNVOTE));
    setItemSelection(
        downvoteChirp,
        R.drawable.ic_social_media_downvote_selected,
        R.drawable.ic_social_media_downvote);
    loveChirp.setSelected(
        socialMediaViewModel.isReactionPresent(chirp.getId(), Reaction.Emoji.LOVE));
    setItemSelection(
        loveChirp, R.drawable.ic_social_media_love_selected, R.drawable.ic_social_media_love);

    // Set the listener for the reaction buttons
    upvoteChirp.setOnClickListener(
        v -> {
          reactionListener(
              upvoteChirp,
              R.drawable.ic_social_media_upvote_selected,
              R.drawable.ic_social_media_upvote,
              Reaction.Emoji.UPVOTE,
              chirp.getId());
          if (downvoteChirp.isSelected() && upvoteChirp.isSelected()) {
            // Remove the downvote reaction
            reactionListener(
                downvoteChirp,
                R.drawable.ic_social_media_downvote_selected,
                R.drawable.ic_social_media_downvote,
                Reaction.Emoji.DOWNVOTE,
                chirp.getId());
          }
        });

    downvoteChirp.setOnClickListener(
        v -> {
          reactionListener(
              downvoteChirp,
              R.drawable.ic_social_media_downvote_selected,
              R.drawable.ic_social_media_downvote,
              Reaction.Emoji.DOWNVOTE,
              chirp.getId());
          if (downvoteChirp.isSelected() && upvoteChirp.isSelected()) {
            // Remove the upvote reaction
            reactionListener(
                upvoteChirp,
                R.drawable.ic_social_media_upvote_selected,
                R.drawable.ic_social_media_upvote,
                Reaction.Emoji.UPVOTE,
                chirp.getId());
          }
        });

    loveChirp.setOnClickListener(
        v ->
            reactionListener(
                loveChirp,
                R.drawable.ic_social_media_love_selected,
                R.drawable.ic_social_media_love,
                Reaction.Emoji.LOVE,
                chirp.getId()));

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
      chirpView.findViewById(R.id.chirp_card_buttons).setVisibility(View.GONE);
      itemText.setTextColor(Color.GRAY);
    } else {
      text = chirp.getText();
    }

    itemUsername.setText(sender.getEncoded());
    itemTime.setText(getRelativeTimeSpanString(timestamp * 1000));
    itemText.setText(text);

    return chirpView;
  }

  private void reactionListener(
      ImageButton button,
      @DrawableRes int selected,
      @DrawableRes int notSelected,
      Reaction.Emoji emoji,
      @NonNull MessageID chirpId) {

    boolean selection = !button.isSelected();
    button.setSelected(selection);

    setItemSelection(button, selected, notSelected);

    if (selection) {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .sendReaction(emoji.getUnicode(), chirpId, Instant.now().getEpochSecond())
              .subscribe(
                  msg -> Timber.tag(TAG).d("Added reaction to chirp %s", chirpId),
                  err ->
                      ErrorUtils.logAndShow(context, TAG, err, R.string.error_sending_reaction)));
    } else {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .deleteReaction(chirpId, Instant.now().getEpochSecond(), emoji)
              .subscribe(
                  msg -> Timber.tag(TAG).d("Deleted reaction of chirp %s", chirpId),
                  err -> ErrorUtils.logAndShow(context, TAG, err, R.string.error_delete_reaction)));
    }
  }

  private void setItemSelection(
      ImageButton button, @DrawableRes int selected, @DrawableRes int notSelected) {
    boolean selection = button.isSelected();
    // Choose either the selected or not icon
    Drawable icon =
        selection
            ? ResourcesCompat.getDrawable(context.getResources(), selected, context.getTheme())
            : ResourcesCompat.getDrawable(context.getResources(), notSelected, context.getTheme());
    button.setImageDrawable(icon);
  }
}
