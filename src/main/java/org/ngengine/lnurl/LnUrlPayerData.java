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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ngengine.platform.NGEUtils;

public class LnUrlPayerData extends HashMap<String, Object> {

    public static class Auth extends HashMap<String, Object> {

        public Auth() {
            super();
        }

        public void setK1(String k1) {
            put("k1", k1);
        }

        public String getK1() {
            return NGEUtils.safeString(get("k1"));
        }

        public void setSig(String sig) {
            put("sig", sig);
        }

        public String getSig() {
            return NGEUtils.safeString(get("sig"));
        }
    }

    private final List<String> required = new ArrayList<>();

    public LnUrlPayerData require(String field) {
        if (!required.contains(field)) {
            required.add(field);
        }
        return this;
    }

    public boolean isRequired(String field) {
        return required.contains(field);
    }

    public LnUrlPayerData optional(String field) {
        required.remove(field);
        return this;
    }

    public LnUrlPayerData requireName() {
        return require("name");
    }

    public LnUrlPayerData requirePubkey() {
        return require("pubkey");
    }

    public LnUrlPayerData requireIdentifier() {
        return require("identifier");
    }

    public LnUrlPayerData requireEmail() {
        return require("email");
    }

    public LnUrlPayerData requireAuth() {
        return require("auth");
    }

    public boolean isNameRequired() {
        return isRequired("name");
    }

    public boolean isPubkeyRequired() {
        return isRequired("pubkey");
    }

    public boolean isIdentifierRequired() {
        return isRequired("identifier");
    }

    public boolean isEmailRequired() {
        return isRequired("email");
    }

    public boolean isAuthRequired() {
        return isRequired("auth");
    }

    public String getName() {
        return NGEUtils.safeString(get("name"));
    }

    public String getPubkey() {
        return NGEUtils.safeString(get("pubkey"));
    }

    public String getIdentifier() {
        return NGEUtils.safeString(get("identifier"));
    }

    public String getEmail() {
        return NGEUtils.safeString(get("email"));
    }

    public Auth getAuth() {
        return (Auth) get("auth");
    }

    public void setName(String name) {
        put("name", name);
    }

    public void setPubkey(String pubkey) {
        put("pubkey", pubkey);
    }

    public void setIdentifier(String identifier) {
        put("identifier", identifier);
    }

    public void setEmail(String email) {
        put("email", email);
    }

    public void setAuth(Auth auth) {
        put("auth", auth);
    }

    public static LnUrlPayerData fromTemplate(Map<String, Map> template) {
        LnUrlPayerData data = new LnUrlPayerData();
        if (template != null) {
            for (Entry<String, Map> entry : template.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> value = entry.getValue();
                boolean mandatory = NGEUtils.safeBool(value.get("mandatory"));
                if (mandatory) {
                    data.require(key);
                }
            }
        }
        return data;
    }

    @Override
    public LnUrlPayerData clone() {
        return (LnUrlPayerData) super.clone();
    }
}
