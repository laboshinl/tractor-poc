package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.routing.*;
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

import static java.lang.Math.toIntExact;

public class MapActor extends UntypedActor {
    Config config = ConfigFactory.load();
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    List<ActorRef> nodes = createRouter();
    Random random = new Random();

    public int random() {
        return Math.abs(random.nextInt()) % config.getInt("workers.count");
    }
    public List<ActorRef> createRouter(){
        final List<ActorRef> routees = new ArrayList<>();
        ActorSystem system = getContext().system();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Cluster cluster = Cluster.get(getContext().system());
        cluster.state().getMembers().forEach(m -> {
            for (int i =0; i< config.getInt("workers.count"); i++) {
                if (m.hasRole("worker"))
                    routees.add(system.actorFor(m.address() + "/user/aggregator"+i));
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

    public WorkerMsgs.PacketStatus parsePacket(ByteString file, String jobId) {
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
                return new WorkerMsgs.PacketStatus(false, file);
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
                //String ip_src = formIpaddress(ipSrc); //46
                //String ip_dst = formIpaddress(ipDst); //50

                if(proto == 6) {
//                switch (proto){
//                    case 6:{ //TCP
                    Integer src_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff; //52
                    Integer dst_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff;  //54

                    Long flowHash = hash(ipSrc, ipDst, src_port, dst_port);

                    Long seq = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //58
                    Long ack = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //62
                    Integer tcpHeaderLen = (it.getByte() & 0xFF) / 4;   //63
                    //it.getByte();
                    byte flags = it.getByte();
                    boolean conset = ((flags >> 7) & 1) != 0;
                    boolean eset = ((flags >> 6) & 1) != 0;
                    boolean urser = ((flags >> 5) & 1) != 0;
                    boolean ackset = ((flags >> 4) & 1) != 0;
                    boolean pushset = ((flags >> 3) & 1) != 0;
                    boolean rstset = ((flags >> 2) & 1) != 0;
                    boolean synset = ((flags >> 1) & 1) != 0;
                    boolean finset = ((flags >> 0) & 1) != 0;
                    int window = it.getShort(ByteOrder.LITTLE_ENDIAN);
                    Packet packet = new Packet("TCP", formIpaddress(ipSrc), src_port, formIpaddress(ipDst), dst_port, incl_len, new Date(ts_sec * 1000L + ts_usec / 1000),ackset, pushset, rstset, synset, finset, window);
                    Long id = Math.abs(flowHash) % nodes.size();
                    nodes.get(toIntExact(id)).tell(new WorkerMsgs.PacketMsg(flowHash, packet, jobId), self());

                }
//                        break;
//                    }
//                    case 17:{ //UDP
//                        Integer src_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff; //52
//                        Integer dst_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff;  //54
//                        new Packet(false,false,false,"UDP", src_port, dst_port, incl_len, new Date(ts_sec * 1000L + ts_usec / 1000),0);
//                        break;
//                    }
//                    default:
//                        break;
//
//                }
//                if (proto == 6) { //Tcp packet
//                    Long seq = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //58
//                    Long ack = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //62
//                    Integer tcpHeaderLen = (it.getByte() & 0xFF) / 4;   //63
//
//                    int tcpDataStart = pcapHeaderLen + ethHeaderLen + ipHeaderLen + tcpHeaderLen;
//                    int tcpDataStop = incl_len + pcapHeaderLen;
//                    ByteString tcpData = tcpDataStop < file.size() ? file.slice(tcpDataStart, tcpDataStop) : file.takeRight(tcpDataStart);
//                    String data = tcpData.utf8String();
//                    if(data.contains("GET")){
//                       String[] lines =  data.split("\\r?\\n");
//                        if (lines.length > 1) {
//                            String[] url = lines[0].split("\\s+");
//                            String[] host = lines[1].split("\\s+");
//                            if (url.length > 1 && host.length > 1){
//                                Long id = (String.format("%s:%s->%s:%s", ip_src, src_port, ip_dst, dst_port).hashCode() & 0xffffffffl) % nodes.size();
//                                nodes.get(toIntExact(id)).tell(new WorkerMsgs.TcpData(String.format("%s:%s->%s:%s", ip_src, src_port, ip_dst, dst_port), seq, ByteString.fromString(host[1])/*tcpData*/, jobId), self());
//                            }
//                        }
//                    }
//
//                }
                return new WorkerMsgs.PacketStatus(true, file.splitAt(incl_len + pcapHeaderLen)._2());
            }
        } else return new WorkerMsgs.PacketStatus(false, file);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof DatabaseMsgs.FileJobResponce){

            DatabaseMsgs.FileJobResponce job = (DatabaseMsgs.FileJobResponce)message;

            Future<Object> lostBytes = null;

            Duration duration = Duration.apply("10 sec");
            if (job.nextAddress != null) {
                lostBytes = Patterns.ask(getContext().system().actorFor(job.nextAddress + "/user/bytes"+random()), new WorkerMsgs.ByteRequest(job.nextChunkname, job.nextOffset), 10000);
            }

            Long chunkname = job.chunkname;
            Integer offset = job.offset;
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
                WorkerMsgs.PacketStatus result = parsePacket(ending, job.jobId);
                ending =  result.data;
                keepGoing = result.status;
            }

            ByteString additionalBytes = ByteString.empty();

            if (lostBytes != null) {
                additionalBytes = (ByteString) Await.result(lostBytes, duration);
            }

            ByteString lastRecord = ending.concat(additionalBytes);
            if (lastRecord.size() > 0){
                if (!parsePacket(lastRecord,job.jobId).status)
                    log.debug("corrupted or ZeroSize last packet");
            }
            nodes.forEach(i -> i.tell(new WorkerMsgs.JobStatus(job.jobId, job.chunkCount), self()));

        }
        else{
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
