package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.mongodb.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by laboshinl on 8/2/16.
 */
public class Database extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    ActorSystem system = getContext().system();
    Cluster cluster = Cluster.get(system);
    Config config = ConfigFactory.load();

    private DBCollection connectDatabase() {
        String host = config.getString("mongo.host");
        Integer port = config.getInt("mongo.port");
        String db = config.getString("mongo.db");
        String table = config.getString("mongo.collection");
        DBCollection collection = null;
        try {
            collection = new MongoClient(host, port).getDB(db).getCollection(table);
        }
        catch (Exception e){
            log.error("Database connection error: {}", e);
        }
        return collection;
    }

    final DBCollection collection = connectDatabase();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof DatabaseMsgs.DatabaseWrite) {
            log.debug("DatabaseWrite recieved");
            BasicDBObject document = new BasicDBObject();
            document.put("filename", ((DatabaseMsgs.DatabaseWrite) message).filename);
            document.put("timestamp", ((DatabaseMsgs.DatabaseWrite) message).timestamp);
            document.put("chunkname", ((DatabaseMsgs.DatabaseWrite) message).chunkname);
            document.put("offset", ((DatabaseMsgs.DatabaseWrite) message).offset);
            document.put("address", ((DatabaseMsgs.DatabaseWrite) message).address);
            collection.insert(document);

        } else if (message instanceof DatabaseMsgs.FileListRequest) {
            HashMap<String, Integer> result = new HashMap<>();
            List<String> files = collection.distinct("filename");
            files.forEach(i -> result.put(i,
                    collection.find(new BasicDBObject("filename", i)).size() * config.getInt("filesystem.chunksize")));
            getSender().tell(result, self());

        } else if(message instanceof WorkerMsgs.JobStatus) {
            BasicDBObject document = new BasicDBObject();
            document.put("uuid", ((WorkerMsgs.JobStatus) message).jobId);
            document.put("status", ((WorkerMsgs.JobStatus) message).numProcessed);
            collection.insert(document);

        } else if (message instanceof DatabaseMsgs.JobListRequest) {
            HashMap<String, Integer> result = new HashMap<>();
            // result = collection.distinct("uuid");
            List<String> files = collection.distinct("uuid");

            files.forEach(i -> result.put(i,
                    (Integer) collection.find(new BasicDBObject("uuid", i)).toArray().get(0).get("status")));
            getSender().tell(result, self());


        } else if(message instanceof DatabaseMsgs.FileJob){
            String filename = ((DatabaseMsgs.FileJob) message).filename;
            String jobId = ((DatabaseMsgs.FileJob) message).jobId;
            DBCursor result = collection.find(new BasicDBObject("filename", filename)).sort(new BasicDBObject("timestamp", 1));
            System.out.println("I'm alive!");
            if (result.length() != 0) {
                List <DBObject> array = result.toArray();
                for (int i = 0; i < array.size(); i++) {
                    String address = array.get(i).get("address").toString();
                    Long chunkname = (Long) array.get(i).get("chunkname");
                    Long nextChunkname = null;
                    Integer offset = (Integer) array.get(i).get("offset");
                    String nextAddress = null;
                    Integer nextOffset = null;
                    if ((i + 1) < array.size()) {
                        nextAddress = array.get(i + 1).get("address").toString();
                        nextOffset = (Integer) array.get(i + 1).get("offset");
                        nextChunkname = (Long) array.get(i + 1).get("chunkname");
                    }
                    DatabaseMsgs.FileJobResponce item = new DatabaseMsgs.FileJobResponce(address, chunkname, offset, nextAddress, nextChunkname, nextOffset, array.size(), jobId );
                    system.actorSelection(address + "/user/worker").tell(item, self());
                }
            }
            else
                log.error("No such file in Database");
        } else {
            log.info("Unknown database message type %s", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        log.error("Database actor started");
    }
    public void postStop(){
        log.error("Database actor stopped");
    }
}
