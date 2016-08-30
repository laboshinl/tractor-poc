package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.ownrobot.tractor.ProtoMessages.*;

public class SendBytesActor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Config config = ConfigFactory.load();

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof ExtraBytesRequest) {
            ExtraBytesRequest extraBytes = (ExtraBytesRequest) message;
            Long chunkId = extraBytes.getChunkId();
            Integer count = extraBytes.getCount();

            Path path = Paths.get(config.getString("filesystem.path") + File.separator + chunkId);

            ByteBuffer buffer = ByteBuffer.allocate(count);
            Files.newByteChannel(path).read(buffer);
            buffer.flip();
            ByteString result = ByteString.fromByteBuffer(buffer);
            sender().tell(result, self());
        } else {
            log.error("Unhandled message of type {}", message.getClass());
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
