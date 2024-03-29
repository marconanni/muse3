// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   OverallStats.java

package com.sun.media.rtp;

import javax.media.rtp.GlobalReceptionStats;

public class OverallStats
    implements GlobalReceptionStats
{

    public OverallStats()
    {
        numPackets = 0;
        numBytes = 0;
        numBadRTPPkts = 0;
        numLocalColl = 0;
        numRemoteColl = 0;
        numPktsLooped = 0;
        numTransmitFailed = 0;
        numRTCPRecd = 0;
        numSRRecd = 0;
        numBadRTCPPkts = 0;
        numUnknownTypes = 0;
        numMalformedRR = 0;
        numMalformedSDES = 0;
        numMalformedBye = 0;
        numMalformedSR = 0;
    }

    public synchronized void update(int which, int num)
    {
        switch(which)
        {
        case 0: // '\0'
            numPackets += num;
            break;

        case 1: // '\001'
            numBytes += num;
            break;

        case 2: // '\002'
            numBadRTPPkts += num;
            break;

        case 3: // '\003'
            numLocalColl += num;
            break;

        case 4: // '\004'
            numRemoteColl += num;
            break;

        case 5: // '\005'
            numPktsLooped += num;
            break;

        case 6: // '\006'
            numTransmitFailed += num;
            break;

        case 11: // '\013'
            numRTCPRecd += num;
            break;

        case 12: // '\f'
            numSRRecd += num;
            break;

        case 13: // '\r'
            numBadRTPPkts += num;
            break;

        case 14: // '\016'
            numUnknownTypes += num;
            break;

        case 15: // '\017'
            numMalformedRR += num;
            break;

        case 16: // '\020'
            numMalformedSDES += num;
            break;

        case 17: // '\021'
            numMalformedBye += num;
            break;

        case 18: // '\022'
            numMalformedSR += num;
            break;
        }
    }

    public int getPacketsRecd()
    {
        return numPackets;
    }

    public int getBytesRecd()
    {
        return numBytes;
    }

    public int getBadRTPkts()
    {
        return numBadRTPPkts;
    }

    public int getLocalColls()
    {
        return numLocalColl;
    }

    public int getRemoteColls()
    {
        return numRemoteColl;
    }

    public int getPacketsLooped()
    {
        return numPktsLooped;
    }

    public int getTransmitFailed()
    {
        return numTransmitFailed;
    }

    public int getRTCPRecd()
    {
        return numRTCPRecd;
    }

    public int getSRRecd()
    {
        return numSRRecd;
    }

    public int getBadRTCPPkts()
    {
        return numBadRTCPPkts;
    }

    public int getUnknownTypes()
    {
        return numUnknownTypes;
    }

    public int getMalformedRR()
    {
        return numMalformedRR;
    }

    public int getMalformedSDES()
    {
        return numMalformedSDES;
    }

    public int getMalformedBye()
    {
        return numMalformedBye;
    }

    public int getMalformedSR()
    {
        return numMalformedSR;
    }

    public String toString()
    {
        String s = "Packets Recd " + getPacketsRecd() + "\nBytes Recd " + getBytesRecd() + "\ngetBadRTP " + getBadRTPkts() + "\nLocalColl " + getLocalColls() + "\nRemoteColl " + getRemoteColls() + "\nPacketsLooped " + getPacketsLooped() + "\ngetTransmitFailed " + getTransmitFailed() + "\nRTCPRecd " + getTransmitFailed() + "\nSRRecd " + getSRRecd() + "\nBadRTCPPkts " + getBadRTCPPkts() + "\nUnknown " + getUnknownTypes() + "\nMalformedRR " + getMalformedRR() + "\nMalformedSDES " + getMalformedSDES() + "\nMalformedBye " + getMalformedBye() + "\nMalformedSR " + getMalformedSR();
        return s;
    }

    public static final int PACKETRECD = 0;
    public static final int BYTESRECD = 1;
    public static final int BADRTPPACKET = 2;
    public static final int LOCALCOLL = 3;
    public static final int REMOTECOLL = 4;
    public static final int PACKETSLOOPED = 5;
    public static final int TRANSMITFAILED = 6;
    public static final int RTCPRECD = 11;
    public static final int SRRECD = 12;
    public static final int BADRTCPPACKET = 13;
    public static final int UNKNOWNTYPE = 14;
    public static final int MALFORMEDRR = 15;
    public static final int MALFORMEDSDES = 16;
    public static final int MALFORMEDBYE = 17;
    public static final int MALFORMEDSR = 18;
    private int numPackets;
    private int numBytes;
    private int numBadRTPPkts;
    private int numLocalColl;
    private int numRemoteColl;
    private int numPktsLooped;
    private int numTransmitFailed;
    private int numRTCPRecd;
    private int numSRRecd;
    private int numBadRTCPPkts;
    private int numUnknownTypes;
    private int numMalformedRR;
    private int numMalformedSDES;
    private int numMalformedBye;
    private int numMalformedSR;
}
