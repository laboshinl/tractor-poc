package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * Created by laboshinl on 8/5/16.
 */
public class FileUploadMain {
    Config config = ConfigFactory.load();
    public FileUploadMain(String inPath) {
        final Integer chunkSize = config.getInt("filesystem.chunksize") * (1024*1024); //bytes
        final File inputFile = new File(inPath);
        ActorSystem system = ActorSystem.create("ClusterSystem", ConfigFactory.load());
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        Cluster cluster = Cluster.get(system);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cluster.state().getMembers().forEach(m -> System.out.println(m.status()));
        FileIO.fromFile(inputFile, chunkSize)
                .map(i -> new WorkerMsgs.FileChunk(inputFile.getName(), i))
                .runWith(Sink.<WorkerMsgs.FileChunk>actorSubscriber(Props.create(FileSink.class)), materializer);
        //system.shutdown();

    }

    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("You must specify path to file");
        else
            new FileUploadMain(args[0]);
    }

    public void shutdown() {
        // TODO Auto-generated method stub
    }

    public void startup() {
        // TODO Auto-generated method stub
    }
}
