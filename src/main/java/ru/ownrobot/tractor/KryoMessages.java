package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.util.ByteString;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Messages that use kryo serialization
 * Created by laboshinl on 8/30/16.
 */

public class KryoMessages {

    public static class JobResult implements Serializable{
        public JobResult(String jobId, HashMap<Long, FlowStat> flows) {
            this.jobId = jobId;
            this.flows = flows;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public HashMap<Long, FlowStat> getFlows() {
            return flows;
        }

        public void setFlows(HashMap<Long, FlowStat> flows) {
            this.flows = flows;
        }

        private String jobId;
        private HashMap<Long,FlowStat> flows;

    }

    public static class FileChunk implements Serializable {
        private ByteString chunkData;
        private String fileName;
        private String jobId;

        public FileChunk(String jobId, String fileName, ByteString chunkData) {
            this.chunkData = chunkData;
            this.fileName = fileName;
            this.jobId = jobId;
        }

        public void setChunkData(ByteString chunkData) {
            this.chunkData = chunkData;
        }

        public ByteString getChunkData() {
            return chunkData;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getJobId() {
            return jobId;
        }

    }

    public static class DBRecord implements Serializable {
        private Long chunkId;
        private Integer offset;
        private Date timestamp;
        private String fileName;
        private String address;

        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public DBRecord(Long chunkId, Integer offset, Date timestamp, String fileName, String address) {
            this.chunkId = chunkId;
            this.offset = offset;
            this.timestamp = timestamp;
            this.fileName = fileName;
            this.address = address;
        }

    }

    public static class JobProgress implements Serializable {
        private Integer count;
        private Integer finished;
        private final List<ActorRef> workers;

        JobProgress(Integer count) {
            this.count = count;
            this.finished = 0;
            this.workers = new ArrayList<>();
        }

        public Integer getProgress() {
            return this.finished * 100 / this.count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public void setFinished(Integer finished) {
            this.finished = finished;
        }

        public void addWorker(ActorRef worker) {
            if (this.workers.contains(worker)) {
                System.out.println("already exist");
            }else {
                this.workers.add(worker);
                System.out.println(this.workers.toString());
                //System.out.println(workers.toArray());
            }
        }

        public List<ActorRef> getWorkers() {
            return this.workers;
        }

        public Integer getFinished() {
            return this.finished;
        }

        public Integer getCount() {
            return this.count;
        }

        public void increment() {
            this.finished++;
        }

        public boolean isFinished() {
            return this.count.equals(this.finished);
        }
    }

    public static class TractorPacketMsg implements Serializable {
        private TractorPacket packet;
        private Long flowId;
        private String jobId;

        public TractorPacketMsg(String jobId, TractorPacket packet, Long flowId) {
            this.packet = packet;
            this.flowId = flowId;
            this.jobId = jobId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public TractorPacket getPacket() {
            return packet;
        }

        public void setPacket(TractorPacket packet) {
            this.packet = packet;
        }

        public Long getFlowId() {
            return flowId;
        }

        public void setFlowId(Long flowId) {
            this.flowId = flowId;
        }
    }

    public static class TractorPacket implements Serializable {
        private Boolean isSyn;
        private Boolean isFin;
        private Boolean isRst;
        private Boolean isAck;
        private Boolean isPush;
        private String proto;
        private String ipSrc;
        private String ipDst;
        private Integer srcPort;
        private Integer dstPort;
        private Integer size;
        private Date time;
        private String protocol;
        private Integer windowSize;

        public TractorPacket(Boolean isSyn, Boolean isFin, Boolean isRst, Boolean isAck, Boolean isPush, String proto, String ipSrc, String ipDst, Integer srcPort, Integer dstPort, Integer size, Date time, String protocol, Integer windowSize) {
            this.isSyn = isSyn;
            this.isFin = isFin;
            this.isRst = isRst;
            this.isAck = isAck;
            this.isPush = isPush;
            this.proto = proto;
            this.ipSrc = ipSrc;
            this.ipDst = ipDst;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.size = size;
            this.time = time;
            this.protocol = protocol;
            this.windowSize = windowSize;
        }

        public Boolean getIsSyn() {
            return isSyn;
        }

        public void setIsSyn(Boolean isSyn) {
            this.isSyn = isSyn;
        }

        public Boolean getIsFin() {
            return isFin;
        }

        public void setIsFin(Boolean isFin) {
            this.isFin = isFin;
        }

        public Boolean getIsRst() {
            return isRst;
        }

        public void setIsRst(Boolean isRst) {
            this.isRst = isRst;
        }

        public Boolean getIsAck() {
            return isAck;
        }

        public void setIsAck(Boolean isAck) {
            this.isAck = isAck;
        }

        public Boolean getIsPush() {
            return isPush;
        }

        public void setIsPush(Boolean isPush) {
            this.isPush = isPush;
        }

        public String getProto() {
            return proto;
        }

        public void setProto(String proto) {
            this.proto = proto;
        }

        public String getIpSrc() {
            return ipSrc;
        }

