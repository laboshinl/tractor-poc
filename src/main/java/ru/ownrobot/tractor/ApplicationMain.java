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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApplicationMain {

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
            System.out.println(config.getString("tractor.storage.path"));
            ActorSystem system = ActorSystem.create("ClusterSystem", config);
            system.actorOf(Props.create(Database.class),"database");

        }

        Config config = ConfigFactory.parseString(
                "akka.remote.netty.tcp.port=" + 0).withFallback(
                ConfigFactory.load());

        // Create test client
        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        final Integer chunkSize = 100000; //bytes
        final String inPath = "/home/laboshinl/Downloads/smallFlows.pcap";
        final File inputFile = new File(inPath);


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FileIO.fromFile(inputFile, chunkSize)
                .map(i -> new WorkerMsgs.FileChunk(inputFile.getName(), i ))
                .runWith(Sink.<WorkerMsgs.FileChunk>actorSubscriber(Props.create(FileSink.class)), materializer);

    }
}