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
package org.mdpnp.devices.philips.intellivue.action.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.mdpnp.devices.io.util.Bits;
import org.mdpnp.devices.philips.intellivue.action.SingleContextPoll;
import org.mdpnp.devices.philips.intellivue.action.SinglePollDataResult;
import org.mdpnp.devices.philips.intellivue.data.AbsoluteTime;
import org.mdpnp.devices.philips.intellivue.data.OIDType;
import org.mdpnp.devices.philips.intellivue.data.RelativeTime;
import org.mdpnp.devices.philips.intellivue.data.Type;
import org.mdpnp.devices.philips.intellivue.dataexport.command.ActionResult;
import org.mdpnp.devices.philips.intellivue.util.Util;

/**
 * @author Jeff Plourde
 *
 */
public class SinglePollDataResultImpl implements SinglePollDataResult {
    private int pollNumber;
    private final RelativeTime relativeTime = new RelativeTime();
    private final AbsoluteTime absoluteTime = new AbsoluteTime();
    private final Type polledObjectType = new Type();
    private OIDType polledAttrGroup;
    private final List<SingleContextPoll> pollInfoList = new ArrayList<SingleContextPoll>();

    private ActionResult action;

    @Override
    public ActionResult getAction() {
        return action;
    }

    @Override
    public void setAction(ActionResult action) {
        this.action = action;
    }

    private void parse(ByteBuffer bb, boolean more) {
        pollNumber = Bits.getUnsignedShort(bb);
        relativeTime.parse(bb);
        absoluteTime.parse(bb);
        polledObjectType.parse(bb);
        polledAttrGroup = OIDType.parse(bb);

        Util.PrefixLengthShort.read(bb, pollInfoList, !more, SingleContextPollImpl.class);
    }

    @Override
    public void parse(ByteBuffer bb) {
        parse(bb, false);
    }

    @Override
    public void parseMore(ByteBuffer bb) {
        parse(bb, true);
    }

    @Override
    public void format(ByteBuffer bb) {
        Bits.putUnsignedShort(bb, pollNumber);
        relativeTime.format(bb);
        absoluteTime.format(bb);
        polledObjectType.format(bb);
        polledAttrGroup.format(bb);

        Util.PrefixLengthShort.write(bb, pollInfoList);
    }

    @Override
    public int getPollNumber() {
        return pollNumber;
    }

    @Override
    public void setPollNumber(int pollNumber) {
        this.pollNumber = pollNumber;
    }

    @Override
    public RelativeTime getRelativeTime() {
        return relativeTime;
    }

    @Override
    public AbsoluteTime getAbsoluteTime() {
        return absoluteTime;
    }

    @Override
    public Type getPolledObjType() {
        return polledObjectType;
    }

    @Override
    public OIDType getPolledAttributeGroup() {
        return polledAttrGroup;
    }

    @Override
    public List<SingleContextPoll> getPollInfoList() {
        return pollInfoList;
    }

    @Override
    public String toString() {
        return "[pollNumber=" + pollNumber + ",relativeTime=" + relativeTime + ",absoluteTime=" + absoluteTime + ",polledTypeType="
                + polledObjectType + ",polledAttributeGroup=" + polledAttrGroup + ",pollInfoList=" + pollInfoList + "]";
    }

}
