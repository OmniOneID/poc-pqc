package org.omnione.did.base.util;

import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;

/**
 * The MultiBaseUtil class provides utility methods for encoding and decoding data using multibase encoding schemes.
 * It is designed to facilitate the conversion of data to and from various base encoding formats,
 * ensuring compatibility and ease of use across different systems and protocols.
 *
 * @author : yklee0911
 * @fileName : MultibaseUtil
 * @since : 2024/05/30
 */
public class BaseMultibaseUtil {

    /**
     * Encode a given string using the Base58btc multibase encoding scheme.
     * This method converts the input string to a byte array using UTF-8 encoding
     * before performing the encoding.
     *
     * @author        : yklee0911
     * @since         : 2024/06/03
     */
    public static String encode(byte[] inputData) {
        try {
            return MultiBaseUtils.encode(inputData, MultiBaseType.base64);
        } catch (CryptoException e) {
            System.out.println("Error occurred while encoding the input data.");
            throw new RuntimeException("Error occurred while encoding the input data.");
        }
    }

    /**
     * Encode a given string using the specified multibase encoding scheme.
     * This method converts the input string to a byte array using UTF-8 encoding
     * before performing the encoding.
     *
     * @author        : yklee0911
     * @since         : 2024/07/02
     */
    public static String encode(byte[] inputData, MultiBaseType multiBaseType) {
        try {
            return MultiBaseUtils.encode(inputData, multiBaseType);
        } catch (CryptoException e) {
            System.out.println("Error occurred while encoding the input data.");
            throw new RuntimeException("Error occurred while encoding the input data.");
        }
    }

    /**
     * Decode a given string from the Base58btc multibase encoding scheme.
     * This method converts the encoded string back to its original byte array form.
     *
     * @author        : yklee0911
     * @since         : 2024/06/03
     */
    public static byte[] decode(String encodedData) {
        try {
            byte[] decodedBytes = MultiBaseUtils.decode(encodedData);
            if (decodedBytes == null) {
                System.out.println("Error occurred while decoding the input data.");
                throw new RuntimeException("Error occurred while decoding the input data.");
            }
            return decodedBytes;
        }  catch (CryptoException e) {
            System.out.println("Error occurred while decoding the input data.");
            throw new RuntimeException("Error occurred while decoding the input data.");
        }
    }
}
