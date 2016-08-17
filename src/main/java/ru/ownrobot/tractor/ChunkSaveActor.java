package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.util.ByteIterator;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.bitbucket.dollar.Dollar;

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

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof WorkerMsgs.FileChunk) {

            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);
            String address = cluster.selfAddress().toString();

            Path path = Paths.get("/tmp/" + (address.hashCode() & 0xffffffffl));

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
            Long chunkname = ((WorkerMsgs.FileChunk) message).data.hashCode() & 0xffffffffl;

            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                  e.printStackTrace();
                }
            }
            Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname), CREATE, WRITE)
                    .write(((WorkerMsgs.FileChunk) message).data.toByteBuffer());
            system.actorFor("/user/database")
                    .tell(new DatabaseMsgs.DatabaseWrite(((WorkerMsgs.FileChunk) message).filename,
                            timestamp, chunkname, offset, address), self());
        } else {
            unhandled(message);
        }
    }
}
