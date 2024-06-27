package com.github.dedis.popstellar.model.network.method.message.data.federation

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.Assert
import org.junit.Test
import java.time.Instant

class TokensExchangeTest {

    @Test
    fun laoIDTest() {
        Assert.assertEquals(LAO_ID, TK_EXCHANGE.laoId)
    }

    @Test
    fun rollCallIDTest() {
        Assert.assertEquals(ROLL_CALL_ID, TK_EXCHANGE.rollCallId)
    }

    @Test
    fun tokensArrayTest() {
        Assert.assertEquals(TOKENS, TK_EXCHANGE.tokens)
    }

    @Test
    fun timestampTest() {
        Assert.assertEquals(TIMESTAMP, TK_EXCHANGE.timestamp)
    }

    @Test
    fun tokensExchangeObjectTest() {
        Assert.assertEquals(Objects.FEDERATION.`object`, TK_EXCHANGE.`object`)
    }

    @Test
    fun tokensExchangeActionTest() {
        Assert.assertEquals(Action.TOKENS_EXCHANGE.action, TK_EXCHANGE.action)
    }

    @Test
    fun equalsTest() {
        val tokensExchange2 = TokensExchange(LAO_ID, ROLL_CALL_ID, TOKENS, TIMESTAMP)
        Assert.assertEquals(TK_EXCHANGE, tokensExchange2)
        Assert.assertEquals(TK_EXCHANGE, TK_EXCHANGE)
        Assert.assertEquals(TK_EXCHANGE.hashCode().toLong(), tokensExchange2.hashCode().toLong())

        val tokensExchange3 = TokensExchange(Lao.generateLaoId(ORGANIZER, CREATION, "LAO2"), ROLL_CALL_ID, TOKENS, TIMESTAMP)
        val tokensExchange4 = TokensExchange(LAO_ID, "UkMy", TOKENS, TIMESTAMP)
        val tokensExchange5 = TokensExchange(LAO_ID, ROLL_CALL_ID, arrayOf("token1"), TIMESTAMP)
        Assert.assertNotEquals(TK_EXCHANGE, tokensExchange3)
        Assert.assertNotEquals(TK_EXCHANGE, tokensExchange4)
        Assert.assertNotEquals(TK_EXCHANGE, tokensExchange5)
        Assert.assertNotEquals(TK_EXCHANGE, null)
    }

    @Test
    fun toStringTest() {
        Assert.assertEquals(
                "TokensExchange{lao_id='$LAO_ID', roll_call_id='$ROLL_CALL_ID', tokens='$TOKENS', timestamp='$TIMESTAMP'}",
                TK_EXCHANGE.toString()
        )
    }

    companion object {
        private val ORGANIZER = Base64DataUtils.generatePublicKey()
        private val CREATION = Instant.now().epochSecond
        private const val NAME = "Lao name"
        private val LAO_ID = Lao.generateLaoId(ORGANIZER, CREATION, NAME)
        private val ROLL_CALL_ID = "UkMx"
        private val TOKENS = arrayOf("token1", "token2", "token3")
        private val TIMESTAMP = Instant.now().epochSecond
        private val TK_EXCHANGE = TokensExchange(LAO_ID, ROLL_CALL_ID, TOKENS, TIMESTAMP)
    }
}
