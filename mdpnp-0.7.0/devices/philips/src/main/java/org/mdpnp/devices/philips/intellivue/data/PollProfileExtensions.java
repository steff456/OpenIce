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
package org.mdpnp.devices.philips.intellivue.data;

import java.nio.ByteBuffer;

import org.mdpnp.devices.io.util.Bits;

/**
 * @author Jeff Plourde
 *
 */
public class PollProfileExtensions implements Value {

    private long pollProfileExtOptions = POLL_EXT_PERIOD_NU_1SEC | POLL_EXT_PERIOD_RTSA | POLL_EXT_NU_PRIO_LIST | POLL_EXT_ENUM;
    public static final long POLL_EXT_PERIOD_NU_1SEC = 0x80000000L;
    public static final long POLL_EXT_PERIOD_NU_AVG_12SEC = 0x40000000L;
    public static final long POLL_EXT_PERIOD_NU_AVG_60SEC = 0x20000000L;
    public static final long POLL_EXT_PERIOD_NU_AVG_300SEC = 0x10000000L;
    public static final long POLL_EXT_PERIOD_RTSA = 0x08000000L;
    public static final long POLL_EXT_ENUM = 0x04000000L;
    public static final long POLL_EXT_NU_PRIO_LIST = 0x02000000L;
    public static final long POLL_EXT_DYN_MODALITIES = 0x01000000L;

    private final AttributeValueList ext_attr = new AttributeValueList();

    @Override
    public void parse(ByteBuffer bb) {
        pollProfileExtOptions = Bits.getUnsignedInt(bb);
        ext_attr.parse(bb);
    }

    @Override
    public void format(ByteBuffer bb) {
        Bits.putUnsignedInt(bb, pollProfileExtOptions);
        ext_attr.format(bb);
    }

    @Override
    public java.lang.String toString() {
        return "[pollProfileExtOptions=" + Long.toHexString(pollProfileExtOptions) + ",ext_attr=" + ext_attr + "]";
    }

}
