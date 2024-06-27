package com.github.dedis.popstellar.utility

import android.net.Uri
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.election.Vote
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.qrcode.PoPCHAQRCode
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionSetupFragment
import java.time.Instant
import java.util.Arrays
import java.util.regex.Pattern

/** Helper class to verify the validity of Data objects at their creation. */
object MessageValidator {
  /** URL-safe base64 pattern */
  private val BASE64_PATTERN =
      Pattern.compile("^(?:[A-Za-z0-9-_]{4})*(?:[A-Za-z0-9-_]{2}==|[A-Za-z0-9-_]{3}=)?$")
  private val URL_PATTERN = Pattern.compile("\\b(?:http|ws)s?:\\/\\/\\S*[^\\s.\"]")

  @JvmStatic
  fun verify(): MessageValidatorBuilder {
    return MessageValidatorBuilder()
  }

  class MessageValidatorBuilder {

    /**
     * Helper method to check that a LAO id is valid.
     *
     * @param organizer the lao organizer
     * @param creation the lao creation time
     * @param name the lao name
     * @throws IllegalArgumentException if the id is invalid
     */
    fun validLaoId(
        id: String,
        organizer: PublicKey,
        creation: Long,
        name: String,
    ): MessageValidatorBuilder {
      // If any of the arguments are empty or null this throws an exception
      require(id == Lao.generateLaoId(organizer, creation, name)) {
        "CreateLao id must be Hash(organizer||creation||name)"
      }
      return this
    }

    /**
     * Helper method to check that a CreateMeeting id is valid.
     *
     * @param id the id of the CreateMeeting
     * @param laoId the lao identifier
     * @param creation the CreateMeeting creation time
     * @param name the CreateMeeting name
     * @throws IllegalArgumentException if the id is invalid
     */
    fun validCreateMeetingId(
        id: String,
        laoId: String,
        creation: Long,
        name: String,
    ): MessageValidatorBuilder {
      // If any of the arguments are empty or null this throws an exception
      require(id == Meeting.generateCreateMeetingId(laoId, creation, name)) {
        "CreateMeeting id must be Hash(\"M\"||laoId||creation||name)"
      }
      return this
    }

    /**
     * Helper method to check that a StateMeeting id is valid.
     *
     * @param id the id of the StateMeeting
     * @param laoId the lao identifier
     * @param creation the StateMeeting creation time
     * @param name the StateMeeting name
     * @throws IllegalArgumentException if the id is invalid
     */
    fun validStateMeetingId(
        id: String,
        laoId: String,
        creation: Long,
        name: String,
    ): MessageValidatorBuilder {
      // If any of the arguments are empty or null this throws an exception
      require(id == Meeting.generateStateMeetingId(laoId, creation, name)) {
        "StateMeeting id must be Hash(\"M\"||laoId||creation||name)"
      }
      return this
    }

    /**
     * Helper method to check that times are reasonably recent and not in the future. The time
     * values provided are assumed to be in Unix epoch time (UTC).
     *
     * @param times time values to be checked
     * @throws IllegalArgumentException if times are too far in the past or in the future
     */
    fun validPastTimes(vararg times: Long): MessageValidatorBuilder {
      val currentTime = Instant.now().epochSecond + VALID_FUTURE_DELAY
      val validPastTime = currentTime - VALID_PAST_DELAY

      for (time in times) {
        require(time >= validPastTime) { "Time cannot be too far in the past" }
        require(time <= currentTime) { "Time cannot be in the future" }
      }
      return this
    }

    /**
     * Helper method to check that times in a Data are ordered. The time values provided are assumed
     * to be in Unix epoch time (UTC).
     *
     * @param times time values in the desired ascending order
     * @throws IllegalArgumentException if the times are not in ascending order
     */
    fun orderedTimes(vararg times: Long): MessageValidatorBuilder {
      for (i in 0 until times.size - 1) {
        require(times[i + 1] >= times[i]) { "Times must be in ascending order" }
      }
      return this
    }

    /**
     * Helper method to check that a string is a valid URL-safe base64 encoding.
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is not a URL-safe base64 encoding
     */
    fun isBase64(input: String?, field: String): MessageValidatorBuilder {
      require(input != null && BASE64_PATTERN.matcher(input).matches()) {
        "$field must be a base 64 encoded string"
      }
      return this
    }

