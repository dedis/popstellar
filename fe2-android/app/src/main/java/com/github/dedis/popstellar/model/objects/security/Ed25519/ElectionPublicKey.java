package com.github.dedis.popstellar.model.objects.security.Ed25519;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;

import org.bouncycastle.crypto.CryptoException;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import ch.epfl.dedis.lib.crypto.Ed25519;
import ch.epfl.dedis.lib.crypto.Ed25519Point;
import ch.epfl.dedis.lib.crypto.Point;
import ch.epfl.dedis.lib.crypto.Scalar;
import io.reactivex.annotations.NonNull;

/**
 * Represents a private key meant for El-Gam
 */
public class ElectionPublicKey {

    // We use elliptic curve Ed25519 for encryption
    public Ed25519 curve;
    // Point is generate with given public key
    private Point point;

    public ElectionPublicKey(@NonNull String publicKey) throws CryptoException {
        try {
            point = new Ed25519Point(publicKey);
        } catch (Exception e) {
            throw new CryptoException("Could not create the point for ellyptic curve, please provide another key");
        }
        curve = new Ed25519();
    }

    @Override
    public String toString() {
        return point.toString();
    }

    public Base64URLData toBase64() {
        return new Base64URLData(toString());
    }

    private Point getPoint() {
        return point;
    }

    /**
     * Encrypts with ElGamal under Ed25519 curve
     *
     * @param message message a 2 byte integer corresponding to the chosen vote
     * @return Returns 2 32 byte strings which should be appended
     * together and base64 encoded. For clarification, the first 32 bytes is point K and the second 32 bytes is point C.
     */
    public String encrypt(@NonNull byte[] message) {
        if (message.length > 29) {
            throw new IllegalArgumentException("The message should contain at maximum 29 bytes");
        }
        // Follows this implementation:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/evoting/lib/elgamal.go#L15-L23
        // An example for embedding was found here:
        // https://github.com/dedis/cothority/blob/0299bcd78bab22bde6d6449b1594613987355535/external/js/kyber/spec/group/edwards25519/point.spec.ts#L203

        // M := cothority.Suite.Point().Embed(message, random.New())
        // Proper embedding with overflowing byte array (len > 32) will need to be changed later
        Point M = Ed25519Point.embed(message);
        // ElGamal-encrypt the point to produce ciphertext (K,C).
        // k := cothority.Suite.Scalar().Pick(random.New()) -- ephemeral private key
        // TODO generate a random scalar of prime order
        Scalar k = Ed25519.prime_order;
        // K = cothority.Suite.Point().Mul(k, nil)
        Point K = Ed25519Point.base().mul(k);
        // S := cothority.Suite.Point().Mul(k, public) -- ephemeral DH shared secret
        Point S = getPoint().mul(k);
        // C = S.Add(S, M)  -- message blinded with secret
        Point C = S.add(M);

        //Concat K and C and encodes it in Base64
        byte[] result;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(K.toBytes());
            output.write(C.toBytes());
            result = output.toByteArray();
        } catch (Exception e) {
            System.out.println("Something happened during the encryption, could concatenate the final result into a byte array");
            result = null;
        }
        if (Objects.isNull(result)) {
            return null;
        }
        Base64URLData encodedResult = new Base64URLData(result);
        return encodedResult.toString();
    }


}

