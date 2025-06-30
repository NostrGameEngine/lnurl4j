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

import java.util.List;

/**
 * Lud16 url types with their prefixes and tags.
 */
public record Lud16Type(String prefix, String tag) {
    public boolean isAssignableTo(String lnurl) {
        return lnurl.toLowerCase().trim().startsWith(prefix + "://");
    }

    public String toHttps(String url) {
        if (!isAssignableTo(url)) {
            throw new IllegalArgumentException("URL does not start with the expected prefix: " + prefix);
        }
        return "https://" + url.substring(prefix.length() + 3); // +3 for "://"
    }

    private static final List<Lud16Type> LUD_16_TYPES = List.of(
        new Lud16Type("lnurlc", "channelRequest"),
        new Lud16Type("lnurlw", "withdrawRequest"),
        new Lud16Type("lnurlp", "payRequest"),
        new Lud16Type("keyauth", "login")
    );

    public static boolean isLud16(String lnurl) {
        if (lnurl == null || lnurl.isEmpty()) return false;
        return LUD_16_TYPES.stream().anyMatch(prefix -> prefix.isAssignableTo(lnurl));
    }

    public static Lud16Type getLud16(String lnurl) {
        if (lnurl == null || lnurl.isEmpty()) return null;
        return LUD_16_TYPES.stream().filter(prefix -> prefix.isAssignableTo(lnurl)).findFirst().orElse(null);
    }
}
