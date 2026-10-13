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

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.ngengine.platform.NGEPlatform;

public class TestLnUrlPay {

    private Map<String, Object> endpoint() {
        return new LinkedHashMap<>(Map.of(
            "tag", "payRequest",
            "minSendable", 1000,
            "maxSendable", 1000000,
            "callback", "https://example.com/pay",
            "metadata", "[[\"text/plain\",\"Fixture\"]]",
            "nostrPubkey", "04c915daefee38317fa734444acee390a8269fe5810b2241e5e6dd343dfbecc9"
        ));
    }

    @Test
    public void standardFlagTakesPrecedenceOverLegacyAlias() {
        Map<String, Object> data = endpoint();
        assertFalse(new LnUrlPay(data).isNostrAllowed());
        data.put("allowsNostr", true);
        assertTrue(new LnUrlPay(data).isNostrAllowed());
        data.put("allowsNostr", false);
        data.put("allowNostr", true);
        assertFalse(new LnUrlPay(data).isNostrAllowed());
        data.remove("allowsNostr");
        assertTrue(new LnUrlPay(data).isNostrAllowed());
        data.put("allowsNostr", "true");
        assertFalse(new LnUrlPay(data).isNostrAllowed());
        data.put("allowsNostr", null);
        assertFalse(new LnUrlPay(data).isNostrAllowed());
    }

    @Test
    public void serializationUsesTheStandardFieldAndRoundTripsRefusals() {
        Map<String, Object> data = endpoint();
        data.put("allowNostr", true);
        Map<String, Object> serialized = new LnUrlPay(data).toMap();
        assertEquals(true, serialized.get("allowsNostr"));
        assertFalse(serialized.containsKey("allowNostr"));
        assertTrue(new LnUrlPay(serialized).isNostrAllowed());
        data.put("allowsNostr", false);
        serialized = new LnUrlPay(data).toMap();
        assertEquals(false, serialized.get("allowsNostr"));
        assertFalse(new LnUrlPay(serialized).isNostrAllowed());
    }

    private Map<String, String> query(URI callback) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : callback.getRawQuery().split("&")) {
            String[] pair = entry.split("=", 2);
            assertNull(result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        }
        return result;
    }

    @Test
    public void zapCallbackIncludesEncodedRequestAndRecipientLnurl() throws Exception {
        Map<String, Object> data = endpoint();
        data.put("allowsNostr", true);
        String lnurl = new LnAddress("index@example.com").toBech32();
        String request = NGEPlatform.get().toJSON(Map.of(
            "kind", 9734, "content", "Fixture & +", "tags", List.of(List.of("p", "recipient"), List.of("lnurl", lnurl))
        ));
        for (String base : List.of("https://example.com/pay", "https://example.com/pay?token=fixture")) {
            data.put("callback", base);
            Map<String, String> params = query(new LnUrlPay(data).getCallback(1000, null, null, request));
            assertEquals("1000", params.get("amount"));
            assertEquals(request, params.get("nostr"));
            assertEquals(lnurl, params.get("lnurl"));
            if (base.contains("?")) assertEquals("fixture", params.get("token"));
        }
    }

    @Test
    public void ordinaryPaymentsAndRequestsWithoutLnurlKeepTheirCallbackParameters() {
        LnUrlPay pay = new LnUrlPay(endpoint());
        Map<String, String> params = query(pay.getCallback(1000, null, null));
        assertEquals(Map.of("amount", "1000"), params);
        String request = NGEPlatform.get().toJSON(Map.of("kind", 9734, "tags", List.of(List.of("p", "recipient"))));
        assertEquals(Map.of("amount", "1000", "nostr", request), query(pay.getCallback(1000, null, null, request)));
        request = NGEPlatform.get().toJSON(Map.of("kind", 9734, "tags", List.of(List.of("lnurl", ""))));
        assertFalse(query(pay.getCallback(1000, null, null, request)).containsKey("lnurl"));
    }
}
