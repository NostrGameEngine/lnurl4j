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

import jakarta.annotation.Nullable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

/**
 * An lnurl pay request service
 */
// lud-06, lud-09, lud-11, lud-12, lud-18
public class LnUrlPay implements LnUrlService {

    public static final int MAX_METADATA_SIZE = 1024 * 1024; // 1 MB

    public static class Metadata {

        private final String type;
        private final Object value;

        public Metadata(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        public String type() {
            return type;
        }

        public Object value() {
            return value;
        }

        @Override
        public String toString() {
            return "Metadata{" +
                "type='" + type + '\'' +
                ", value=" + value +
                '}';    
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Metadata metadata = (Metadata) o;
            return type.equals(metadata.type) && value.equals(metadata.value);  
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);   
        }
    }

    private static final List<String> STR_METADATA_TYPES = List.of(
        "text/plain",
        "text/long-desc",
        "image/png;base64",
        "image/jpeg;base64"
    );

    protected static boolean isAssignableTo(Map<String, Object> data) {
        return "payRequest".equals(data.get("tag"));
    }

    private final long maxSendable, minSendable;
    private final int commentAllowed;
    private final List<Metadata> metadata = new ArrayList<>();
    private final URI callback;
    private final LnUrlPayerData payerData;

    protected LnUrlPay(
        long maxSendable,
        long minSendable,
        URI callback,
        int commentAllowed,
        Collection<Metadata> metadata,
        LnUrlPayerData payerData
    ) {
        if (maxSendable < 1 || minSendable < 1 || minSendable > maxSendable) {
            throw new IllegalArgumentException("Invalid sendable range: " + minSendable + " - " + maxSendable);
        }
        this.maxSendable = maxSendable;
        this.minSendable = minSendable;
        this.callback = callback;
        this.commentAllowed = commentAllowed;
        this.metadata.addAll(metadata);
        this.payerData = payerData;
    }

    protected LnUrlPay(Map<String, Object> data) {
        if (!isAssignableTo(data)) {
            throw new IllegalArgumentException("Data does not represent a pay request");
        }
        this.maxSendable = NGEUtils.safeLong(data.get("maxSendable"));
        this.minSendable = NGEUtils.safeLong(data.get("minSendable"));
        if (this.minSendable < 1 || this.minSendable > this.maxSendable) {
            throw new IllegalArgumentException("Invalid sendable range: " + minSendable + " - " + maxSendable);
        }
        this.callback = NGEUtils.safeURI(data.get("callback"));

        String metaStr = NGEUtils.safeString(data.get("metadata"));
        if (metaStr.isEmpty() || metaStr.length() > MAX_METADATA_SIZE) {
            throw new IllegalArgumentException("Metadata is empty or exceeds maximum size of " + MAX_METADATA_SIZE + " bytes");
        }

        List<Object> metaRaw = NGEPlatform.get().fromJSON(metaStr, List.class);
        for (Object o : metaRaw) {
            if (o.getClass().isArray()) {
                o = List.of((Object[]) o);
            }
            if (o instanceof List && ((List) o).size() == 2) {
                List<?> lo = (List<?>) o;
                String type = NGEUtils.safeString(lo.get(0));
                Object value = lo.get(1);
                if (STR_METADATA_TYPES.contains(type)) {
                    value = NGEUtils.safeString(value);
                }
                metadata.add(new Metadata(type, value));
            }
        }

        Metadata firstMeta = metadata.isEmpty() ? null : metadata.get(0);
        if (firstMeta == null || !firstMeta.type.equals("text/plain")) {
            throw new IllegalArgumentException("First metadata item must be of type 'text/plain'");
        }

        payerData = LnUrlPayerData.fromTemplate((Map<String, Map>) data.get("payerData"));
        commentAllowed = NGEUtils.safeInt(data.get("commentAllowed"));
    }

    public boolean isCommentAllowed() {
        return commentAllowed > 0;
    }

    public int getMaxCommentLength() {
        return commentAllowed;
    }

