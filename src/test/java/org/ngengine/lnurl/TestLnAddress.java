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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.ngengine.bech32.Bech32DecodingException;
import org.ngengine.bech32.Bech32EncodingException;
import org.ngengine.bech32.Bech32InvalidChecksumException;
import org.ngengine.bech32.Bech32InvalidRangeException;

public class TestLnAddress {

    @Test
    public void testLnAddress()
        throws Bech32EncodingException, URISyntaxException, Bech32DecodingException, Bech32InvalidChecksumException, Bech32InvalidRangeException {
        String lnAddress = "unit@lntest.rblb.it";
        String lnUrl = "https://lntest.rblb.it/.well-known/lnurlp/unit";
        LnAddress lnAddressObj = new LnAddress(lnAddress);
        assertEquals(lnAddressObj.toURI().toString(), lnUrl);
    }

    @Test
    public void getInvoice() throws Exception {
        LnAddress lnAddressObj = new LnAddress("unit@lntest.rblb.it");
        LnUrlPay service = (LnUrlPay) lnAddressObj.getService().await();
        assertEquals(service.canSend(1000), true);
        LnUrlPayerData payerData = new LnUrlPayerData();
        payerData.setName("Test Payer");

        URI callback = service.getCallback(1000, "test payment", payerData);
        System.out.println("Callback URL: " + callback);
        assertEquals(callback.toString().contains("Test+Payer"), true);

        LnUrlPaymentResponse resp = service.fetchInvoice(1000, "test payment", payerData).await();
        assertEquals(resp.getPr().startsWith("lnbc"), true);
    }
}
