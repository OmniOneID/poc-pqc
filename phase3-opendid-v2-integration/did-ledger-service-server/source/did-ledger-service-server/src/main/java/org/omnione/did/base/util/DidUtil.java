package org.omnione.did.base.util;

/**
 * Description...
 *
 * @author : gwnam
 * @fileName : d
 * @since : 6/14/24
 */
public class DidUtil {

    /**
     * Extract the DID from a given DID URL, removing any query or fragment components.
     *
     * @author        : yklee0911
     * @since         : 2024/06/03
     */
    public static String extractDid(String didKeyUrl) {
        if (didKeyUrl == null) {
            return null;
        }

        int queryIndex = didKeyUrl.indexOf('?');
        int fragmentIndex = didKeyUrl.indexOf('#');

        int endIndex = didKeyUrl.length();

        if (queryIndex != -1) {
            endIndex = queryIndex;
        }

        if (fragmentIndex != -1 && (fragmentIndex < endIndex)) {
            endIndex = fragmentIndex;
        }

        return didKeyUrl.substring(0, endIndex);
    }

    public static Short extractVersion(String url) {
        String versionParam = "version=";
        int versionStart = url.indexOf(versionParam);

        if (versionStart == -1) {
            return null; // version 파라미터가 없을 경우 null 반환
        }

        int versionEnd = url.indexOf('&', versionStart);
        if (versionEnd == -1) {
            versionEnd = url.indexOf('#', versionStart);
            if (versionEnd == -1) {
                versionEnd = url.length();
            }
        }

        return Short.parseShort(url.substring(versionStart + versionParam.length(), versionEnd));
    }


}
