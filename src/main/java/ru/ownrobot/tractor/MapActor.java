package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.routing.Router;
import akka.util.ByteIterator;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import ru.ownrobot.tractor.KryoMessages.*;
import ru.ownrobot.tractor.ProtoMessages.*;

import static java.lang.Math.toIntExact;

class RecordStatus {
    private boolean isFinished;
    private ByteString nextRecord;

    public RecordStatus(boolean isFinished, ByteString nextRecord) {
        this.isFinished = isFinished;
        this.nextRecord = nextRecord;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setIsFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

    public ByteString getNextRecord() {
        return nextRecord;
    }

    public void setNextRecord(ByteString nextRecord) {
        this.nextRecord = nextRecord;
    }
}
public class MapActor extends UntypedActor {
    private final Config config = ConfigFactory.load();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Random random = new Random();
    private final List<ActorSelection> jobTrackers = new ArrayList<>();

    private final List<ActorSelection> nodes = createRouter();

    private ActorSelection selectJobTracker(String jobId) {
        return jobTrackers.get(Math.abs(jobId.hashCode() % jobTrackers.size()));
    }

    private int random() {
        return Math.abs(random.nextInt()) % config.getInt("workers.count");
    }

    private List<ActorSelection> createRouter(){
        final List<ActorSelection> routees = new ArrayList<>();
        ActorSystem system = getContext().system();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cluster cluster = Cluster.get(getContext().system());
        cluster.state().getMembers().forEach(m -> {
            for (int i =0; i< config.getInt("workers.count"); i++) {
                //if (m.hasRole("worker")) {
                    routees.add(system.actorSelection(m.address() + "/user/aggregator" + i));
                    jobTrackers.add(system.actorSelection(m.address() + "/user/jobTracker"));
                //}
            }
        });

        return routees;
    }
    private String formIpaddress(byte[] packet) {
        return String.format("%s.%s.%s.%s", packet[0] & 0xFF,
                packet[1] & 0xFF,
                packet[2] & 0xFF,
                packet[3] & 0xFF);
    }
    private Long hash(byte[] ipSrc, byte[] ipDst, Integer portSrc, Integer portDst) {
        byte[] a1 = ByteBuffer.allocate(4).putInt(portSrc).array();
        byte[] b1 = ByteBuffer.allocate(4).putInt(portDst).array();
        byte[] a2 = ByteBuffer.allocate(8).put(ipSrc).put(a1).array();
        byte[] b2 = ByteBuffer.allocate(8).put(ipDst).put(b1).array();
        Long a = ByteBuffer.wrap(a2).getLong();
        Long b = ByteBuffer.wrap(b2).getLong();

        Long d = Math.abs(a-b);
        Long min = a + (d & d>>63); // fast branchless min
        Long max = b - (d & d>>63); // fast branchless max

        return max<<64 | min;
    }

    private RecordStatus parsePacket(ByteString file, String jobId) {
        int pcapHeaderLen = 16;
        int ethHeaderLen = 14;
        int ipHeaderLen = 20;
        if (file.size() > 16) { // Can read pcapHeader
            ByteIterator it = file.iterator();
            int ts_sec = it.getInt(ByteOrder.LITTLE_ENDIAN);//4
            int ts_usec = it.getInt(ByteOrder.LITTLE_ENDIAN);//8
            int incl_len = it.getInt(ByteOrder.nativeOrder());//12
            int orig_len = it.getInt(ByteOrder.nativeOrder());//16

            if (incl_len + pcapHeaderLen > file.size()){ //Packet corrupted
                return new RecordStatus(false, file);
            } else {
                it.getBytes(6);//Srt // 22
                it.getBytes(6);//DSt // 28
                int ether_type = it.getShort(ByteOrder.LITTLE_ENDIAN); //30
                it.getBytes(8); //X3  //38
                it.getBytes(1); //ttl  //39
                int proto = it.getBytes(1)[0] & 0xFF; //proto //40
                it.getBytes(2);//checksum;  //42
                byte[] ipSrc = it.getBytes(4);
                byte[] ipDst = it.getBytes(4);

                if(proto == 6) {
                    Integer portSrc = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff; //52
                    Integer portDst = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff;  //54

                    Long flowHash = hash(ipSrc, ipDst, portSrc, portDst);

                    Long seq = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //58
                    Long ack = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //62
                    Integer tcpHeaderLen = (it.getByte() & 0xFF) / 4;   //63
                    //it.getByte();
                    byte flags = it.getByte();
                    boolean conIsSet = ((flags >> 7) & 1) != 0;
                    boolean eIsSet = ((flags >> 6) & 1) != 0;
                    boolean urIsSet = ((flags >> 5) & 1) != 0;
                    boolean ackIsSet = ((flags >> 4) & 1) != 0;
                    boolean pusIsSset = ((flags >> 3) & 1) != 0;
                    boolean rstIsSet = ((flags >> 2) & 1) != 0;
                    boolean synIsSet = ((flags >> 1) & 1) != 0;
                    boolean finIsSet = ((flags) & 1) != 0;
                    int window = it.getShort(ByteOrder.LITTLE_ENDIAN);
                    int tcpDataStart = pcapHeaderLen + ethHeaderLen + ipHeaderLen + tcpHeaderLen;
                    ByteString payload = tcpDataStart + 4 < file.size() ? file.slice(tcpDataStart, tcpDataStart+4) : ByteString.empty();
                    String protocol = detectProtocol(payload);

                    TractorPacket packet = new TractorPacket(synIsSet,finIsSet,rstIsSet,ackIsSet,pusIsSset,
                            "TCP", formIpaddress(ipSrc), formIpaddress(ipDst), portSrc, portDst, incl_len,
                            new Date(ts_sec * 1000L + ts_usec / 1000), protocol, window);
                    //"TCP", formIpaddress(ipSrc), src_port, formIpaddress(ipDst), dst_port, incl_len, new Date(ts_sec * 1000L + ts_usec / 1000),ackset, pushset, rstset, synset, finset, window, protocol);
                    Long id = Math.abs(flowHash) % nodes.size();
                    nodes.get(toIntExact(id)).tell(new TractorPacketMsg(jobId, packet, flowHash), self());

                }
                return new RecordStatus(true, file.splitAt(incl_len + pcapHeaderLen)._2());
            }
        } else return new RecordStatus(false, file);
    }

