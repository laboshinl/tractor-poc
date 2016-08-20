package ru.ownrobot.tractor;

import akka.util.ByteString;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by laboshinl on 8/19/16.
 */
public class Packet implements Serializable {
    public Boolean isSyn;
    public Boolean isFin;
    public Boolean isRst;
    public Boolean isAck;
    public Boolean isPush;
    public String proto;
    public String ipSrc;
    public String ipDst;
    public Integer srcPort;
    public Integer dstPort;
    public Integer size;
    public Date time;
    public String protocol;
    public Integer windowSize;

    public Packet(String proto, String ipSrc, Integer srcPort, String ipDst, Integer dstPort, Integer size, Date time, Boolean isAck, Boolean isPush, Boolean isRst, Boolean isSyn, Boolean isFin,  Integer windowSize, String protocol ){
        this.isSyn = isSyn;
        this.ipSrc = ipSrc;
        this.ipDst = ipDst;
        this.isFin = isFin;
        this.isRst = isRst;
        this.isPush = isPush;
        this.isAck = isAck;
        this.proto = proto;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.size = size;
        this.time = time;
        this.windowSize = windowSize;
        this.protocol = protocol;
    }
}