    /**
     * Helper method to check that a string is a valid not-empty URL-safe base64 encoding.
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is empty or not a URL-safe base64 encoding
     */
    fun isNotEmptyBase64(input: String?, field: String): MessageValidatorBuilder {
      return stringNotEmpty(input, field).isBase64(input, field)
    }

    /**
     * Helper method to ensure all provided strings are valid, not-empty URL-safe base64 encodings.
     *
     * @param inputs the strings to check
     * @param field the name of the field (to print in case of error)
     * @throws IllegalArgumentException if any string is empty or not a URL-safe base64 encoding
     */
    fun areNotEmptyBase64(vararg inputs: String?, field: String): MessageValidatorBuilder {
      inputs.forEach { input -> isNotEmptyBase64(input, field) }
      return this
    }

    fun isNotNull(input: Any?, field: String): MessageValidatorBuilder {
      requireNotNull(input) { "$field cannot be null" }
      return this
    }

    /**
     * Helper method to check that a string represents a valid unicode emoji supported for reactions
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is not representing a valid codepoint
     */
    fun isValidEmoji(input: String, field: String): MessageValidatorBuilder {
      require(Reaction.ReactionEmoji.isSupported(input)) {
        "$field is not a supported unicode emoji"
      }
      return this
    }

    /**
     * Helper method to check that an int is not negative.
     *
     * @param input the int to check
     * @param field name of the field (to print in case of error)
     */
    fun isNotNegative(input: Int, field: String): MessageValidatorBuilder {
      require(input >= 0) { "$field cannot be negative" }
      return this
    }

    /**
     * Helper method to check that a string is not empty.
     *
     * @param input the string to check
     * @param field name of the field (to print in case of error)
     * @throws IllegalArgumentException if the string is empty or null
     */
    fun stringNotEmpty(input: String?, field: String): MessageValidatorBuilder {
      require(!input.isNullOrEmpty()) { "$field cannot be empty" }
      return this
    }

    /**
     * Helper method to check that a value is greater or equal than a given value.
     *
     * @param input the value to check
     * @param field name of the field (to print in case of error)
     * @param value the value to compare to
     * @throws IllegalArgumentException if the value is not greater or equal than the given value
     */
    fun greaterOrEqualThan(input: Long, value: Long, field: String): MessageValidatorBuilder {
      require(input >= value) { "$field must be greater or equal than $value" }
      return this
    }

    /**
     * Helper method to check that a value is one of a given list of values
     *
     * @param input the value to check
     * @param field name of the field (to print in case of error)
     * @param values the list of values to compare to (modular number of arguments)
     * @throws IllegalArgumentException if the value is not one of the given values
     */
    fun elementIsOneOf(input: Any, field: String, vararg values: Any): MessageValidatorBuilder {
      require(values.isNotEmpty()) { "Values cannot be empty" }
      require(values.contains(input)) { "$field must be one of $values" }
      return this
    }

    /**
     * Helper method to check that a list is not empty.
     *
     * @param list the list to check
     * @throws IllegalArgumentException if the string is empty or null
     */
    fun listNotEmpty(list: List<*>?): MessageValidatorBuilder {
      require(!list.isNullOrEmpty()) { "List cannot be null or empty" }
      return this
    }

    /**
     * Helper method to check that a list has no duplicates. This method relies on hashCode for
     * equality comparison.
     *
     * @param list the list to check
     * @throws IllegalArgumentException if there are duplicates
     */
    fun noListDuplicates(list: List<*>): MessageValidatorBuilder {
      val uniqueElements: Set<*> = HashSet(list)
      require(uniqueElements.size == list.size) { "List has duplicates" }
      return this
    }

    /**
     * Helper method to check that a list of votes has no duplicates and their ids are base 64.
     *
     * @param votes the list to check
     * @throws IllegalArgumentException if the list has invalid votes
     */
    fun validVotes(votes: List<Vote>?): MessageValidatorBuilder {
      // votes is null if it is an open ballot election
      if (votes == null) {
        return this
      }
      noListDuplicates(votes)
      return this
    }

    /**
     * Helper method to check that a message has a valid structure.
     *
     * @param message the message to check, expected to be a map with string keys and optional
     *   string values
     */
    fun validMessage(message: MessageGeneral): MessageValidatorBuilder {

      val data = message.dataEncoded
      val sender = message.sender
      val signature = message.signature
      val messageId = message.messageId

      verify()
          .areNotEmptyBase64(
              data.encoded,
              sender.encoded,
              signature.encoded,
              messageId.encoded,
              field = "Message Fields")

      val witnessSignatures = message.witnessSignatures
      witnessSignatures.forEach {
        verify().isNotEmptyBase64(it.signature.encoded, "Witness Signature")
      }

      return this
    }