    private String detectProtocol(ByteString payload) {
        if (!payload.isEmpty()){
            String payString =  payload.utf8String();
            byte [] payBytes = payload.iterator().getBytes(4);
            if (payString.contains("GET") ||
                    payString.contains("POST") ||
                    payString.contains("HEAD") ||
                    payString.contains("PUT") ||
                    payString.contains("HTTP") ||
                    payString.contains("UNKN") ||
                    payString.contains("DELE")) {
                return "HTTP";
            }
            else if (payString.contains("LOCK") ||
                    payString.contains("UNLO") ||
                    payString.contains("OPTI") ||
                    payString.contains("PROP") ||
                    payString.contains("POLL") ||
                    payString.contains("SEAR") ||
                    payString.contains("MKCO")) {
                return "Webdav";
            }
            else if (payString.contains("SSH-") || payString.contains("QUIT")) {
                return "SSH";
            }
            else if (payBytes[0] == (byte) 0xff && payBytes[3] == (byte) 0xff  ){
                //Not Working!
                System.out.println("Telnet");
                //else if (payload.contains(ByteString.fromArray(new byte[]{(byte)0x80}))){
                return "Telnet";
            }
            else if (payString.contains("Bit")) {
                return "BitTorrent";
            }
            else if (payBytes[0] == (byte) 0x80 || payBytes[1] == (byte) 0x03  ){
                //Not Working!
                //System.out.println("DeeeeBUG");
            //else if (payload.contains(ByteString.fromArray(new byte[]{(byte)0x80}))){
                return "SSL/TLS";
            }
            else
               return null;
        }
        else
            return null;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ChunkProcessRequest){

            ChunkProcessRequest job = (ChunkProcessRequest) message;

            Future<Object> lostBytes = null;

            Duration duration = Duration.apply("60 sec");
            if ( !job.getNextNodeAddress().isEmpty()) {
                lostBytes = Patterns.ask(getContext().system().actorSelection(job.getNextNodeAddress() + "/user/bytes" + random()), ExtraBytesRequest.newBuilder().setChunkId(job.getNextChunkName()).setCount(job.getNextOffset()).build(), 60000);
            }

            Long chunkname = job.getChunkName();
            Integer offset = job.getOffset();
            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);

            Path path = Paths.get(config.getString("filesystem.path"));

            SeekableByteChannel inChannel = Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname));
            ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
            inChannel.read(buffer);
            buffer.flip();
            ByteString file = ByteString.fromByteBuffer(buffer);
            inChannel.close();

            boolean keepGoing = true;
            ByteString ending = file.splitAt(offset)._2();
            while (keepGoing){
                RecordStatus result = parsePacket(ending, job.getJobId());
                ending =  result.getNextRecord();
                keepGoing = result.isFinished();
            }

            ByteString additionalBytes = ByteString.empty();

            if (lostBytes != null) {
                additionalBytes = (ByteString) Await.result(lostBytes, duration);
            }

            ByteString lastRecord = ending.concat(additionalBytes);
            if (lastRecord.size() > 0){
                if (!parsePacket(lastRecord,job.getJobId()).isFinished())
                    log.debug("corrupted or ZeroSize last packet");
            }
            selectJobTracker(job.getJobId()).tell(JobStatusMsg.newBuilder().setJobId(job.getJobId()).setFinished(true).build(), self());
            //nodes.forEach(i -> i.tell(JobStatusMsg.newBuilder().setJobId(job.getJobId()).setFinished(true).build(), self()));

        }else if (message instanceof JobFinishedMsg){
            String jobId = ((JobFinishedMsg) message).getJobId();
            nodes.forEach(i->i.tell(message,self()));
            //selectJobTracker(jobId).tell(message, self());
        }
        else{
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Map actor started");
    }
    public void postStop(){
        log.info("Map actor stopped");
    }
}
