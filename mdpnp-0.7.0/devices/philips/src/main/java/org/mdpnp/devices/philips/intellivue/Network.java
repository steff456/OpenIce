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
package org.mdpnp.devices.philips.intellivue;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.mdpnp.devices.io.util.Bits;
import org.mdpnp.devices.io.util.HexUtil;

/**
 * @author Jeff Plourde
 *
 */
public class Network {
    public static class AddressSubnet {
        private final InetAddress inetAddress;
        private final InetAddress localAddress;
        private final short prefixLength;

        public AddressSubnet(InetAddress inetAddress, short prefixLength, InetAddress localAddress) {
            this.inetAddress = inetAddress;
            this.prefixLength = prefixLength;
            this.localAddress = localAddress;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public short getPrefixLength() {
            return prefixLength;
        }

        public InetAddress getLocalAddress() {
            return localAddress;
        }

        @Override
        public String toString() {
            return inetAddress.toString() + "/" + prefixLength;
        }
    }

    public static final List<AddressSubnet> getBroadcastAddresses() throws SocketException {
        final List<AddressSubnet> address = new ArrayList<AddressSubnet>();

        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        while (n.hasMoreElements()) {
            NetworkInterface ni = n.nextElement();
            if (ni.isLoopback()) {
                continue;
            }
            for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                InetAddress iaddr = addr.getBroadcast();
                if (iaddr != null) {
                    address.add(new AddressSubnet(iaddr, addr.getNetworkPrefixLength(), addr.getAddress()));
                }
            }
        }
        return address;
    }

    public static final List<InetAddress> getLocalAddresses() throws SocketException {
        return getLocalAddresses(null, null, true);
    }

    public static final long createMask(short prefix) {
        long mask = 0L;

        for (short i = 0; i < prefix; i++) {
            mask |= (1L << (Integer.SIZE - i - 1));
        }
        return mask;
    }

    public static final void prefix(byte[] mask, short prefixLength) {
        for (int i = 0; i < mask.length; i++) {
            mask[i] = 0;
        }

        for (int i = 0; i < prefixLength; i++) {
            mask[i / Byte.SIZE] |= (1 << (Byte.SIZE - (i % Byte.SIZE) - 1));
        }

    }

    public static final short prefixCount(byte[] subnetmask) {
        int cnt = Byte.SIZE * subnetmask.length;

        for (int i = 0; i < cnt; i++) {
            int b = 0xFF & subnetmask[i / Byte.SIZE];
            if (0 == (b & (1 << (Byte.SIZE - (i % Byte.SIZE) - 1)))) {
                return (short) (i);
            }
        }
        return (short) cnt;
    }

    public static final List<InetAddress> getLocalAddresses(InetAddress remoteAddr, Short prefixLength, boolean ipv4only) throws SocketException {
        final List<InetAddress> address = new ArrayList<InetAddress>();

        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        while (n.hasMoreElements()) {
            NetworkInterface ni = n.nextElement();
            if (ni.isLoopback() || !ni.supportsMulticast()) {
                continue;
            }
            for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                InetAddress iaddr = addr.getAddress();

                if (iaddr != null) {
                    if (null != remoteAddr && null != prefixLength) {
                        long local = Bits.getUnsignedInt(iaddr.getAddress());
                        long remote = Bits.getUnsignedInt(remoteAddr.getAddress());
                        if (0 == (createMask(prefixLength) & (local ^ remote))) {
                            if (!ipv4only || iaddr instanceof Inet4Address) {
                                address.add(iaddr);
                            }
                        }

                    } else {
                        address.add(iaddr);
                    }
                }
            }
        }
        return address;
    }

    public static void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println(Long.toHexString(createMask((short) 24)));
        InetAddress addr = InetAddress.getByAddress(new byte[] { -64, -88, 1, -57 });
        System.out.println(getLocalAddresses(addr, (short) 24, true));
        byte[] subnet = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0 };
        System.out.println(HexUtil.dump(ByteBuffer.wrap(subnet)));
        System.out.println(prefixCount(subnet));
        prefix(subnet, prefixCount(subnet));
        System.out.println(HexUtil.dump(ByteBuffer.wrap(subnet)));

    }
}
