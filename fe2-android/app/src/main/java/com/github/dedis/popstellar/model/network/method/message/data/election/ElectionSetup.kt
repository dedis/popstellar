package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName
import java.util.stream.Collectors

@Immutable
class ElectionSetup(
        name: String,
        creation: Long,
        start: Long,
        end: Long,
        laoId: String,
        electionVersion: ElectionVersion,
        questions: List<Question?>) : Data() {
    val electionId: String

    @JvmField
    val name: String
    val laoId: String

    @SerializedName(value = "created_at")
    val creation: Long

    @JvmField
    @SerializedName(value = "start_time")
    val startTime: Long

    @JvmField
    @SerializedName(value = "end_time")
    val endTime: Long

    @JvmField
    @SerializedName("version")
    val electionVersion: ElectionVersion
    private val questions: List<ElectionQuestion>

    /**
     * @param name name of the election
     * @param creation creation timestamp
     * @param start start timestamp
     * @param end end timestamp
     * @param laoId id of the LAO
     * @param electionVersion version of the election
     * @param questions list of election questions
     */
    init {
        // The lao id is checked to be a known lao in the election setup handler
        MessageValidator.verify()
                .orderedTimes(creation, start, end)
                .validPastTimes(creation)
                .stringNotEmpty(name, "election name")
                .isNotEmptyBase64(laoId, "lao id")
                .noListDuplicates(questions)
        this.name = name
        this.creation = creation
        startTime = start
        endTime = end
        this.laoId = laoId
        this.electionVersion = electionVersion
        electionId = Election.generateElectionSetupId(laoId, this.creation, name)
        this.questions = questions.stream().map { q: Question? -> ElectionQuestion(electionId, q) }.collect(Collectors.toList())
    }

    override val `object`: String
        get() = Objects.ELECTION.`object`
    override val action: String
        get() = Action.SETUP.action

    fun getQuestions(): List<ElectionQuestion> {
        return ArrayList(questions)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ElectionSetup
        return creation == that.creation && electionVersion == that.electionVersion && startTime == that.startTime
                && electionId == that.electionId && creation == that.creation && name == that.name && endTime == that.endTime
                && questions == that.getQuestions()
    }

    override fun hashCode(): Int {
        return java.util.Objects.hash(
                electionId, name, creation, startTime, endTime, getQuestions())
    }

    override fun toString(): String {
        return ("ElectionSetup={"
                + "version='"
                + electionVersion
                + '\''
                + ", id='"
                + electionId
                + '\''
                + ", lao='"
                + laoId
                + '\''
                + ", name='"
                + name
                + '\''
                + ", createdAt="
                + creation
                + ", startTime="
                + startTime
                + ", endTime="
                + endTime
                + ", questions="
                + questions.toTypedArray().contentToString()
                + '}')
    }
}