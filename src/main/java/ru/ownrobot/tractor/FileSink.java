package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.Deploy;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.japi.pf.ReceiveBuilder;
import akka.remote.RemoteScope;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import akka.stream.actor.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laboshinl on 8/2/16.
 */
public class FileSink extends AbstractActorSubscriber {

    @Override
    public RequestStrategy requestStrategy() {
        return new WatermarkRequestStrategy(10);
    }
    public FileSink (){
        final List<Routee> routees = new ArrayList<>();
        ActorSystem system = getContext().system();
        Cluster cluster = Cluster.get(getContext().system());
        cluster.state().getMembers().forEach(m -> {
                    if (m.hasRole("worker"))
                        routees.add(new ActorRefRoutee(system.actorFor(m.address() + "/user/worker")));
                });
//                        routees.add(new ActorRefRoutee(system.actorOf(Props.create(ChunkSaveActor.class).
//                                withDeploy(new Deploy(new RemoteScope(m.address()))))));});
        Router router = new Router(new RoundRobinRoutingLogic(), routees);
        receive(ReceiveBuilder.
                match(ActorSubscriberMessage.OnNext.class, on -> on.element() instanceof WorkerMsgs.FileChunk,
                        onNext -> {
                            WorkerMsgs.FileChunk msg = (WorkerMsgs.FileChunk) onNext.element();
                            router.route(msg, self());
                        }
                ).build());

        }
    }