    public LnUrlPayerData getPayerData() {
        return payerData;
    }

    public boolean canSend(long amount) {
        return amount >= minSendable && amount <= maxSendable;
    }

    public long getMaxSendable() {
        return maxSendable;
    }

    public long getMinSendable() {
        return minSendable;
    }

    public List<Metadata> getMetadata() {
        return metadata;
    }

    public URI getCallback() {
        return callback;
    }

    public URI getCallback(long amount, @Nullable String comment, @Nullable LnUrlPayerData payerData) {
        if (!canSend(amount)) {
            throw new IllegalArgumentException(
                "Amount " + amount + " is not within the allowed range: " + minSendable + " - " + maxSendable
            );
        }
        StringBuilder build = new StringBuilder(getCallback().toString());
        if (build.indexOf("?") < 0) {
            build.append("?");
        } else {
            build.append("&");
        }
        build.append("amount=").append(URLEncoder.encode("" + amount, StandardCharsets.UTF_8));
        if (isCommentAllowed() && comment != null && !comment.isEmpty()) {
            if (comment.length() > getMaxCommentLength()) {
                throw new IllegalArgumentException(
                    "Comment exceeds maximum length of " + getMaxCommentLength() + " characters"
                );
            }
            build.append("&comment=").append(URLEncoder.encode(comment, StandardCharsets.UTF_8));
        }
        if (payerData != null) {
            build.append("&payerdata=").append(URLEncoder.encode(NGEPlatform.get().toJSON(payerData), StandardCharsets.UTF_8));
        }
        return NGEUtils.safeURI(build.toString());
    }

    public AsyncTask<LnUrlPaymentResponse> fetchInvoice(
        long amount,
        @Nullable String comment,
        @Nullable LnUrlPayerData payerData
    ) throws Exception {
        return fetchInvoice(amount, comment, payerData, LnUrl.DEFAULT_TIMEOUT, null);
    }

    public AsyncTask<LnUrlPaymentResponse> fetchInvoice(
        long amount,
        @Nullable String comment,
        @Nullable LnUrlPayerData payerData,
        @Nullable Duration timeout,
        @Nullable Map<String, String> headers
    ) throws Exception {
        URI callback = getCallback(amount, comment, payerData);
        return NGEPlatform
            .get()
            .httpGet(callback.toString(), timeout, headers)
            .then(body -> {
                Map<String, Object> map = NGEPlatform.get().fromJSON(body, Map.class);
                if (LnUrlException.isAssignableTo(map)) {
                    throw new RuntimeException(new LnUrlException(map));
                }
                if (LnUrlPaymentResponse.isAssignableTo(map)) {
                    LnUrlPaymentResponse response = new LnUrlPaymentResponse(map);
                    return response;
                }
                throw new RuntimeException(
                    new LnUrlException(LnUrlException.Status.INVALID, "Invalid LNURL payment response: " + body)
                );
            });
    }

    @Override
    public String getName() {
        return "LNURL Pay Request";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tag", "payRequest");
        map.put("maxSendable", maxSendable);
        map.put("minSendable", minSendable);
        map.put("callback", callback.toString());
        if (commentAllowed > 0) {
            map.put("commentAllowed", commentAllowed);
        }
        if (!metadata.isEmpty()) {
            List<List<Object>> metaList = new ArrayList<>();
            for (Metadata meta : metadata) {
                List<Object> item = new ArrayList<>();
                item.add(meta.type);
                item.add(meta.value);
                metaList.add(item);
            }
            map.put("metadata", NGEPlatform.get().toJSON(metaList));
        }
        if (payerData != null) {
            Map<String, Map> payerDataTemplate = new HashMap<>();
            for (String key : payerData.keySet()) {
                Map<String, Object> field = new HashMap<>();
                field.put("mandatory", payerData.isRequired(key));
                payerDataTemplate.put(key, field);
            }
            map.put("payerData", payerDataTemplate);
        }

        return map;
    }
}
