package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.japi.pf.ReceiveBuilder;
import akka.remote.RemoteScope;
import akka.routing.*;
import akka.stream.ActorMaterializer;
import akka.stream.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laboshinl on 8/2/16.
 */
public class FileSink extends AbstractActorSubscriber {
    Config config = ConfigFactory.load();
    @Override
    public RequestStrategy requestStrategy() {
        return new WatermarkRequestStrategy(10);
    }
    public FileSink (){
        final List<Routee> routees = new ArrayList<>();
        ActorSystem system = getContext().system();
        Cluster cluster = Cluster.get(getContext().system());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Integer numWorkers = config.getInt("workers.count");
        cluster.state().getMembers().forEach(m -> {
                    if (m.hasRole("worker"))
                        for (int i =0; i<numWorkers; i++)
                        routees.add(new ActorSelectionRoutee(system.actorSelection(m.address() + "/user/filesystem"+i)));
                });

        Router router = new Router(new RoundRobinRoutingLogic(), routees);
        receive(ReceiveBuilder.
                match(ActorSubscriberMessage.OnNext.class, on -> on.element() instanceof WorkerMsgs.FileChunk,
                        onNext -> {
                            WorkerMsgs.FileChunk msg = (WorkerMsgs.FileChunk) onNext.element();
                            System.out.println("#");
                            router.route(msg, self());
                        }
                ).build());

        }
    }
