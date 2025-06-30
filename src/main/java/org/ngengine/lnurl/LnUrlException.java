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

import java.util.Map;
import org.ngengine.platform.NGEUtils;

public class LnUrlException extends Exception implements LnUrlService {

    public enum Status {
        OK,
        ERROR,
        NOT_FOUND,
        INVALID,
    }

    private final Status status;
    private transient Map<String, Object> map;

    public LnUrlException(Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public LnUrlException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public LnUrlException(Map<String, Object> data) {
        super(NGEUtils.safeString(data.get("reason")));
        String statusStr = NGEUtils.safeString(data.get("status"));
        this.status = Status.valueOf(statusStr != null ? statusStr.toUpperCase() : "ERROR");
    }

    @Override
    public String getMessage() {
        return "LnUrlServiceException: " + super.getMessage() + " (Status: " + status + ")";
    }

    public static boolean isAssignableTo(Map<String, Object> data) {
        return "ERROR".equals(data.get("status"));
    }

    @Override
    public Map<String, Object> toMap() {
        if (this.map == null) {
            this.map = Map.of("status", status.name(), "reason", getMessage());
        }
        return this.map;
    }

    @Override
    public String getName() {
        return "LnUrl Service Exception";
    }
}
