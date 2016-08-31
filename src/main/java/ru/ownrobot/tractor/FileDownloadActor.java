package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
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
import java.util.List;
import java.util.UUID;

import ru.ownrobot.tractor.ProtoMessages.*;
import ru.ownrobot.tractor.KryoMessages.*;

public class FileDownloadActor extends UntypedActor {

    private final Config config = ConfigFactory.load();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = getContext().system();
    private final Cluster cluster = Cluster.get(system);

    private final List<ActorSelection> jobTrackers = new ArrayList<>();
    private final List<Router> routers = createRouters();


    private ActorSelection selectJobTracker(String jobId) {
        return jobTrackers.get(Math.abs(jobId.hashCode() % jobTrackers.size()));
    }

    private List<Router> createRouters() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Integer numWorkers = config.getInt("workers.count");
        List<Routee> fileDownload = new ArrayList<>();
        List<Routee> chunkSave = new ArrayList<>();
        cluster.state().getMembers().forEach(m -> {
//            if (m.hasRole("worker"))
            jobTrackers.add(system.actorSelection(m.address() + "/user/jobTracker"));
            for (int i = 0; i < numWorkers; i++) {
                chunkSave.add(new ActorSelectionRoutee(system.actorSelection(m.address() + "/user/filesystem" + i)));
                fileDownload.add(new ActorSelectionRoutee(system.actorSelection(m.address() + "/user/download" + i)));

            }
        });
        List<Router> routers = new ArrayList<>();
        routers.add(0, new Router(new RoundRobinRoutingLogic(), fileDownload));
        routers.add(1, new Router(new RoundRobinRoutingLogic(), chunkSave));
        return routers;
    }

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
            selectJobTracker(jobId).tell(newJob, self());

            for (int i = 0; i < chunkCount; i++) {
                int startPos = chunkSize * i;
                int endPos = (i == chunkCount - 1) ? fileSize
                        : (chunkSize * (i + 1) - 1);

                routers.get(0).route(ChunkDownloadRequest.newBuilder().
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

            routers.get(1).route(new FileChunk(request.getJobId(), request.getUrl(), chunkData), self());

        } else if (message instanceof FileDownloadRequest) {
            FileDownloadRequest request = (FileDownloadRequest) message;
            splitDownload(config.getInt("filesystem.chunksize") * (1024 * 1024), request.getUrl());
        } else if (message instanceof JobStatusMsg) {
            JobStatusMsg jobStatus = (JobStatusMsg) message;
            selectJobTracker(jobStatus.getJobId()).tell(message, sender());
        } else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }

    }

    public void preStart() {
        log.info("Download actor {} started", self().path().address());
    }

    public void postStop() {
        log.info("Download actor {} stopped", self().path().address());
    }
}
