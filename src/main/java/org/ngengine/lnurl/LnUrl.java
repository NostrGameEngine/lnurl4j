/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2025, Riccardo Balbo
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ngengine.lnurl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngengine.bech32.Bech32;
import org.ngengine.bech32.Bech32DecodingException;
import org.ngengine.bech32.Bech32EncodingException;
import org.ngengine.bech32.Bech32InvalidChecksumException;
import org.ngengine.bech32.Bech32InvalidRangeException;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;

/**
 * A lnurl or lud16 URL.
 */
// lud06
public class LnUrl {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final byte[] hrp = "lnurl".getBytes();

    private static final Logger logger = Logger.getLogger(LnUrl.class.getName());
    private final String bech32;
    private final URI plainUrl;
    private final String tag;

    private static final List<LnUrlServiceFactory> serviceFactories = new ArrayList<>();

    static {
        registerServiceFactory(data -> {
            if (LnUrlPay.isAssignableTo(data)) {
                return new LnUrlPay(data);
            }
            return null;
        });
    }

    public static void registerServiceFactory(LnUrlServiceFactory factory) {
        serviceFactories.add(Objects.requireNonNull(factory, "Factory cannot be null"));
    }

    public LnUrl(String lnurl) throws URISyntaxException {
        try {
            lnurl = lnurl.toLowerCase().trim();
            if (lnurl.startsWith("lightning:")) {
                lnurl = lnurl.substring(10);
            }

            Lud16Type lud16 = Lud16Type.getLud16(lnurl);
            if (lud16 != null) {
                logger.finer("LUD16 lnurl: " + lnurl);

                plainUrl = new URI(lud16.toHttps(lnurl));
                logger.finer("Decoded lnurl: " + plainUrl);

                bech32 = Bech32.bech32Encode(hrp, ByteBuffer.wrap(plainUrl.toString().getBytes()));
                logger.finer("Bech32 lnurl: " + bech32);

                tag = lud16.tag();
                logger.finer("LUD16 tag: " + tag);
            } else {
                this.bech32 = lnurl;
                logger.finer("Bech32 lnurl: " + bech32);

                ByteBuffer decodedB = Bech32.bech32Decode(lnurl);
                plainUrl = new URI(new String(decodedB.array(), 0, decodedB.limit()));
                logger.finer("Decoded lnurl: " + plainUrl);

                tag = loadTag();
                logger.finer("Tag: " + tag);
            }
        } catch (Exception e) {
            if (e instanceof URISyntaxException) {
                throw (URISyntaxException) e;
            } else {
                throw new URISyntaxException(lnurl, "Failed to parse LNURL: " + e.getMessage(), 0);
            }
        }
    }

    protected LnUrl(URI url) throws URISyntaxException {
        try {
            plainUrl = url;
            bech32 = Bech32.bech32Encode(hrp, ByteBuffer.wrap(url.toString().getBytes()));
            tag = loadTag();
        } catch (Exception e) {
            throw new URISyntaxException(url.toString(), "Failed to parse LNURL: " + e.getMessage(), 0);
        }
    }

    private String loadTag() {
        String tag = null;
        if (plainUrl.getQuery() != null) {
            String[] params = plainUrl.getQuery().split("&");
            for (String param : params) {
                if (param.startsWith("tag=")) {
                    tag = param.substring(4);
                    tag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        return tag;
    }

    /**
     * Encode an url to a lnurl bech32 representation.
     */
    public static LnUrl encode(URI url)
        throws Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException, URISyntaxException, Bech32EncodingException {
        return new LnUrl(url);
    }

    /**
     * Encode an url to a lnurl bech32 representation.
     */
    public static LnUrl encode(String url)
        throws Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException, URISyntaxException, Bech32EncodingException {
        return new LnUrl(new URI(url));
    }

    /**
     * Returns a link that can be used in a browser or app to open the LNURL.
     * @return A link in the format "lightning:bech32"
     */
    public String toLnUrlLink() {
        return "lightning:" + bech32;
    }

    /**
     * The decoded java URI representation of the LNURL after it is decoded from bech32.
     * This can be used to fetch the service with any http client.
     * @return  The decoded URI of the LNURL.
     */
    public URI toURI() {
        return plainUrl;
    }

    /**
     * Return the bech32 representation of the LNURL.
     * The classic shareable representation of a LNURL.
     * @return The bech32 encoded LNURL string.
     */
    public String toBech32() {
        return bech32;
    }

    @Override
    public String toString() {
        return bech32;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LnUrl)) return false;
        LnUrl other = (LnUrl) obj;
        return bech32.equals(other.bech32);
    }

    @Override
    public int hashCode() {
        return bech32.hashCode();
    }

    /**
     * The tag for this LNURL service if applicable.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get an handler for the underlying service offered by this LNURL.
     * @param <T> The type of the service to be returned.
     * @return An AsyncTask that will return the service when completed.
     * @throws LnUrlException
     */

    public <T extends LnUrlService> AsyncTask<T> getService() throws LnUrlException {
        return getService(DEFAULT_TIMEOUT);
    }

    /**
     * Get an handler for the underlying service offered by this LNURL.
     * @param <T> The type of the service to be returned.
     * @param timeout The timeout for the request to fetch the service.
     * @return An AsyncTask that will return the service when completed.
     * @throws LnUrlException
     */
    public <T extends LnUrlService> AsyncTask<T> getService(Duration timeout) throws LnUrlException {
        String url = toURI().toString();
        logger.finer("Fetching LNURL service from: " + url+ " with timeout: " + timeout);
        return NGEPlatform
            .get()
            .httpGet(url, timeout, null)
            .then(res -> {
                try {
                    Map<String, Object> data = NGEPlatform.get().fromJSON(res, Map.class);
                    if (LnUrlException.isAssignableTo(data)) {
                        throw new LnUrlException(data);
                    }
                    LnUrlService service = null;
                    for (LnUrlServiceFactory factory : serviceFactories) {
                        try {
                            service = factory.construct(data);
                            if (service != null) {
                                break; // Found a valid service
                            }
                        } catch (Exception e) {
                            logger.warning("Failed to create service with factory: " + e.getMessage());
                        }
                    }

                    if (service == null) {
                        throw new LnUrlException(
                            LnUrlException.Status.NOT_FOUND,
                            "No LNURL service found for: " + toURI().toString()
                        );
                    }
                    return (T) service;
                } catch (Exception e) {
                    throw new RuntimeException(
                        new LnUrlException(
                            LnUrlException.Status.INVALID,
                            "Failed to parse LNURL service response: " + e.getMessage(),
                            e
                        )
                    );
                }
            });
    }
}