        public void setIpSrc(String ipSrc) {
            this.ipSrc = ipSrc;
        }

        public String getIpDst() {
            return ipDst;
        }

        public void setIpDst(String ipDst) {
            this.ipDst = ipDst;
        }

        public Integer getSrcPort() {
            return srcPort;
        }

        public void setSrcPort(Integer srcPort) {
            this.srcPort = srcPort;
        }

        public Integer getDstPort() {
            return dstPort;
        }

        public void setDstPort(Integer dstPort) {
            this.dstPort = dstPort;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Date getTime() {
            return time;
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Integer getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(Integer windowSize) {
            this.windowSize = windowSize;
        }
    }

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

    public static class FlowStat implements Serializable, Comparable<FlowStat> {
        private String serverIp;
        private String clientIp;
        private Long totalCount;
        private Long accSize;

        private Long clientCount;
        private Long serverCount;

        private Integer serverSentMax;
        private Integer clientSentMax;
        private Integer serverSentMin;
        private Integer clientSentMin;

        private HashMap<String, Integer> protocols;

        private Date firstTime;
        private Date lastTime;

        private String proto;

        private Long synCount;
        private Long rstCount;
        private Long finCount;
        private Long ackCount;
        private Long pushCount;

        private Integer minWindowSize;
        private Integer maxWindowSize;

        private Long accWindowSize;

        private Integer clientPort;
        private Integer serverPort;
        private Long accClientSize;
        private Long accServerSize;

        public FlowStat() {
            this.totalCount = new Long(0);
            this.accSize = new Long(0);
            this.clientCount = new Long(0);
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

        public void add(TractorPacket packet) {
            this.totalCount++;
            this.proto = packet.proto;
            this.accSize += packet.size;
            this.accWindowSize += packet.windowSize;

            if (packet.protocol != null) {
                //System.out.println("Debug");
                Integer count = this.protocols.containsKey(packet.protocol) ? this.protocols.get(packet.protocol) : 0;
                count++;
                this.protocols.put(packet.protocol, count);
            }

            if (packet.time.before(this.firstTime)) {
                this.firstTime = packet.time;
            }
            if (packet.time.after(this.lastTime)) {
                this.lastTime = packet.time;
            }

            // client -> server
            if (packet.srcPort > packet.dstPort) {
                clientPort = packet.srcPort;
                serverPort = packet.dstPort;
                clientCount++;
                this.accClientSize += packet.size;
                if (packet.size > this.clientSentMax) {
                    this.clientSentMax = packet.size;
                }
                if (packet.size < this.clientSentMin) {
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
                if (packet.size > this.serverSentMax) {
                    this.serverSentMax = packet.size;
                }
                if (packet.size < this.serverSentMin) {
                    this.serverSentMin = packet.size;
                }
                this.clientIp = packet.ipDst;
                this.serverIp = packet.ipSrc;
            }
            if (packet.isSyn) {
                synCount++;
            }
            if (packet.isFin) {
                finCount++;
            }
            if (packet.isRst) {
                rstCount++;
            }
            if (packet.isAck) {
                ackCount++;
            }
            if (packet.isPush) {
                pushCount++;
            }
        }

        public double getAvSize() {
            return this.totalCount.equals(new Long(0)) ? 0 : this.accSize / this.totalCount;
        }

        public double getAvClientSize() {
            return this.clientCount.equals(new Long(0)) ? 0 : this.accClientSize / this.clientCount;
        }

        public double getAvServerSize() {
            return this.serverCount.equals(new Long(0)) ? 0 : this.accServerSize / this.serverCount;
        }

        public double getDuration() {
            return getDateDiff(this.firstTime, this.lastTime, TimeUnit.SECONDS);
        }

        public double getCountRatio() {
            return this.clientCount.equals(new Long(0)) ? 0 : this.serverCount / this.clientCount;
        }

        public double getSizeRatio() {
            return this.accClientSize.equals(new Long(0)) ? 0 : this.accServerSize / this.accClientSize;
        }

        public String getProtocol() {
            if (this.protocols.isEmpty()) {
                return "UNIDENTED";
            } else {
                String result = Collections.max(this.protocols.entrySet(), Map.Entry.comparingByValue()).getKey();
                if (result == "SSL/TLS") {
                    if (serverPort == 443 || serverPort == 80) {
                        return "HTTPS";
                    } else if (serverPort == 9001) {
                        return "TOR";
                    } else return result;
                } else
                    return result;
            }
            //return Collections.max(this.protocols.entrySet(), (entry1, entry2) -> entry1.getValue() - entry2.getValue()).getKey();
        }

        public double getAvWindowSize() {
            return this.totalCount.equals(new Long(0)) ? 0 : accWindowSize / totalCount;
        }

        public Integer getWindowVar() {
            return maxWindowSize - minWindowSize;
        }

        public Integer getServerSentVar() {
            return this.serverSentMax - this.serverSentMin;
        }

        public Integer getClientSentVar() {
            return this.clientSentMax - this.clientSentMin;
        }

        public String toString() {
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


}
