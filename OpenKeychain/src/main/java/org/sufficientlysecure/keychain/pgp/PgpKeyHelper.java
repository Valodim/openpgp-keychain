/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class PgpKeyHelper {

    /**
     * TODO: Only used in HkpKeyServer. Get rid of this one!
     */
    public static String getAlgorithmInfo(int algorithm) {
        return getAlgorithmInfo(null, algorithm, 0);
    }

    public static String getAlgorithmInfo(Context context, int algorithm) {
        return getAlgorithmInfo(context, algorithm, 0);
    }

    /**
     * Based on <a href="http://tools.ietf.org/html/rfc2440#section-9.1">OpenPGP Message Format</a>
     */
    public static String getAlgorithmInfo(Context context, int algorithm, int keySize) {
        String algorithmStr;

        switch (algorithm) {
            case PGPPublicKey.RSA_ENCRYPT:
            case PGPPublicKey.RSA_GENERAL:
            case PGPPublicKey.RSA_SIGN: {
                algorithmStr = "RSA";
                break;
            }
            case PGPPublicKey.DSA: {
                algorithmStr = "DSA";
                break;
            }

            case PGPPublicKey.ELGAMAL_ENCRYPT:
            case PGPPublicKey.ELGAMAL_GENERAL: {
                algorithmStr = "ElGamal";
                break;
            }

            case PGPPublicKey.ECDSA:
            case PGPPublicKey.ECDH: {
                algorithmStr = "ECC";
                break;
            }

            default: {
                if (context != null) {
                    algorithmStr = context.getResources().getString(R.string.unknown_algorithm);
                } else {
                    // TODO
                    algorithmStr = "unknown";
                }
                break;
            }
        }
        if (keySize > 0)
            return algorithmStr + ", " + keySize + " bit";
        else
            return algorithmStr;
    }

    /**
     * Converts fingerprint to hex (optional: with whitespaces after 4 characters)
     * <p/>
     * Fingerprint is shown using lowercase characters. Studies have shown that humans can
     * better differentiate between numbers and letters when letters are lowercase.
     *
     * @param fingerprint
     * @return
     */
    public static String convertFingerprintToHex(byte[] fingerprint) {
        String hexString = Hex.toHexString(fingerprint).toLowerCase(Locale.US);

        return hexString;
    }

    /**
     * Convert key id from long to 64 bit hex string
     * <p/>
     * V4: "The Key ID is the low-order 64 bits of the fingerprint"
     * <p/>
     * see http://tools.ietf.org/html/rfc4880#section-12.2
     *
     * @param keyId
     * @return
     */
    public static String convertKeyIdToHex(long keyId) {
        long upper = keyId >> 32;
        if (upper == 0) {
            // this is a short key id
            return convertKeyIdToHexShort(keyId);
        }
        return "0x" + convertKeyIdToHex32bit(keyId >> 32) + convertKeyIdToHex32bit(keyId);
    }

    public static String convertKeyIdToHexShort(long keyId) {
        return "0x" + convertKeyIdToHex32bit(keyId);
    }

    private static String convertKeyIdToHex32bit(long keyId) {
        String hexString = Long.toHexString(keyId & 0xffffffffL).toLowerCase(Locale.US);
        while (hexString.length() < 8) {
            hexString = "0" + hexString;
        }
        return hexString;
    }


    public static SpannableStringBuilder colorizeFingerprint(String fingerprint) {
        // split by 4 characters
        fingerprint = fingerprint.replaceAll("(.{4})(?!$)", "$1 ");

        // add line breaks to have a consistent "image" that can be recognized
        char[] chars = fingerprint.toCharArray();
        chars[24] = '\n';
        fingerprint = String.valueOf(chars);

        SpannableStringBuilder sb = new SpannableStringBuilder(fingerprint);
        try {
            // for each 4 characters of the fingerprint + 1 space
            for (int i = 0; i < fingerprint.length(); i += 5) {
                int spanEnd = Math.min(i + 4, fingerprint.length());
                String fourChars = fingerprint.substring(i, spanEnd);

                int raw = Integer.parseInt(fourChars, 16);
                byte[] bytes = {(byte) ((raw >> 8) & 0xff - 128), (byte) (raw & 0xff - 128)};
                int[] color = getRgbForData(bytes);
                int r = color[0];
                int g = color[1];
                int b = color[2];

                // we cannot change black by multiplication, so adjust it to an almost-black grey,
                // which will then be brightened to the minimal brightness level
                if (r == 0 && g == 0 && b == 0) {
                    r = 1;
                    g = 1;
                    b = 1;
                }

                // Convert rgb to brightness
                double brightness = 0.2126 * r + 0.7152 * g + 0.0722 * b;

                // If a color is too dark to be seen on black,
                // then brighten it up to a minimal brightness.
                if (brightness < 80) {
                    double factor = 80.0 / brightness;
                    r = Math.min(255, (int) (r * factor));
                    g = Math.min(255, (int) (g * factor));
                    b = Math.min(255, (int) (b * factor));

                    // If it is too light, then darken it to a respective maximal brightness.
                } else if (brightness > 180) {
                    double factor = 180.0 / brightness;
                    r = (int) (r * factor);
                    g = (int) (g * factor);
                    b = (int) (b * factor);
                }

                // Create a foreground color with the 3 digest integers as RGB
                // and then converting that int to hex to use as a color
                sb.setSpan(new ForegroundColorSpan(Color.rgb(r, g, b)),
                        i, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Colorization failed", e);
            // if anything goes wrong, then just display the fingerprint without colour,
            // instead of partially correct colour or wrong colours
            return new SpannableStringBuilder(fingerprint);
        }

        return sb;
    }

    /**
     * Converts the given bytes to a unique RGB color using SHA1 algorithm
     *
     * @param bytes
     * @return an integer array containing 3 numeric color representations (Red, Green, Black)
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.DigestException
     */
    private static int[] getRgbForData(byte[] bytes) throws NoSuchAlgorithmException, DigestException {
        MessageDigest md = MessageDigest.getInstance("SHA1");

        md.update(bytes);
        byte[] digest = md.digest();

        int[] result = {((int) digest[0] + 256) % 256,
                ((int) digest[1] + 256) % 256,
                ((int) digest[2] + 256) % 256};
        return result;
    }

}
