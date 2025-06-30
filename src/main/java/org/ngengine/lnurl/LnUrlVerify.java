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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.ngengine.platform.NGEUtils;

/**
 * The lnurl verify service.
 */
// lud-21
public class LnUrlVerify implements LnUrlService {

    private final boolean settled;
    private final String pr;
    private final String preimage;
    private transient Map<String, Object> map;

    protected static boolean isAssignableTo(Map<String, Object> data) {
        return data.containsKey("settled") && data.containsKey("pr");
    }

    protected LnUrlVerify(Map<String, Object> data) {
        if (!isAssignableTo(data)) {
            throw new IllegalArgumentException("Data does not match LnUrlVerify structure");
        }
        this.settled = NGEUtils.safeBool(data.get("settled"));
        this.pr = NGEUtils.safeString(data.get("pr"));
        this.preimage = data.containsKey("preimage") ? NGEUtils.safeString(data.get("preimage")) : null;
    }

    /**
     * Get the preimage of the payment request if it was settled.
     * @return The preimage of the payment request, or null if not provided.
     */
    @Nullable
    public String getPreimage() {
        return preimage;
    }

    /**
     * Get the invoice
     * @return  the invoice
     */
    public String getPr() {
        return pr;
    }

    /**
     * Check if the payment request was settled.
     * @return true if the payment request was settled, false otherwise.
     */
    public boolean isSettled() {
        return settled;
    }

    @Override
    public String getName() {
        return "Verify Service";
    }

    @Override
    public Map<String, Object> toMap() {
        if (this.map == null) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("settled", settled);
            map.put("pr", pr);
            if (preimage != null) {
                map.put("preimage", preimage);
            }
            this.map = Collections.unmodifiableMap(map);
        }
        return this.map;
    }
}
