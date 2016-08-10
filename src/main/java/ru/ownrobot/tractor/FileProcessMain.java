package ru.ownrobot.tractor;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.routing.ActorRefRoutee;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import com.typesafe.config.ConfigFactory;
import ru.ownrobot.tractor.DatabaseMsgs.FileListRequest;

import java.io.File;
import java.util.List;

/**
 * Created by laboshinl on 8/5/16.
 */
public class FileProcessMain  {
    public FileProcessMain(String filename) {
        ActorSystem system = ActorSystem.create("ClusterSystem", ConfigFactory.load());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ActorRef dbInstance = system.actorFor("/user/database");
        //ActorRef dbInstance = system.actorFor(system.actorSelection("/user/database").pathString());
        ActorRef chunkRouter = system.actorOf(Props.create(ChunkRouter.class), "chunkRouter");
        ActorRef AggregateActor = system.actorOf(Props.create(AggregateActor.class), "aggregate");
        //dbInstance.tell(new FileListRequest(), self());
        dbInstance.tell(new DatabaseMsgs.FileJob(filename), chunkRouter);
        //system.shutdown();
    }

    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("You must specify filename");
        else
            new FileProcessMain(args[0]);
    }

}