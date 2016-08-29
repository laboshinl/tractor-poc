package ru.ownrobot.tractor;

import com.esotericsoftware.kryo.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by laboshinl on 8/19/16.
 */

//@attribute numPackSent numeric
//@attribute numPackRec numeric
//@attribute totalPackets numeric
//@attribute numberAck numeric
//@attribute headerBytesSent numeric
//@attribute headerBytesRec numeric
//@attribute totalHeaderBytes numeric
//@attribute caplenSent numeric
//@attribute caplenRec numeric
//@attribute minPackLength numeric
//@attribute maxPackLength numeric
//@attribute avPackLength numeric
//@attribute avPayload numeric
//@attribute payloadSent numeric
//@attribute payloadRec numeric
//@attribute totalPayload numeric
//@attribute totalSize numeric

//    libprotoident
//            * Application protocol (as reported by libprotoident)
//            * ID number for the application protocol
//            * Total number of packets sent from first endpoint to second endpoint
//            * Total number of bytes sent from first endpoint to second endpoint
//            * Total number of packets sent from second endpoint to first endpoint
//            * Total number of bytes sent from second endpoint to first endpoint
//            * Minimum payload size sent from first endpoint to second endpoint
//            * Mean payload size sent from first endpoint to second endpoint
//            * Maximum payload size sent from first endpoint to second endpoint
//            * Standard deviation of payload size sent from first endpoint to
//            second endpoint
//            * Minimum payload size sent from second endpoint to first endpoint
//            * Mean payload size sent from second endpoint to first endpoint
//            * Maximum payload size sent from second endpoint to first endpoint
//            * Standard deviation of payload size sent from second endpoint to
//            first endpoint
//            * Minimum packet interarrival time for packets sent from first
//            endpoint to second endpoint
//            * Mean packet interarrival time for packets sent from first
//            endpoint to second endpoint
//            * Maximum packet interarrival time for packets sent from first
//            endpoint to second endpoint
//            * Standard deviation of packet interarrival time for packets sent from
//            first endpoint to second endpoint
//            * Minimum packet interarrival time for packets sent from second
//            endpoint to first endpoint
//            * Mean packet interarrival time for packets sent from second
//            endpoint to first endpoint
//            * Maximum packet interarrival time for packets sent from second
//            endpoint to first endpoint
//            * Standard deviation of packet interarrival time for packets sent from
//            second endpoint to first endpoint
//            * Flow duration (in microseconds)
//            * Flow start time (as a Unix timestamp)

public class FlowStat implements Serializable, Comparable<FlowStat> {
    public String serverIp;
    public String clientIp;
    public Long totalCount;
    public Long accSize;

    public Long clientCount;
    public Long serverCount;

    public Integer serverSentMax;
    public Integer clientSentMax;
    public Integer serverSentMin;
    public Integer clientSentMin;

    public HashMap<String, Integer> protocols;

    public Date firstTime;
    public Date lastTime;

    public String proto;

    public Long synCount;
    public Long rstCount;
    public Long finCount;
    public Long ackCount;
    public Long pushCount;

    public Integer minWindowSize;
    public Integer maxWindowSize;

    public Long accWindowSize;

    public Integer clientPort;
    public Integer serverPort;
    public Long accClientSize;
    public Long accServerSize;

    public FlowStat(){
        this.totalCount = new Long(0);
        this.accSize = new Long(0);
        this.clientCount = new Long (0);
        this.serverCount = new Long(0);
        this.serverSentMax = 0;
        this.clientSentMax = 0;
        this.serverSentMin = Integer.MAX_VALUE;
        this.clientSentMin = Integer.MAX_VALUE;
        this.firstTime = new Date(Long.MAX_VALUE);
        this.lastTime = new Date(Long.MIN_VALUE);
        this.proto = "NONE";
        this.synCount = new Long(0);
        this.rstCount = new Long(0);
        this.finCount = new Long(0);
        this.pushCount = new Long(0);
        this.ackCount = new Long(0);
        this.maxWindowSize = 0;
        this.minWindowSize = Integer.MAX_VALUE;
        this.accWindowSize = new Long(0);
        this.serverIp = "0.0.0.0";
        this.clientIp = "0.0.0.0";
        this.clientPort = 0;
        this.serverPort = 0;
        this.accClientSize = new Long(0);
        this.accServerSize = new Long(0);
        this.protocols = new HashMap<>();
    }

