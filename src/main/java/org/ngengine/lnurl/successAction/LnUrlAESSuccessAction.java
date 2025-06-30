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

package org.ngengine.lnurl.successAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.ngengine.platform.NGEUtils;

public class LnUrlAESSuccessAction implements LnUrlSuccessAction {

    private static final int MAX_DESCRIPTION_LENGTH = 144;
    private static final int MAX_CIPHERTEXT_LENGTH = 1024 * 4; // 4 KB
    private static final int IV_LENGTH = 24;

    private final String description;
    private final String ciphertext;
    private final String iv;

    private transient Map<String, Object> map;

    public LnUrlAESSuccessAction(String description, String ciphertext, String iv) {
        this.description = description;
        this.ciphertext = ciphertext;
        this.iv = iv;
        check();
    }

    public LnUrlAESSuccessAction(Map<String, Object> data) {
        if (!isAssignableTo(data)) {
            throw new IllegalArgumentException("Data does not match LnUrlAESSuccessAction structure");
        }
        this.description = NGEUtils.safeString(data.get("description"));
        this.ciphertext = NGEUtils.safeString(data.get("ciphertext"));
        this.iv = NGEUtils.safeString(data.get("iv"));
        check();
    }

    private void check() {
        if (this.description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed 144 characters");
        }
        if (this.ciphertext.length() > MAX_CIPHERTEXT_LENGTH) {
            throw new IllegalArgumentException("Ciphertext cannot exceed " + MAX_CIPHERTEXT_LENGTH + " bytes");
        }
        if (this.iv.length() != IV_LENGTH) {
            throw new IllegalArgumentException("IV must be exactly " + IV_LENGTH + " bytes long");
        }
    }

    public static boolean isAssignableTo(Map<String, Object> data) {
        return "aes".equals(data.get("tag"));
    }

    public String getDescription() {
        return description;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIv() {
        return iv;
    }

    @Override
    public Map<String, Object> toMap() {
        if (this.map == null) {
            Map<String, Object> map = new HashMap<>();
            map.put("tag", "aes");
            map.put("description", description);
            map.put("ciphertext", ciphertext);
            map.put("iv", iv);
            this.map = Collections.unmodifiableMap(map);
        }
        return this.map;
    }
}
