package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.ownrobot.tractor.ProtoMessages.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class ChunkDeleteActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final Config config = ConfigFactory.load();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ChunkDeleteRequest) {

            Long chunkId = ((ChunkDeleteRequest) message).getChunkName();
            Path path = Paths.get(config.getString("filesystem.path") + File.separator + chunkId);

            try {
                Files.delete(path);
            } catch (NoSuchFileException x) {
                log.error("{}: no such file or directory", path);
            } catch (DirectoryNotEmptyException x) {
                log.error("{} not empty", path);
            } catch (IOException x) {
                log.error("{} wrong permissions", x);
            }
        } else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
}
