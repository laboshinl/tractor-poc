package ru.ownrobot.tractor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Created by laboshinl on 8/5/16.
 */
public class ChunkRouter extends UntypedActor {

    ActorSystem system = getContext().system();
    LoggingAdapter log = Logging.getLogger(system, this);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof DatabaseMsgs.FileJobResponce ) {
            DatabaseMsgs.FileJobResponce job = (DatabaseMsgs.FileJobResponce) message;
            ActorRef worker = system.actorFor(job.address + "/user/worker");
            ActorRef reducer = system.actorFor("akka.tcp://ClysterSystem@node3.ownrobot.ru:2551/user/aggregate");
            worker.tell(message, reducer);
        }
        else{
            unhandled(message);
        }
    }
    public void preStart(){
        log.error("ChunkRouter actor started");
    }
    public void postStop(){
        log.error("ChunkRouter actor stopped");
    }
}