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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ngengine.lnurl.successAction.LnUrlAESSuccessAction;
import org.ngengine.lnurl.successAction.LnUrlMessageSuccessAction;
import org.ngengine.lnurl.successAction.LnUrlSuccessAction;
import org.ngengine.lnurl.successAction.LnUrlUrlSuccessAction;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

public class LnUrlPaymentResponse {

    public static class SuccessActionProcessor {
        private final Function<Map<String, Object>, Boolean> isAssignableTo;
        private final BiFunction<LnUrlPay, Map<String, Object>, LnUrlSuccessAction> constructor;

        public SuccessActionProcessor(
            Function<Map<String, Object>, Boolean> isAssignableTo,
            BiFunction<LnUrlPay, Map<String, Object>, LnUrlSuccessAction> constructor
        ) {
            this.isAssignableTo = isAssignableTo;
            this.constructor = constructor;
        }

        public boolean isAssignableTo(Map<String, Object> data) {
            return isAssignableTo.apply(data);
        }

        public LnUrlSuccessAction construct(LnUrlPay req, Map<String, Object> data) {
            return constructor.apply(req, data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isAssignableTo, constructor);   
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SuccessActionProcessor other = (SuccessActionProcessor) obj;
            return Objects.equals(isAssignableTo, other.isAssignableTo) && Objects.equals(constructor,
                    other.constructor);
        }


    }

    private static final List<SuccessActionProcessor> successActionsProcessors = new ArrayList<>();

    public static void registerSuccessActionProcessor(SuccessActionProcessor p) {
        successActionsProcessors.add(p);
    }

    static {
        registerSuccessActionProcessor(
            new SuccessActionProcessor(
                LnUrlMessageSuccessAction::isAssignableTo,
                (req, data) -> new LnUrlMessageSuccessAction(data)
            )
        );
        registerSuccessActionProcessor(
            new SuccessActionProcessor(
                LnUrlUrlSuccessAction::isAssignableTo,
                (req, data) -> new LnUrlUrlSuccessAction(req, data)
            )
        );
        registerSuccessActionProcessor(
            new SuccessActionProcessor(LnUrlAESSuccessAction::isAssignableTo, (req, data) -> new LnUrlAESSuccessAction(data))
        );
    }

    private final String pr;
    private final boolean disposable;
    private final URI verify;
    private LnUrlSuccessAction successAction;

    public LnUrlPaymentResponse(String pr, boolean disposable, URI verify, @Nonnull LnUrlSuccessAction successAction) {
        this.pr = pr;
        this.disposable = disposable;
        this.verify = verify;
        this.successAction = successAction;
    }

    public LnUrlPaymentResponse(Map<String, Object> data) {
        if (!data.containsKey("pr")) throw new IllegalArgumentException("Data does not contain 'pr' field");
        this.pr = NGEUtils.safeString(data.get("pr"));
        this.disposable = NGEUtils.safeBool(data.getOrDefault("disposable", true));

        String verifyRaw = (String) data.get("verify");
        this.verify = verifyRaw != null ? NGEUtils.safeURI(verifyRaw) : null;

        Map<String, Object> rawSuccessAction = (Map<String, Object>) data.get("successAction");
        if (rawSuccessAction != null) {
            for (int i = successActionsProcessors.size() - 1; i >= 0; i--) {
                SuccessActionProcessor p = successActionsProcessors.get(i);
                if (p.isAssignableTo.apply(rawSuccessAction)) {
                    this.successAction = p.constructor.apply(null, rawSuccessAction);
                    break;
                }
            }
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("pr", pr);
        map.put("disposable", disposable);

        if (verify != null) {
            map.put("verify", verify.toString());
        }
        if (successAction != null) {
            map.put("successAction", successAction.toMap());
        }
        return map;
    }

    @Nullable
    public LnUrlSuccessAction getSuccessAction() {
        return successAction;
    }

    public static boolean isAssignableTo(Map<String, Object> data) {
        return data.containsKey("pr") && !data.containsKey("tag");
    }

    public String getPr() {
        return pr;
    }

    public boolean isDisposable() {
        return disposable;
    }

    public boolean isVerificable() {
        return verify != null;
    }

    public AsyncTask<LnUrlVerify> verify(Duration timeout) throws IOException, InterruptedException {
        if (!isVerificable()) {
            throw new IllegalStateException("This payment response is not verificable");
        }
        return NGEPlatform
            .get()
            .httpGet(verify.toString(), timeout, null)
            .then(body -> {
                Map<String, Object> data = NGEPlatform.get().fromJSON(body, Map.class);
                if (LnUrlException.isAssignableTo(data)) {
                    throw new RuntimeException(new LnUrlException(data));
                }
                if (LnUrlVerify.isAssignableTo(data)) {
                    return new LnUrlVerify(data);
                } else {
                    throw new RuntimeException(
                        new LnUrlException(LnUrlException.Status.INVALID, "Response does not contain a valid verify structure")
                    );
                }
            });
    }
}
