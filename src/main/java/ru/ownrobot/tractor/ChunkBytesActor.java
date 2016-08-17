package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkBytesActor extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Config config = ConfigFactory.load();

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof WorkerMsgs.ByteRequest) {
            Long chunkname = ((WorkerMsgs.ByteRequest) message).chunkname;
            Integer size = ((WorkerMsgs.ByteRequest) message).size;
            ActorSystem system = getContext().system();
            Cluster cluster = Cluster.get(system);

            Path path = Paths.get(config.getString("filesystem.path"));

            ByteBuffer buffer = ByteBuffer.allocate(size);
            Files.newByteChannel(Paths.get(path.toString() + "/" + chunkname)).read(buffer);
            buffer.flip();
            ByteString result = ByteString.fromByteBuffer(buffer);
            sender().tell(result, self());
        } else {
            unhandled(message);
        }
    }

    public void preStart() {
        log.info("ChunkBytes actor started");
    }

    public void postStop() {
        log.info("ChunkBytes actor stopped");
    }
}
