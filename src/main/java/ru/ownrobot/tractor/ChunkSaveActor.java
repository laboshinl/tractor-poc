package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteIterator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bitbucket.dollar.Dollar;
import ru.ownrobot.tractor.KryoMessages.*;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class ChunkSaveActor extends UntypedActor {

    private final Config config = ConfigFactory.load();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final List<Integer> validEtherTypes =
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

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof FileChunk) {
            //Cast message
            FileChunk fileChunk = (FileChunk) message;

            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();

            Path path = Paths.get(config.getString("filesystem.path"));

            ByteIterator it = fileChunk.getChunkData().iterator();
            ByteIterator itCopy;

            int tsSec = 0;
            int tsUsec = 0;
            int offset = 0;
            int inclLen = 0;
            int origLen = 0;
            int etherType = 0;

            while (it.hasNext()) {
                itCopy = it.clone();
                try {
                    tsSec = itCopy.getInt(ByteOrder.LITTLE_ENDIAN);
                    tsUsec = itCopy.getInt(ByteOrder.LITTLE_ENDIAN);
                    inclLen = itCopy.getInt(ByteOrder.nativeOrder());
                    origLen = itCopy.getInt(ByteOrder.nativeOrder());
                    itCopy.getBytes(12);
                    etherType = itCopy.getShort(ByteOrder.LITTLE_ENDIAN);
                } catch (Exception e) {
                    log.error("Cannot find beggining of the packet record {}", e);
                }
                if (inclLen == origLen
                        && inclLen <= 65535 && inclLen >= 41
                        && tsSec > 964696316
                        && validEtherTypes.contains(etherType))
                    break;
                else
                    it.next();
                offset++;
            }

            Date timestamp = new Date(tsSec * 1000L + tsUsec / 1000);
            Long chunkId = fileChunk.getChunkData().hashCode() & 0xffffffffl;

            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Files.newByteChannel(Paths.get(path.toString() + "/" + chunkId), CREATE, WRITE)
                    .write(fileChunk.getChunkData().toByteBuffer());
            system.actorSelection("/user/database")
                    .tell(new DBRecord(chunkId,offset,timestamp,fileChunk.getFileName(),address),self());
            getSender().tell(ProtoMessages.JobStatusMsg.newBuilder().setJobId(fileChunk.getJobId()).setFinished(true).build(), self());
        } else {
            unhandled(message);
        }
    }
}
