package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.mongodb.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laboshinl on 8/2/16.
 */
public class Database extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());

    private DBCollection connectDatabase() {
        Config config = ConfigFactory.load();
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

        } else if(message instanceof DatabaseMsgs.FileListRequest){
            List<String> result = collection.distinct("filename");
            System.out.println(result);

        } else if(message instanceof DatabaseMsgs.FileJob){
            String filename = ((DatabaseMsgs.FileJob) message).filename;
            DBCursor result = collection.find(new BasicDBObject("filename", filename)).sort(new BasicDBObject("timestamp", 1));
           // List<DatabaseMsgs.FileJobResponce> responce = new ArrayList<>();
            List <DBObject> array = result.toArray();
            for (int i =0; i< array.size(); i++){
                String address = array.get(i).get("address").toString();
                Integer chunkname = (Integer) array.get(i).get("chunkname");
                Integer nextChunkname = null;
                Integer offset = (Integer) array.get(i).get("offset");
                String nextAddress = null;
                Integer nextOffset = null;
                if( (i + 1) < array.size()){
                    nextAddress = array.get(i+1).get("address").toString();
                    nextOffset = (Integer) array.get(i+1).get("offset");
                    nextChunkname = (Integer) array.get(i).get("chunkname");
                }
                DatabaseMsgs.FileJobResponce item = new DatabaseMsgs.FileJobResponce(address, chunkname, offset, nextAddress, nextChunkname, nextOffset);
                getSender().tell(item,self());
                //responce.add(item);
            }
           // getSender().tell(responce, self());

        } else if (message instanceof DatabaseMsgs.NextChunkRequest) {
            DatabaseMsgs.NextChunkResponce nextChunk = null;
            String filename = ((DatabaseMsgs.NextChunkRequest) message).filename;
            String chunkname = ((DatabaseMsgs.NextChunkRequest) message).chunkname;
            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put("filename", filename);
            whereQuery.put("chunkname", chunkname);
            DBCursor result = collection.find(whereQuery);
            whereQuery = new BasicDBObject();
            whereQuery.put("filename", filename);
            whereQuery.put("timestamp", result.next().get("timestamp"));
            DBObject obj = collection.find(whereQuery).sort(new BasicDBObject("timestamp", 1)).limit(1).next();
            getSender().tell(new DatabaseMsgs.NextChunkResponce(obj.get("chunkname").toString(), obj.get("address").toString(), (Integer) obj.get("offset")), self());
        } else {
            log.info("Unknown database message type %s", message.getClass());
            unhandled(message);
        }
    }
}
