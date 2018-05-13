/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.devices.philips.intellivue.association.impl;

import java.nio.ByteBuffer;

import org.mdpnp.devices.io.util.HexUtil;
import org.mdpnp.devices.philips.intellivue.association.AssociationAccept;
import org.mdpnp.devices.philips.intellivue.association.AssociationMessageType;
import org.mdpnp.devices.philips.intellivue.association.MDSEUserInfoStd;
import org.mdpnp.devices.philips.intellivue.data.ASNLength;

/**
 * @author Jeff Plourde
 *
 */
public class AssociationAcceptImpl extends AbstractAssociationMessage implements AssociationAccept {

    private final ASNLength length = new ASNLength();
    private final MDSEUserInfoStd userInfo = new MDSEUserInfoStd();

    @Override
    public void parse(ByteBuffer bb) {
        length.parse(bb);
        userInfo.parse(bb);
    }

    @Override
    public void format(ByteBuffer bb) {
        ByteBuffer bb1 = ByteBuffer.allocate(3000);
        userInfo.format(bb1);
        length.setLength(bb1.position());
        length.format(bb);
        bb1.flip();
        bb.put(bb1);

    }

    @Override
    public AssociationMessageType getType() {
        return AssociationMessageType.Accept;
    }

    private final static byte[] PRESENTATION_HEADER = new byte[] { 0x31, (byte) 0x80, (byte) 0xA0, (byte) 0x80, (byte) 0x80, 0x01, 0x01, 0x00, 0x00,
            (byte) 0xA2, (byte) 0x80, (byte) 0xA0, 0x03, 0x00, 0x00, 0x01, (byte) 0xA5, (byte) 0x80, 0x30, (byte) 0x80, (byte) 0x80, 0x01, 0x00,
            (byte) 0x81, 0x02, 0x51, 0x01, 0x00, 0x00, 0x30, (byte) 0x80, (byte) 0x80, 0x01, 0x00, (byte) 0x81, 0x0C, 0x2A, (byte) 0x86, 0x48,
            (byte) 0xCE, 0x14, 0x02, 0x01, 0x00, 0x00, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x61, (byte) 0x80, 0x30, (byte) 0x80, 0x02, 0x01,
            0x01, (byte) 0xA0, (byte) 0x80, 0x61, (byte) 0x80, (byte) 0xA1, (byte) 0x80, 0x06, 0x0C, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x14,
            0x02, 0x01, 0x00, 0x00, 0x00, 0x03, 0x01, 0x00, 0x00, (byte) 0xA2, 0x03, 0x02, 0x01, 0x00, (byte) 0xA3, 0x05, (byte) 0xA1, 0x03, 0x02,
            0x01, 0x00, (byte) 0xBE, (byte) 0x80, 0x28, (byte) 0x80, 0x02, 0x01, 0x02, (byte) 0x81

    };
    private final static byte[][] presentationHeaderEnds = new byte[][] { { (byte) 0xBE, (byte) 0x80, 0x28, (byte) 0x80, (byte) 0x81 },
            { (byte) 0xBE, (byte) 0x80, 0x28, (byte) 0x80, 0x02, 0x01, 0x02, (byte) 0x81 } };

    @Override
    public boolean advancePastPresentationHeader(ByteBuffer bb) {
        return HexUtil.advancePast(bb, presentationHeaderEnds);
    }

    @Override
    public byte[] getPresentationHeader() {
        return PRESENTATION_HEADER;
    }

    @Override
    public byte[] getPresentationTrailer() {
        return PRESENTATION_TRAILER;
    }

    private static final byte[] PRESENTATION_TRAILER = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00 };

    @Override
    public String toString() {
        return "[type=" + getType() + ",length=" + length + ",userData=" + userInfo + "]";
    }

    @Override
    public MDSEUserInfoStd getUserInfo() {
        return userInfo;
    }
}