    /**
     * Helper method to check a single question for validity.
     *
     * @param title the title of the question
     * @param votingMethod the voting method of the question
     * @param ballotOptions the ballot options of the question
     * @throws IllegalArgumentException if the question does not meet the criteria.
     */
    fun validQuestion(
        title: String,
        votingMethod: String,
        ballotOptions: List<String>,
    ): MessageValidatorBuilder {
      stringNotEmpty(title, "question title")
      require(votingMethod in validVotingMethods) {
        "Unsupported voting method in question: ${title}. Must be one of $validVotingMethods."
      }
      validBallotOptions(ballotOptions)
      return this
    }

    /** Helper method to check that a string list is sorted */
    fun stringListIsSorted(list: List<*>, field: String): MessageValidatorBuilder {
      require(list == list.sortedBy { it.toString() }) { "$field must be sorted" }
      return this
    }

    private fun validBallotOptions(ballotOptions: List<String>?): MessageValidatorBuilder {
      requireNotNull(ballotOptions) { "Ballot options cannot be null" }
      listNotEmpty(ballotOptions)
      require(ballotOptions.size >= 2) { "There must be at least 2 ballot options" }
      noListDuplicates(ballotOptions)
      ballotOptions.forEach {
        stringNotEmpty(it, "ballot option in place " + ballotOptions.indexOf(it))
      }
      return this
    }

    fun validUrl(input: String?): MessageValidatorBuilder {
      require(input != null && URL_PATTERN.matcher(input).matches()) { "Input is not a url" }
      return this
    }

    fun isValidPoPCHAUrl(input: String?, laoId: String): MessageValidatorBuilder {
      // Check it's a valid url
      verify().validUrl(input)
      val uri =
          Uri.parse(input) ?: throw IllegalArgumentException("Impossible to parse the URL: $input")

      // Check required arguments are present
      for (arg in REQUIRED_ARGUMENTS) {
        requireNotNull(uri.getQueryParameter(arg)) {
          "Required argument $arg is missing in the URL."
        }
      }

      // Check response type respects openid standards
      val responseType = uri.getQueryParameter(PoPCHAQRCode.FIELD_RESPONSE_TYPE)
      require(responseType == VALID_RESPONSE_TYPE) { "Invalid response type in the URL" }

      // Check the scope contains all the required scopes
      require(
          Arrays.stream(REQUIRED_SCOPES).allMatch { name: String ->
            uri.getQueryParameter(PoPCHAQRCode.FIELD_SCOPE)!!.contains(name)
          }) {
            "Invalid scope"
          }

      // Check response mode is valid
      val responseMode = uri.getQueryParameter(PoPCHAQRCode.FIELD_RESPONSE_MODE)
      require(
          responseMode == null ||
              Arrays.stream(VALID_RESPONSE_MODES).anyMatch { s: String ->
                responseMode.contains(s)
              }) {
            "Invalid response mode"
          }

      // Check lao ID in login hint match the right laoID
      val laoHint = uri.getQueryParameter(PoPCHAQRCode.FIELD_LOGIN_HINT)
      require(laoHint == laoId) { "Invalid LAO ID $laoHint" }

      return this
    }

    companion object {
      // Defines how old messages can be to be considered valid, keeping it non-restrictive here for
      // now
      const val VALID_PAST_DELAY: Long = 100000000

      // Constant used to validate a timestamp as not in the future, considering that timestamp from
      // different devices can slightly vary
      const val VALID_FUTURE_DELAY: Long = 120

      // Constants used for checking PoPCHA URLs
      private val REQUIRED_ARGUMENTS =
          arrayOf(
              PoPCHAQRCode.FIELD_CLIENT_ID,
              PoPCHAQRCode.FIELD_NONCE,
              PoPCHAQRCode.FIELD_REDIRECT_URI,
              PoPCHAQRCode.FIELD_SCOPE,
          )
      private const val VALID_RESPONSE_TYPE = "id_token"
      private val REQUIRED_SCOPES = arrayOf("openid", "profile")
      private val VALID_RESPONSE_MODES = arrayOf("query", "fragment")

      private val validVotingMethods = ElectionSetupFragment.VotingMethods.values().map { it.desc }
    }
  }
}
