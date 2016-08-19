package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.nio.file.*;

public class ChunkDeleteActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Config config = ConfigFactory.load();
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof WorkerMsgs.DeleteChunk) {

            Long chunkname = ((WorkerMsgs.DeleteChunk) message).chunkname;
            Path path = Paths.get(config.getString("filesystem.path") + "/" + chunkname);

            try {
                Files.delete(path);
            } catch (NoSuchFileException x) {
                log.error("{}: no such file or directory", path);
            } catch (DirectoryNotEmptyException x) {
                log.error("{} not empty", path);
            } catch (IOException x) {
                // File permission problems are caught here.
                log.error("{} wrong permissions", x);
            }
        } else {
            unhandled(message);
        }
    }
}
