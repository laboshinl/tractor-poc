package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorSelectionRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import ru.ownrobot.tractor.ProtoMessages.*;
import ru.ownrobot.tractor.KryoMessages.*;

public class FileDownloadActor extends UntypedActor {

    private final Config config = ConfigFactory.load();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = getContext().system();
    private final Cluster cluster = Cluster.get(system);

    //private final List<ActorSelection> jobTrackers = new ArrayList<>();

    private RoutingUtils router = new RoutingUtils(system);

    private int getContentLength(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        URLConnection connection = url.openConnection();
        return connection.getContentLength();
    }

    private void splitDownload(int chunkSize, String url) {
        String jobId = UUID.randomUUID().toString();
        try {
            int fileSize = getContentLength(url);
            int chunkCount = fileSize / chunkSize;

            NewJobMsg newJob = NewJobMsg.newBuilder().setJobId(jobId).setCount(chunkCount).build();
            router.selectTracker(jobId).tell(newJob, self());

            for (int i = 0; i < chunkCount; i++) {
                int startPos = chunkSize * i;
                int endPos = (i == chunkCount - 1) ? fileSize
                        : (chunkSize * (i + 1) - 1);
                router.getDownloaderRouter().route(ChunkDownloadRequest.newBuilder().
                        setUrl(url).
                        setStartPos(startPos).
                        setEndPos(endPos).
                        setJobId(jobId).build(), self());
            }
        } catch (IOException e) {
            log.error("Error downloading file {}", e);
        }
    }

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof ChunkDownloadRequest) {

            ChunkDownloadRequest request = (ChunkDownloadRequest) message;
            URL url = new URL(request.getUrl());
            URLConnection urlConnection = url.openConnection();

            urlConnection.setRequestProperty("Range", "bytes="
                    + request.getStartPos() + "-" + request.getEndPos());

            InputStream stream = urlConnection.getInputStream();
            byte[] array = ByteStreams.toByteArray(stream);

            akka.util.ByteString chunkData = akka.util.ByteString.fromArray(array);

            router.getChunksaverRouter().route(new FileChunk(request.getJobId(), request.getUrl(), chunkData), self());

        } else if (message instanceof FileDownloadRequest) {
            FileDownloadRequest request = (FileDownloadRequest) message;
            splitDownload(config.getInt("filesystem.chunksize") * (1024 * 1024), request.getUrl());
        } else if (message instanceof JobStatusMsg) {
            JobStatusMsg jobStatus = (JobStatusMsg) message;
            router.selectTracker(jobStatus.getJobId()).tell(message, sender());
        } else if (message instanceof ClusterEvent.MemberEvent){
            router.updateMembers();
        }else {
            log.error("Unhandled message of type {} from {}", message.getClass(), getSender().path());
            unhandled(message);
        }

    }

    public void preStart() {
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
        log.info("Download actor {} started", self().path().address());
    }

    public void postStop() {
        log.info("Download actor {} stopped", self().path().address());
    }
}
