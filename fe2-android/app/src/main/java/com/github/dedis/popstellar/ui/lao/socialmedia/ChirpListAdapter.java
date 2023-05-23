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
import java.util.stream.Collectors;

import timber.log.Timber;

import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static com.github.dedis.popstellar.model.objects.Reaction.ReactionEmoji.*;

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

    // If the user has no valid pop token then it's not possible to react
    // (make invisible the buttons)
    try {
      socialMediaViewModel.getValidPoPToken();
      chirpView.findViewById(R.id.chirp_card_buttons).setVisibility(View.VISIBLE);
    } catch (KeyException e) {
      chirpView.findViewById(R.id.chirp_card_buttons).setVisibility(View.GONE);
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
    ImageButton heartChirp = chirpView.findViewById(R.id.heart_button);
    TextView upvoteCounter = chirpView.findViewById(R.id.upvote_counter);
    TextView downvoteCounter = chirpView.findViewById(R.id.downvote_counter);
    TextView heartCounter = chirpView.findViewById(R.id.heart_counter);

    // Set dynamically the reaction buttons selection and counter
    try {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .getReactions(chirp.getId())
              // Each time the observable changes the counter and the selection is notified
              .subscribe(
                  reactions -> {
                    // Map the reactions to the chirp into <codepoint, list of senders' pk>
                    Map<String, Set<String>> codepointToSendersMap =
                        reactions.stream()
                            // Filter just non deleted reactions
                            .filter(reaction -> !reaction.isDeleted())
                            // Then collect by emoji type and count the occurrences
                            .collect(
                                Collectors.groupingBy(
                                    Reaction::getCodepoint,
                                    Collectors.mapping(
                                        reaction -> reaction.getSender().getEncoded(),
                                        Collectors.toSet())));

                    // Extract the number of reactions by emoji
                    Set<String> upVotes =
                        codepointToSendersMap.getOrDefault(UPVOTE.getCode(), new HashSet<>(0));
                    Set<String> downVotes =
                        codepointToSendersMap.getOrDefault(DOWNVOTE.getCode(), new HashSet<>(0));
                    Set<String> hearts =
                        codepointToSendersMap.getOrDefault(HEART.getCode(), new HashSet<>(0));

                    upvoteCounter.setText(String.format(Locale.US, "%d", upVotes.size()));
                    downvoteCounter.setText(String.format(Locale.US, "%d", downVotes.size()));
                    heartCounter.setText(String.format(Locale.US, "%d", hearts.size()));

                    // As retrieving the database state is asynchronous, the selection can be
                    // slightly delayed, so we shall observe this observable to set the selection
                    upvoteChirp.setSelected(socialMediaViewModel.isReactionPresent(upVotes));
                    downvoteChirp.setSelected(socialMediaViewModel.isReactionPresent(downVotes));
                    heartChirp.setSelected(socialMediaViewModel.isReactionPresent(hearts));

                    // Based on the selection of the buttons choose the correct drawable
                    setItemSelection(
                        upvoteChirp,
                        R.drawable.ic_social_media_upvote_selected,
                        R.drawable.ic_social_media_upvote);
                    setItemSelection(
                        downvoteChirp,
                        R.drawable.ic_social_media_downvote_selected,
                        R.drawable.ic_social_media_downvote);
                    setItemSelection(
                        heartChirp,
                        R.drawable.ic_social_media_heart_selected,
                        R.drawable.ic_social_media_heart);
                  },
                  err ->
                      ErrorUtils.logAndShow(context, TAG, err, R.string.unknown_chirp_exception)));
    } catch (UnknownChirpException e) {
      throw new IllegalArgumentException("The chirp does not exist");
    }

    setupReactionButtons(chirp.getId(), upvoteChirp, downvoteChirp, heartChirp);

    // Show the delete button only if the user is the owner of the chirp
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

    // If the chirp has been deleted display a special text and hide the rest
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

  /**
   * Function to setup the three reactions buttons (upvote, downvote and heart). It sets the buttons
   * selection, their relative drawables and listeners.
   *
   * @param chirpId identifier of the chirp to which reacting
   * @param upvoteChirp button for upvote reaction
   * @param downvoteChirp button for downvote reaction
   * @param heartChirp button for heart reaction
   */
  private void setupReactionButtons(
      MessageID chirpId,
      ImageButton upvoteChirp,
      ImageButton downvoteChirp,
      ImageButton heartChirp) {
    // Set the listener for the upvote button to add and delete reaction
    upvoteChirp.setOnClickListener(
        v -> {
          reactionListener(upvoteChirp, UPVOTE, chirpId);
          // Implement the exclusivity of upvote and downvote (i.e. disable downvote if upvote)
          if (downvoteChirp.isSelected() && upvoteChirp.isSelected()) {
            reactionListener(downvoteChirp, DOWNVOTE, chirpId);
          }
        });

    // Set the listener for the downvote button to add and delete reaction
    downvoteChirp.setOnClickListener(
        v -> {
          reactionListener(downvoteChirp, DOWNVOTE, chirpId);
          // Implement the exclusivity of upvote and downvote (i.e. disable upvote if downvote)
          if (downvoteChirp.isSelected() && upvoteChirp.isSelected()) {
            reactionListener(upvoteChirp, UPVOTE, chirpId);
          }
        });

    // Set the listener for the heart button to add and delete reaction
    heartChirp.setOnClickListener(v -> reactionListener(heartChirp, HEART, chirpId));
  }

  /**
   * Function that sets the listener of the reaction buttons by sending the correct message.
   *
   * @param button reaction button pressed
   * @param emoji type of reaction
   * @param chirpId chirp to which react
   */
  private void reactionListener(
      ImageButton button, Reaction.ReactionEmoji emoji, @NonNull MessageID chirpId) {
    // Invert the selection
    boolean selection = !button.isSelected();

    // Send the proper message
    if (selection) {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .sendReaction(emoji.getCode(), chirpId, Instant.now().getEpochSecond())
              .subscribe(
                  msg -> Timber.tag(TAG).d("Added reaction to chirp %s", chirpId),
                  err ->
                      ErrorUtils.logAndShow(context, TAG, err, R.string.error_sending_reaction)));
    } else {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .deleteReaction(chirpId, Instant.now().getEpochSecond(), emoji.getCode())
              .subscribe(
                  msg -> Timber.tag(TAG).d("Deleted reaction of chirp %s", chirpId),
                  err -> ErrorUtils.logAndShow(context, TAG, err, R.string.error_delete_reaction)));
    }
  }

  /**
   * This function selects the correct drawable for the button based on its selection status.
   *
   * @param button button for which changing the drawable
   * @param selected drawable resource for the selected status
   * @param notSelected drawable resource for the not selected status
   */
  private void setItemSelection(
      ImageButton button, @DrawableRes int selected, @DrawableRes int notSelected) {
    boolean selection = button.isSelected();
    // Choose either the selected or not selected icon
    Drawable icon =
        selection
            ? ResourcesCompat.getDrawable(context.getResources(), selected, context.getTheme())
            : ResourcesCompat.getDrawable(context.getResources(), notSelected, context.getTheme());
    button.setImageDrawable(icon);
  }
}
