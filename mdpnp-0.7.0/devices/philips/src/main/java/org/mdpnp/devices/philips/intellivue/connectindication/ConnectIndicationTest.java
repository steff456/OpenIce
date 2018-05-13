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
package org.mdpnp.devices.philips.intellivue.connectindication;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.mdpnp.devices.io.util.HexUtil;
import org.mdpnp.devices.philips.intellivue.Network;

/**
 * @author Jeff Plourde
 *
 */
public class ConnectIndicationTest {
    private interface ConnectIndicationCallback {
        void beacon(ConnectIndication ci);
    }

    public ConnectIndicationTest(List<Network.AddressSubnet> addrs, ConnectIndicationCallback callback) throws IOException {
        Selector select = Selector.open();

        for (Network.AddressSubnet a : addrs) {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.socket().setReuseAddress(true);
            channel.socket().bind(new InetSocketAddress(a.getInetAddress(), 24005));
            channel.register(select, SelectionKey.OP_READ);
        }

        ByteBuffer bb = ByteBuffer.allocate(5000);
        bb.order(ByteOrder.BIG_ENDIAN);
        while (true) {
            select.select();
            Set<SelectionKey> keys = select.selectedKeys();
            for (SelectionKey sk : keys) {
                if (sk.isReadable()) {
                    SocketAddress addr = ((DatagramChannel) sk.channel()).receive(bb);
                    RandomAccessFile file = new RandomAccessFile("connectind", "rw");

                    bb.flip();
                    bb.mark();
                    file.getChannel().write(bb);
                    bb.reset();
                    file.close();

                    bb.reset();
                    System.out.println(HexUtil.dump(bb, 100));
                    ConnectIndicationImpl ci = new ConnectIndicationImpl();
                    ci.parse(bb);
                    if (callback != null) {
                        callback.beacon(ci);
                    }
                    System.out.print(addr + " " + ci);

                    bb.clear();
                    System.out.println();
                }

            }
        }

    }

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Beacon");
        frame.setSize(640, 480);

        frame.getContentPane().setLayout(new BorderLayout());
        final JButton button = new JButton("SEND BEACON");
        final JTextArea text = new JTextArea();
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        frame.getContentPane().add(button, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(text), BorderLayout.CENTER);
        System.setProperty("java.net.preferIPv4Stack", "true");

        final List<Network.AddressSubnet> address = Network.getBroadcastAddresses();

        System.out.println(address);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConnectIndicationImpl ci = new ConnectIndicationImpl();
                try {
                    ci.getIpAddressInformation().setInetAddress(Network.getLocalAddresses().get(0));
                } catch (SocketException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
                ByteBuffer bb = ByteBuffer.allocate(5000);
                bb.order(ByteOrder.BIG_ENDIAN);
                ci.format(bb);
                byte[] bytes = new byte[bb.position()];
                bb.position(0);
                bb.get(bytes);

                DatagramSocket ds;
                try {
                    ds = new DatagramSocket();
                    DatagramPacket dp = new DatagramPacket(bytes, bytes.length, null, 24005);

                    for (int i = 0; i < bytes.length; i++) {
                        System.out.print(Integer.toHexString(0xFF & bytes[i]) + " ");
                    }
                    System.out.println();

                    for (Network.AddressSubnet as : address) {
                        System.out.println("Transmit to " + as.getInetAddress());

                        dp.setAddress(as.getInetAddress());

                        ds.send(dp);
                    }
                } catch (SocketException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        new ConnectIndicationTest(address, new ConnectIndicationCallback() {

            @Override
            public void beacon(ConnectIndication ci) {
                text.setText(ci.toString());
            }
        });

    }
}
