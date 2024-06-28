package com.github.dedis.popstellar.ui.lao.socialmedia

import android.content.Context
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.Reaction.ReactionEmoji
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownChirpException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.time.Instant
import java.util.Locale
import java.util.stream.Collectors
import timber.log.Timber

class ChirpListAdapter(
    private val context: Context,
    private val socialMediaViewModel: SocialMediaViewModel,
    private val laoViewModel: LaoViewModel
) : BaseAdapter() {
  private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
  private var chirps: List<Chirp>? = null
  private val disposables = CompositeDisposable()

  init {
    laoViewModel.addDisposable(
        socialMediaViewModel.chirps.subscribe(
            { chirps: List<Chirp> -> replaceList(chirps) },
            { err: Throwable -> logAndShow(context, TAG, err, R.string.unknown_chirp_exception) }))
  }

  fun replaceList(chirps: List<Chirp>?) {
    // Dispose of previous observables
    disposables.clear()

    this.chirps = chirps
    notifyDataSetChanged()
  }

  override fun getCount(): Int {
    return chirps?.size ?: 0
  }

  override fun getItem(position: Int): Chirp? {
    return chirps?.get(position)
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getView(position: Int, chirpView: View?, viewGroup: ViewGroup): View {
    val view = chirpView ?: layoutInflater.inflate(R.layout.chirp_card, viewGroup, false)

    val chirp = getItem(position) ?: throw IllegalArgumentException("The chirp does not exist")

    // If the user has no valid pop token then it's not possible to react
    // (make invisible the buttons)
    try {
      socialMediaViewModel.validPoPToken
      view.findViewById<View>(R.id.chirp_card_buttons).visibility = View.VISIBLE
    } catch (e: KeyException) {
      view.findViewById<View>(R.id.chirp_card_buttons).visibility = View.GONE
    }

    // Dispose of previous observables for the chirp at this position
    val previousDisposable = view.getTag(R.id.chirp_card_buttons) as Disposable?
    previousDisposable?.dispose()

    val sender = chirp.sender
    val senderUsername = sender.getLabel()
    val timestamp = chirp.timestamp
    val text: String
    val itemUsername = view.findViewById<TextView>(R.id.social_media_username)
    val itemTime = view.findViewById<TextView>(R.id.social_media_time)
    val itemText = view.findViewById<TextView>(R.id.social_media_text)
    val itemProfile = view.findViewById<ImageView>(R.id.social_media_profile)
    val deleteChirp = view.findViewById<ImageButton>(R.id.delete_chirp_button)
    val upvoteChirp = view.findViewById<ImageButton>(R.id.upvote_button)
    val downvoteChirp = view.findViewById<ImageButton>(R.id.downvote_button)
    val heartChirp = view.findViewById<ImageButton>(R.id.heart_button)
    val upvoteCounter = view.findViewById<TextView>(R.id.upvote_counter)
    val downvoteCounter = view.findViewById<TextView>(R.id.downvote_counter)
    val heartCounter = view.findViewById<TextView>(R.id.heart_counter)

    // Set dynamically the reaction buttons selection and counter
    try {
      val reactionDisposable =
          socialMediaViewModel
              .getReactions(
                  chirp.laoId,
                  chirp.id) // Each time the observable changes the counter and the selection is
              // notified
              .subscribe(
                  { reactions: Set<Reaction> ->
                    // Map the reactions to the chirp into <codepoint, list of senders' pk>
                    val codepointToSendersMap =
                        reactions
                            .stream() // Filter just non deleted reactions
                            .filter { reaction: Reaction ->
                              !reaction.isDeleted
                            } // Then collect by emoji type and count the occurrences
                            .collect(
                                Collectors.groupingBy(
                                    Reaction::codepoint,
                                    Collectors.mapping(
                                        { reaction: Reaction -> reaction.sender.encoded },
                                        Collectors.toSet())))

                    // Extract the number of reactions by emoji
                    val upVotes = codepointToSendersMap[ReactionEmoji.UPVOTE.code] ?: HashSet(0)
                    val downVotes = codepointToSendersMap[ReactionEmoji.DOWNVOTE.code] ?: HashSet(0)
                    val hearts = codepointToSendersMap[ReactionEmoji.HEART.code] ?: HashSet(0)

                    upvoteCounter.text = String.format(Locale.US, "%d", upVotes.size)
                    downvoteCounter.text = String.format(Locale.US, "%d", downVotes.size)
                    heartCounter.text = String.format(Locale.US, "%d", hearts.size)

                    // As retrieving the database state is asynchronous, the selection can be
                    // slightly delayed, so we shall observe this observable to set the selection
                    upvoteChirp.isSelected = socialMediaViewModel.isReactionPresent(upVotes)
                    downvoteChirp.isSelected = socialMediaViewModel.isReactionPresent(downVotes)
                    heartChirp.isSelected = socialMediaViewModel.isReactionPresent(hearts)

                    // Based on the selection of the buttons choose the correct drawable
                    setItemSelection(
                        upvoteChirp,
                        R.drawable.ic_social_media_upvote_selected,
                        R.drawable.ic_social_media_upvote)
                    setItemSelection(
                        downvoteChirp,
                        R.drawable.ic_social_media_downvote_selected,
                        R.drawable.ic_social_media_downvote)
                    setItemSelection(
                        heartChirp,
                        R.drawable.ic_social_media_heart_selected,
                        R.drawable.ic_social_media_heart)
                  },
                  { err: Throwable ->
                    logAndShow(context, TAG, err, R.string.unknown_chirp_exception)
                  })
      // Store the disposable as a tag
      view.setTag(R.id.chirp_card_buttons, reactionDisposable)
      disposables.add(reactionDisposable)
    } catch (e: UnknownChirpException) {
      throw IllegalArgumentException("The chirp does not exist")
    }

    setupReactionButtons(chirp.id, upvoteChirp, downvoteChirp, heartChirp)

    // Show the delete button only if the user is the owner of the chirp
    if (socialMediaViewModel.isOwner(sender.encoded)) {
      deleteChirp.visibility = View.VISIBLE
      deleteChirp.setOnClickListener {
        laoViewModel.addDisposable(
            socialMediaViewModel
                .deleteChirp(chirp.id, Instant.now().epochSecond)
                .subscribe(
                    { Toast.makeText(context, R.string.deleted_chirp, Toast.LENGTH_LONG).show() },
                    { error: Throwable ->
                      logAndShow(context, TAG, error, R.string.error_delete_chirp)
                    }))
      }
    } else {
      deleteChirp.visibility = View.GONE
    }

    // If the chirp has been deleted display a special text and hide the rest
    if (chirp.isDeleted) {
      text = context.getString(R.string.deleted_chirp_2)
      view.findViewById<View>(R.id.chirp_card_buttons).visibility = View.GONE
      itemText.setTextColor(Color.GRAY)
    } else {
      text = chirp.text
    }

    itemUsername.text = senderUsername
    itemTime.text = DateUtils.getRelativeTimeSpanString(timestamp * 1000)
    itemText.text = text

    // Changes the color of the profile if the chirps comes from another LAO
    if (chirp.laoId != laoViewModel.laoId) {
      itemProfile.imageTintList = context.getColorStateList(R.color.gray)
    }

    return view
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
  private fun setupReactionButtons(
      chirpId: MessageID,
      upvoteChirp: ImageButton,
      downvoteChirp: ImageButton,
      heartChirp: ImageButton
  ) {
    // Set the listener for the upvote button to add and delete reaction
    upvoteChirp.setOnClickListener {
      // Implement the exclusivity of upvote and downvote (i.e. disable downvote if upvote)
      if (downvoteChirp.isSelected && !upvoteChirp.isSelected) {
        reactionListener(downvoteChirp, ReactionEmoji.DOWNVOTE, chirpId)
      }
      reactionListener(upvoteChirp, ReactionEmoji.UPVOTE, chirpId)
    }

    // Set the listener for the downvote button to add and delete reaction
    downvoteChirp.setOnClickListener {
      // Implement the exclusivity of upvote and downvote (i.e. disable upvote if downvote)
      if (!downvoteChirp.isSelected && upvoteChirp.isSelected) {
        reactionListener(upvoteChirp, ReactionEmoji.UPVOTE, chirpId)
      }
      reactionListener(downvoteChirp, ReactionEmoji.DOWNVOTE, chirpId)
    }

    // Set the listener for the heart button to add and delete reaction
    heartChirp.setOnClickListener { reactionListener(heartChirp, ReactionEmoji.HEART, chirpId) }
  }

  /**
   * Function that sets the listener of the reaction buttons by sending the correct message.
   *
   * @param button reaction button pressed
   * @param emoji type of reaction
   * @param chirpId chirp to which react
   */
  private fun reactionListener(button: ImageButton, emoji: ReactionEmoji, chirpId: MessageID) {
    // Invert the selection
    val selection = !button.isSelected

    // Send the proper message
    if (selection) {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .sendReaction(emoji.code, chirpId, Instant.now().epochSecond)
              .subscribe(
                  { Timber.tag(TAG).d("Added reaction to chirp %s", chirpId) },
                  { err: Throwable ->
                    logAndShow(context, TAG, err, R.string.error_sending_reaction)
                  }))
    } else {
      laoViewModel.addDisposable(
          socialMediaViewModel
              .deleteReaction(chirpId, Instant.now().epochSecond, emoji.code)
              .subscribe(
                  { Timber.tag(TAG).d("Deleted reaction of chirp %s", chirpId) },
                  { err: Throwable ->
                    logAndShow(context, TAG, err, R.string.error_delete_reaction)
                  }))
    }
  }

  /**
   * This function selects the correct drawable for the button based on its selection status.
   *
   * @param button button for which changing the drawable
   * @param selected drawable resource for the selected status
   * @param notSelected drawable resource for the not selected status
   */
  private fun setItemSelection(
      button: ImageButton,
      @DrawableRes selected: Int,
      @DrawableRes notSelected: Int
  ) {
    val selection = button.isSelected

    // Choose either the selected or not selected icon
    val icon =
        if (selection) ResourcesCompat.getDrawable(context.resources, selected, context.theme)
        else ResourcesCompat.getDrawable(context.resources, notSelected, context.theme)

    button.setImageDrawable(icon)
  }

  companion object {
    private val TAG = ChirpListAdapter::class.java.simpleName
  }
}
