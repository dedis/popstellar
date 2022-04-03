package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.AddDummyTransaction;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.DummyCoin;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;

/** Coin messages handler class */
public class DummyTransactionHandler {
    public static final String TAG = DummyTransactionHandler.class.getSimpleName();

    private DummyTransactionHandler() {
        throw new IllegalArgumentException("Utility class");
    }

    /**
     * Process an AddDummyTransaction message.
     *
     * @param context the HandlerContext of the message
     * @param addDummyTransaction the data of the message that was received
     */
    public static void handleChirpAdd(HandlerContext context, AddDummyTransaction addDummyTransaction) {
        LAORepository laoRepository = context.getLaoRepository();
        Channel channel = context.getChannel();
        MessageID messageId = context.getMessageId();
        PublicKey senderPk = context.getSenderPk();

        Lao lao = laoRepository.getLaoByChannel(channel);

        DummyCoin dummyCoin = new DummyCoin(messageId);

        dummyCoin.setChannel(channel);
        dummyCoin.setSender(senderPk);

        lao.updateAllDummyCoin(messageId, dummyCoin);
    }


}
