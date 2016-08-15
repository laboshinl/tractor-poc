package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Created by laboshinl on 8/5/16.
 */
public class WorkerMain implements Bootable {
    public WorkerMain(int port, String hostname) {
        Config config = ConfigFactory.parseString(String.format(
                "akka.remote.netty.tcp.bind-port=%1$d \n " +
                        "akka.remote.netty.tcp.port=%1$d \n " +
                        "akka.remote.netty.tcp.hostname=%2$s \n " +
                        "akka.cluster.roles = [worker] \n", port, hostname))
                .withFallback(ConfigFactory.load());
        ActorSystem system = ActorSystem.create("ClusterSystem", config);
        system.actorOf(Props.create(Database.class),"database");
        system.actorOf(Props.create(ChunkBytesActor.class),"bytes");
        system.actorOf(Props.create(ChunkSaveActor.class),"filesystem");
        system.actorOf(Props.create(MapActor.class),"worker");
        system.actorOf(Props.create(AggregateActor.class),"aggregator");

    }

    public static void main(String[] args) {
        if (args.length == 0)
            new WorkerMain(2551, "127.0.0.1");
        else
            new WorkerMain(/*Integer.parseInt(args[0]),*/ 2551, args[0]);
    }

    public void shutdown() {
        // TODO Auto-generated method stub
    }

    public void startup() {
        // TODO Auto-generated method stub
    }
}
