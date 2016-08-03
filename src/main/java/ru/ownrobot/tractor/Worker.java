package ru.ownrobot.tractor;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.pattern.Patterns;
import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bitbucket.dollar.Dollar;
import scala.Predef;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.io.Source$;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class Worker extends UntypedActor {
    Config config = ConfigFactory.load();
    //Path path = Paths.get(config.getString("tractor.storage.path"));

    final List<Integer> validEthertypes =
            Dollar.$(Integer.parseInt("800", 16), Integer.parseInt("808", 16))
                    .concat(Dollar.$(Integer.parseInt("0", 16), Integer.parseInt("5dc", 16)))
                    .concat(Dollar.$(Integer.parseInt("884", 16), Integer.parseInt("89a", 16)))
                    .concat(Dollar.$(Integer.parseInt("884", 16), Integer.parseInt("89a", 16)))
                    .concat(Dollar.$(Integer.parseInt("b00", 16), Integer.parseInt("b07", 16)))
                    .concat(Dollar.$(Integer.parseInt("bad", 16), Integer.parseInt("baf", 16)))
                    .concat(Dollar.$(Integer.parseInt("1000", 16), Integer.parseInt("10ff", 16)))
                    .concat(Dollar.$(Integer.parseInt("2000", 16), Integer.parseInt("207f", 16)))
                    .concat(Dollar.$(Integer.parseInt("22e0", 16), Integer.parseInt("22f2", 16)))
                    .concat(Dollar.$(Integer.parseInt("86dd", 16), Integer.parseInt("8fff", 16)))
                    .concat(Dollar.$(Integer.parseInt("9000", 16), Integer.parseInt("9003", 16)))
                    .concat(Dollar.$(Integer.parseInt("9040", 16), Integer.parseInt("905f", 16)))
                    .concat(Dollar.$(Integer.parseInt("c020", 16), Integer.parseInt("c02f", 16)))
                    .concat(Dollar.$(Integer.parseInt("c220", 16), Integer.parseInt("c22f", 16)))
                    .concat(Dollar.$(Integer.parseInt("fea0", 16), Integer.parseInt("feaf", 16)))
                    .concat(Dollar.$(Integer.parseInt("ff00", 16), Integer.parseInt("ff0f", 16)))
                    .concat(Integer.parseInt("81c", 16)).concat(Integer.parseInt("844", 16))
                    .concat(Integer.parseInt("900", 16)).concat(Integer.parseInt("a00", 16))
                    .concat(Integer.parseInt("a01", 16)).concat(Integer.parseInt("22df", 16))
                    .concat(Integer.parseInt("9999", 16)).concat(Integer.parseInt("9c40", 16))
                    .concat(Integer.parseInt("a580", 16)).concat(Integer.parseInt("fc0f", 16))
                    .concat(Integer.parseInt("ffff", 16)).sort().toList();

    private String formIpaddress(byte[] packet) {
        return String.format("%s.%s.%s.%s", packet[0] & 0xFF,
                packet[1] & 0xFF,
                packet[2] & 0xFF,
                packet[3] & 0xFF);
    }

    public void parsePacket(ByteString file) {
        if (file.size()>70) {
            ByteIterator it = file.iterator();
            int ts_sec = it.getInt(ByteOrder.LITTLE_ENDIAN);//4
            int usec = it.getInt(ByteOrder.LITTLE_ENDIAN);//8
            int incl_len = it.getInt(ByteOrder.nativeOrder());//12
            int orig_len = it.getInt(ByteOrder.nativeOrder());//16
            it.getBytes(6);//Srt // 22
            it.getBytes(6);//DSt // 28
            int ether_type = it.getShort(ByteOrder.LITTLE_ENDIAN); //30
            it.getBytes(8); //X3  //38
            it.getBytes(1); //ttl  //39
            int proto = it.getBytes(1)[0] & 0xFF; //proto //40
            //System.out.println(proto);
            if (proto == 6) { //Tcp packet
                it.getBytes(2);//checksum;  //42
                String ip_src = formIpaddress(it.getBytes(4)); //46
                String ip_dst = formIpaddress(it.getBytes(4)); //50
                //
                Integer src_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff; //52
                Integer dst_port = it.getShort(ByteOrder.BIG_ENDIAN) & 0xffff;;  //54
                Long seq = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //58
                Long ack = it.getInt(ByteOrder.BIG_ENDIAN) & 0xffffffffl;  //62
                Integer tcpHeaderLen = (it.getByte() & 0xFF)/ 4;   //63
                System.out.println(tcpHeaderLen);

//                ByteString tcpdata = file.splitAt(50+tcpHeaderLen)._2();
                //System.out.println(tcpdata);
            }
            parsePacket(file.splitAt(incl_len+16)._2());
        }
    }
//    }
    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof WorkerMsgs.ByteRequest){
            Integer chunkname = ((WorkerMsgs.ByteRequest) message).chunkname;
            Integer size = ((WorkerMsgs.ByteRequest) message).size;
            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();
            Path path = Paths.get("/tmp/" + cluster.selfAddress().hashCode());

            ByteBuffer buf = ByteBuffer.allocate(size);
            Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname)).read(buf);
            sender().tell(ByteString.fromByteBuffer(buf), self());
        }else if (message instanceof DatabaseMsgs.FileJobResponce){
            DatabaseMsgs.FileJobResponce job = (DatabaseMsgs.FileJobResponce)message;
            Integer chunkname = job.chunkname;
            Integer offset = job.offset;
            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();

            ///
//            Duration duration = Duration.apply("100 sec");
//            Future<Object> test =  Patterns.ask(getContext().system().actorFor(job.nextAddress + "/user/worker"), new WorkerMsgs.ByteRequest(job.nextChunkname,job.nextOffset), 100000);
//            System.out.println(Await.result(test,duration));
            ///
            Path path = Paths.get("/tmp/" + cluster.selfAddress().hashCode());
            SeekableByteChannel inChannel = Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname));
            ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
            inChannel.read(buffer);
            buffer.flip();
            ByteString file = ByteString.fromByteBuffer(buffer);
            inChannel.close();

            parsePacket(file.splitAt(offset)._2());




        }
       else if (message instanceof WorkerMsgs.FileChunk) {

            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();
            Path path = Paths.get("/tmp/" + cluster.selfAddress().hashCode());

            ByteIterator it = ((WorkerMsgs.FileChunk) message).data.iterator();
            ByteIterator itCopy;
            int ts_usec = 0;
            int ts_sec = 0;
            int offset = 0;

            while (it.hasNext()) {
                itCopy = it.clone();
                ts_sec = itCopy.getInt(ByteOrder.LITTLE_ENDIAN);
                ts_usec = itCopy.getInt(ByteOrder.LITTLE_ENDIAN);
                int incl_len = itCopy.getInt(ByteOrder.nativeOrder());
                int orig_len = itCopy.getInt(ByteOrder.nativeOrder());
                itCopy.getBytes(12);
                int ether_type = itCopy.getShort(ByteOrder.LITTLE_ENDIAN);
                if (incl_len == orig_len
                        && incl_len <= 65535 && incl_len >= 41
                        && ts_sec > 964696316
                        && validEthertypes.contains(ether_type))
                    break;
                else
                    it.next();
                offset++;
            }
            Date timestamp = new Date(ts_sec * 1000L + ts_usec / 1000);
            int chunkname = ((WorkerMsgs.FileChunk) message).data.hashCode();
            //if directory exists?
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    //fail to create directory
                    e.printStackTrace();
                }
            }
            Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname), CREATE, WRITE).write(((WorkerMsgs.FileChunk) message).data.toByteBuffer());
            system.actorSelection(address + "/user/database").tell(new DatabaseMsgs.DatabaseWrite(((WorkerMsgs.FileChunk) message).filename, timestamp, chunkname, offset ,address), self());
            //system.actorSelection("../database").tell(new DatabaseMsgs.DatabaseWrite(((WorkerMsgs.FileChunk) message).filename, timestamp, chunkname, offset ,address), self());
           // sender().tell(new DatabaseMsgs.DatabaseWrite(((WorkerMsgs.FileChunk) message).filename, timestamp, chunkname, offset ,address), self());

        }

    else{
        unhandled(message);
    }
}}
