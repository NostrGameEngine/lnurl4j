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

public class LnUrlMessageSuccessAction implements LnUrlSuccessAction {

    private static final int MAX_DESCRIPTION_LENGTH = 144;

    private final String message;
    private transient Map<String, Object> map;

    public LnUrlMessageSuccessAction(String message) {
        this.message = message;
        check();
    }

    public LnUrlMessageSuccessAction(Map<String, Object> data) {
        if (!isAssignableTo(data)) {
            throw new IllegalArgumentException("Data does not match the structure");
        }
        this.message = NGEUtils.safeString(data.get("message"));
        check();
    }

    private void check() {
        if (this.message.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    public String getMessage() {
        return message;
    }

    @Override
    public Map<String, Object> toMap() {
        if (this.map == null) {
            Map<String, Object> map = new HashMap<>();
            map.put("tag", "message");
            map.put("message", message);
            this.map = Collections.unmodifiableMap(map);
        }
        return this.map;
    }

    public static boolean isAssignableTo(Map<String, Object> data) {
        return "message".equals(data.get("tag"));
    }
}
