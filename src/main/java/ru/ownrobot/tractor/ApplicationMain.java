package ru.ownrobot.tractor;

import akka.actor.*;

import akka.cluster.Cluster;
import akka.remote.RemoteScope;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.io.Source$;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApplicationMain {

    public static class PrintActor extends UntypedActor {
       // final List<Routee> routees = new ArrayList<>();
        ActorSystem system = getContext().system();
        Cluster cluster = Cluster.get(getContext().system());

        public void PrintActor(){

        }
        @Override
        public void onReceive(Object message) throws Throwable {
           // if (message instanceof DatabaseMsgs.FileJobResponce){
                DatabaseMsgs.FileJobResponce job = (DatabaseMsgs.FileJobResponce)message;
           // System.out.println(job.address);
                ActorSelection worker = system.actorSelection(job.address+"/user/worker");
                ActorRef reducer = system.actorFor("/user/aggregate");
                worker.tell(message, reducer);

          //  }
            //System.out.println(message.toString());
        }

    }

    public static void main(String[] args) {
        //Create 3 worker nodes
        Integer [] ports = { 2551, 2552, 2553 };
        for (Integer port : ports) {
            // Override the configuration
            String options = String.format(
                    "akka.remote.netty.tcp.port=%1$d \n " +
                            "akka.cluster.roles = [worker] \n " +
                            "tractor.storage.path = /tmp/%1$d/", port
            );

            Config config = ConfigFactory.parseString(options).withFallback(
                    ConfigFactory.load());
            //System.out.println(config.getString("tractor.storage.path"));
            ActorSystem system = ActorSystem.create("ClusterSystem", config);
            system.actorOf(Props.create(Database.class),"database");
            system.actorOf(Props.create(ChunkBytesActor.class),"bytes");
            system.actorOf(Props.create(MapActor.class),"worker");
        }

        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + 0).withFallback(
                ConfigFactory.load());

        // Create test client
        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Integer chunkSize = 10 * (1024*1024); //bytes
        final String inPath = "/home/laboshinl/Downloads/bigFlows.pcap";
        final File inputFile = new File(inPath);


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();
        FileIO.fromFile(inputFile, chunkSize)
                .map(i -> new WorkerMsgs.FileChunk(inputFile.getName(), i ))
                .runWith(Sink.<WorkerMsgs.FileChunk>actorSubscriber(Props.create(FileSink.class)), materializer);
        long estimatedTime = System.currentTimeMillis() - startTime;

        System.out.printf("File %s saved! in %s ms.\n", inputFile.length() ,estimatedTime);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ActorRef reducer = system.actorOf(Props.create(AggregateActor.class),"aggregate");
        ActorRef printer = system.actorOf(Props.create(PrintActor.class), "printer");
        ActorSelection test = system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2552/user/database");
        test.tell(new DatabaseMsgs.FileJob("bigFlows.pcap"), printer);

    }
}