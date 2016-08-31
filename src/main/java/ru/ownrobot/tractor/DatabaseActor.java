package ru.ownrobot.tractor;

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
import com.mongodb.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import ru.ownrobot.tractor.ProtoMessages.*;
import ru.ownrobot.tractor.KryoMessages.*;


public class DatabaseActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = getContext().system();
    private final Config config = ConfigFactory.load();
    private final Random random = new Random();
    private final Cluster cluster = Cluster.get(system);

    private int random() {
        return Math.abs(random.nextInt()) % config.getInt("workers.count");
    }

    private final List<ActorSelection> jobTrackers = getJobTrackers();


    private ActorSelection selectJobTracker(String jobId) {
        return jobTrackers.get(Math.abs(jobId.hashCode() % jobTrackers.size()));
    }

    private List<ActorSelection> getJobTrackers() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<ActorSelection> jobTrackers = new ArrayList<>();
        cluster.state().getMembers().forEach(m -> {
            jobTrackers.add(system.actorSelection(m.address() + "/user/jobTracker"));
        });
        return jobTrackers;
    }

    private DBCollection connectDatabase() {
        String host = config.getString("mongo.host");
        Integer port = config.getInt("mongo.port");
        String db = config.getString("mongo.db");
        String table = config.getString("mongo.collection");
        DBCollection collection = null;
        try {
            collection = new MongoClient(host, port).getDB(db).getCollection(table);
        } catch (Exception e) {
            log.error("DatabaseActor connection error: {}", e);
        }
        return collection;
    }

    private final DBCollection collection = connectDatabase();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof DBRecord) {
            DBRecord record = (DBRecord) message;
            BasicDBObject document = new BasicDBObject();
            document.put("fileName", record.getFileName());
            document.put("timestamp",record.getTimestamp());
            document.put("chunkId", record.getChunkId());
            document.put("offset", record.getOffset());
            document.put("address", record.getAddress());
            collection.insert(document);
        }
        else if (message instanceof FileDeleteRequest) {
            String fileName = ((FileDeleteRequest) message).getFileName();
            DBCursor cursor = collection.find(new BasicDBObject("fileName", fileName));
            while (cursor.hasNext()) {
                DBObject item = cursor.next();
                system.actorSelection(item.get("address") + "/user/chunkdelete" + random())
                        .tell(ChunkDeleteRequest.newBuilder().setChunkName((Long) item.get("chunkId")).build(), self());
                collection.remove(item);
            }

        } else if (message instanceof JobDeleteRequest) {
            String jobId = ((JobDeleteRequest) message).getJobId();
            DBCursor cursor = collection.find(new BasicDBObject("jobId", jobId));
            while (cursor.hasNext()) {
                DBObject item = cursor.next();
                collection.remove(item);
            }

        } else if (message instanceof FileListRequest) {
            HashMap<String, Integer> result = new HashMap<>();
            @SuppressWarnings("unchecked")
            List<String> files = collection.distinct("fileName");
            files.forEach(i -> result.put(i,
                    collection.find(new BasicDBObject("fileName", i)).size() * config.getInt("filesystem.chunksize")));
            getSender().tell(result, self());

        } else if (message instanceof JobStatusMsg) {
            JobStatusMsg jobStatus = (JobStatusMsg) message;

            DBObject exists = new BasicDBObject("jobId", jobStatus.getJobId());
            BasicDBObject document = new BasicDBObject();
            document.put("jobId", jobStatus.getJobId());
            document.put("status", jobStatus.getFinished());
            collection.update(exists, document, true, false);


        } else if (message instanceof JobListRequest) {
            HashMap<String, Integer> result = new HashMap<>();

            @SuppressWarnings("unchecked")
            List<String> files = collection.distinct("uuid");
            files.forEach(i -> result.put(i,
                    (Integer) collection.find(new BasicDBObject("uuid", i)).toArray().get(0).get("status")));
            getSender().tell(result, self());


        } else if (message instanceof FileProcessRequest) {
            FileProcessRequest fileProcess = (FileProcessRequest) message;

            String fileName = fileProcess.getFileName();
            String jobId = fileProcess.getJobId();

            DBCursor result = collection.find(new BasicDBObject("filename", fileName)).sort(new BasicDBObject("timestamp", 1));

            selectJobTracker(jobId).tell(NewJobMsg.newBuilder().setJobId(jobId).setCount(result.length()).build(), self());

            if (result.length() != 0) {
                List<DBObject> array = result.toArray();
                for (int i = 0; i < array.size(); i++) {
                    String address = array.get(i).get("address").toString();
                    Long chunkId = (Long) array.get(i).get("chunkname");
                    Long nextChunkId = 0L;
                    Integer offset = (Integer) array.get(i).get("offset");
                    String nextAddress = new String();
                    Integer nextOffset = 0;
                    if ((i + 1) < array.size()) {
                        nextAddress = array.get(i + 1).get("address").toString();
                        nextOffset = (Integer) array.get(i + 1).get("offset");
                        nextChunkId = (Long) array.get(i + 1).get("chunkname");
                    }
                    ChunkProcessRequest request = ChunkProcessRequest.newBuilder()
                            .setChunkCount(array.size())
                            .setChunkName(chunkId)
                            .setJobId(jobId)
                            .setNextChunkName(nextChunkId)
                            .setNextNodeAddress(nextAddress)
                            .setNextOffset(nextOffset)
                            .setOffset(offset)
                            .setNodeAddress(address)
                            .build();
                    system.actorSelection(address + "/user/worker" + random()).tell(request, self());

                }
            } else
                log.error("No such file in DatabaseActor");
        } else {
            log.info("Unknown database message type %s", message.getClass());
            unhandled(message);
        }
    }

    public void preStart() {
        log.info("DatabaseActor actor started");
    }

    public void postStop() {
        log.info("DatabaseActor actor stopped");
    }
}
