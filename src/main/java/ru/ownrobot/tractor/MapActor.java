package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.ByteIterator;
import akka.util.ByteString;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MapActor extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private String formIpaddress(byte[] packet) {
        return String.format("%s.%s.%s.%s", packet[0] & 0xFF,
                packet[1] & 0xFF,
                packet[2] & 0xFF,
                packet[3] & 0xFF);
    }

    public WorkerMsgs.PacketStatus parsePacket(ByteString file) {
        int pcapHeaderLen = 16;
        int ethHeaderLen = 14;
        int ipHeaderLen = 20;
        if (file.size() > 16) { // Can read pcapHeader
            ByteIterator it = file.iterator();
            int ts_sec = it.getInt(ByteOrder.LITTLE_ENDIAN);//4
            int usec = it.getInt(ByteOrder.LITTLE_ENDIAN);//8
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

                if (proto == 6) { //Tcp packet
                    it.getBytes(2);//checksum;  //42
                    String ip_src = formIpaddress(it.getBytes(4)); //46
                    String ip_dst = formIpaddress(it.getBytes(4)); //50

                    Integer src_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff; //52
                    Integer dst_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff;  //54
                    Long seq = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //58
                    Long ack = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //62
                    Integer tcpHeaderLen = (it.getByte() & 0xFF) / 4;   //63

                    int tcpDataStart = pcapHeaderLen + ethHeaderLen + ipHeaderLen + tcpHeaderLen;
                    int tcpDataStop = incl_len + pcapHeaderLen;
                    ByteString tcpData = file.slice(tcpDataStart, tcpDataStop);
                    getSender().tell(new WorkerMsgs.TcpData(String.format("$s:$s->$s:%s", ip_src, src_port, ip_dst, dst_port), seq, tcpData), self());
                                        // System.out.println(tcpdata.size());
                }
                return new WorkerMsgs.PacketStatus(true, file.splitAt(incl_len + pcapHeaderLen)._2());
            }
        } else return new WorkerMsgs.PacketStatus(false, file);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof DatabaseMsgs.FileJobResponce){

            DatabaseMsgs.FileJobResponce job = (DatabaseMsgs.FileJobResponce)message;


            Duration duration = Duration.apply("10 sec");
            Future<Object> lostBytes = Patterns.ask(getContext().system().actorFor(job.nextAddress + "/user/bytes"), new WorkerMsgs.ByteRequest(job.nextChunkname, job.nextOffset), 10000);

            Long chunkname = job.chunkname;
            Integer offset = job.offset;
            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();

            Path path = Paths.get("/tmp/" + (cluster.selfAddress().hashCode() & 0xffffffffl));

            SeekableByteChannel inChannel = Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname));
            ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
            inChannel.read(buffer);
            buffer.flip();
            ByteString file = ByteString.fromByteBuffer(buffer);
            inChannel.close();

            boolean keepGoing = true;
            ByteString ending = file.splitAt(offset)._2();
            while (keepGoing){
                WorkerMsgs.PacketStatus result = parsePacket(ending);
                ending =  result.data;
                keepGoing = result.status;
            }

            ByteString additionalBytes = (ByteString) Await.result(lostBytes, duration);
            ByteString lastRecord = ending.concat(additionalBytes);
            if (lastRecord.size() > 0){
                if (!parsePacket(lastRecord).status)
                    log.debug("corrupted or ZeroSize last packet");
            }
            System.out.println("All packets sent!");
        }
        else{
            unhandled(message);
        }
    }
    public void preStart(){
        log.error("Map actor started");
    }
    public void postStop(){
        log.error("Map actor stopped");
    }
}