    private long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public void add(Packet packet){
        this.totalCount++;
        this.proto = packet.proto;
        this.accSize += packet.size;
        this.accWindowSize += packet.windowSize;

        if(packet.protocol != null){
           //System.out.println("Debug");
           Integer count = this.protocols.containsKey(packet.protocol) ? this.protocols.get(packet.protocol) : 0;
           count ++;
           this.protocols.put(packet.protocol, count);
        }

        if(packet.time.before(this.firstTime)){
            this.firstTime = packet.time;
        }
        if (packet.time.after(this.lastTime)) {
            this.lastTime = packet.time;
        }

        // client -> server
        if (packet.srcPort > packet.dstPort) {
            clientPort = packet.srcPort;
            serverPort= packet.dstPort;
            clientCount++;
            this.accClientSize += packet.size;
            if(packet.size > this.clientSentMax){
                this.clientSentMax = packet.size;
            }
            if(packet.size < this.clientSentMin){
                this.clientSentMin = packet.size;
            }
            this.clientIp = packet.ipSrc;
            this.serverIp = packet.ipDst;
        }
        // server -> client
        else {
            clientPort = packet.dstPort;
            serverPort = packet.srcPort;
            serverCount++;
            this.accServerSize += packet.size;
            if(packet.size > this.serverSentMax){
                this.serverSentMax = packet.size;
            }
            if(packet.size < this.serverSentMin){
                this.serverSentMin = packet.size;
            }
            this.clientIp = packet.ipDst;
            this.serverIp = packet.ipSrc;
        }
        if (packet.isSyn){
            synCount ++;
        }
        if (packet.isFin){
            finCount ++;
        }
        if (packet.isRst){
            rstCount ++;
        }
        if (packet.isAck){
            ackCount ++;
        }
        if (packet.isPush){
            pushCount ++;
        }
    }

    public double getAvSize(){
        return this.totalCount.equals(new Long(0)) ? 0 : this.accSize / this.totalCount;
    }
    public double getAvClientSize(){
        return this.clientCount.equals(new Long(0)) ? 0 : this.accClientSize / this.clientCount;
    }
    public double getAvServerSize(){
        return this.serverCount.equals(new Long(0)) ? 0 : this.accServerSize / this.serverCount;
    }
    public double getDuration(){
        return getDateDiff(this.firstTime,this.lastTime,TimeUnit.SECONDS);
    }
    public double getCountRatio(){
        return this.clientCount.equals(new Long(0)) ? 0 : this.serverCount / this.clientCount;
    }
    public double getSizeRatio(){
        return this.accClientSize.equals(new Long(0)) ? 0 : this.accServerSize / this.accClientSize;
    }

    public String getProtocol(){
        if (this.protocols.isEmpty()){
            return "UNIDENTED";
        }else {
            String result = Collections.max(this.protocols.entrySet(), Map.Entry.comparingByValue()).getKey();
            if (result == "SSL/TLS") {
                if (serverPort == 443 || serverPort == 80){
                    return "HTTPS";
                }
                else if (serverPort == 9001){
                    return "TOR";
                }
                else return result;
            }else
                return result;
        }
            //return Collections.max(this.protocols.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
    }

    public double getAvWindowSize(){
        return this.totalCount.equals(new Long(0)) ? 0 : accWindowSize / totalCount;
    }
    public Integer getWindowVar(){
        return maxWindowSize - minWindowSize;
    }
    public Integer getServerSentVar(){
        return this.serverSentMax - this.serverSentMin ;
    }
    public Integer getClientSentVar(){
        return this.clientSentMax - this.clientSentMin ;
    }
    public String toString(){
        return String.format("Protocol %s, Duration %s s, SYN %s, FIN %s, RST %s\n" +
                        "Server %s:%s sent %s packets, %s bytes.\n" +
                        "Client %s:%s sent %s packets, %s bytes.",
                getProtocol(),
                getDuration(),
                this.synCount,
                this.finCount,
                this.rstCount,
                this.serverIp,
                this.serverPort,
                this.serverCount,
                this.accServerSize,
                this.clientIp,
                this.clientPort,
                this.clientCount,
                this.accClientSize
//                this.totalCount,
//                this.accSize,
//                this.clientCount,
//                this.serverCount,
//                this.serverSentMax,
//                this.clientSentMax,
//                this.serverSentMin,
//                this.clientSentMin,
//                this.firstTime,
//                this.lastTime,
//                this.proto,
//                this.synCount,
//                this.rstCount,
//                this.finCount,
//                this.maxWindowSize,
//                this.minWindowSize,
//                this.accWindowSize ,
//                this.serverIp,
//                this.clientIp,
//                this.clientPort,
//                this.serverPort,
//                this.accClientSize,
//                this.accServerSize,
//                getClientSentVar(),
//                getServerSentVar(),
//                getWindowVar(),
//                getAvWindowSize(),
//                getSizeRatio(),
//                getCountRatio(),
//                getDuration(),
//                getAvServerSize(),
//                getAvClientSize(),
//                getAvSize()

        );
    }

    @Override
    public int compareTo(FlowStat o) {
        return Long.compare(this.totalCount, o.totalCount);
    }
}

