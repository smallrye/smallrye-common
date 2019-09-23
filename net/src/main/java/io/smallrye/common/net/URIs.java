package io.smallrye.common.net;

import java.net.URI;

/**
 * URI-specific utilities.
 */
public final class URIs {
    private URIs() {
    }

    /**
     * Get the user name information from a URI, if any.
     *
     * @param uri the URI
     * @return the user name, or {@code null} if the URI did not contain a recoverable user name
     */
    public static String getUserFromURI(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo == null && "domain".equals(uri.getScheme())) {
            final String ssp = uri.getSchemeSpecificPart();
            final int at = ssp.lastIndexOf('@');
            if (at == -1) {
                return null;
            }
            userInfo = ssp.substring(0, at);
        }
        if (userInfo != null) {
            final int colon = userInfo.indexOf(':');
            if (colon != -1) {
                userInfo = userInfo.substring(0, colon);
            }
        }
        return userInfo;
    }
}
