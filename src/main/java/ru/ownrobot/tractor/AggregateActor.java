package ru.ownrobot.tractor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;


/**
 * Created by laboshinl on 8/4/16.
 */
public class AggregateActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    HashMap<String,Integer> aggregator = new HashMap<String,Integer>();
    Integer finishedJobs = 0;
    public void onComplete() {

    }
    @Override
    public void onReceive(Object message) throws Throwable {
        WorkerMsgs.TcpData msg = (WorkerMsgs.TcpData) message;
        finishedJobs++;
        HashMap<String, Integer> freq = new HashMap<String, Integer>();
        int count = freq.containsKey(msg.direction) ? freq.get(msg.direction) : 0;
        freq.put(msg.direction, count + 1);
        System.out.println(freq);
    }
    public void preStart(){
        log.error("Aggregate actor started");
    }
    public void postStop(){
        log.error("Aggregate actor stopped");
    }
}
