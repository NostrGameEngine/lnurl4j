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

/**
 * A Lightning address
 */
// lud16
public class LnAddress extends LnUrl {

    private final String lnAddress;

    public LnAddress(String lnAddress) throws URISyntaxException {
        super(addressToUrl(lnAddress));
        this.lnAddress = lnAddress.trim();
    }

    private static URI addressToUrl(String addr) throws URISyntaxException {
        if (addr == null || addr.isEmpty()) {
            throw new URISyntaxException(addr, "Lightning address cannot be null or empty", 0);
        }

        // Split the address into username and domain parts
        String[] parts = addr.split("@");
        if (parts.length != 2) {
            throw new URISyntaxException(addr, "Invalid lightning address format", 0);
        }

        String username = parts[0];
        String domain = parts[1];

        // Validate username - only a-z0-9-_.+ are allowed
        if (!username.matches("^[a-z0-9\\-_.+]+$")) {
            throw new URISyntaxException(addr, "Invalid username format. Only a-z0-9-_.+ characters are allowed.", 0);
        }

        // Determine if it's a clearnet or onion domain
        String scheme = domain.endsWith(".onion") ? "http" : "https";

        // Construct the URL
        String url = scheme + "://" + domain + "/.well-known/lnurlp/" + username;

        return new URI(url);
    }

    @Override
    public String toString() {
        return lnAddress;
    }
}
