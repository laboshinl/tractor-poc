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
    HashMap<String, Integer> freq = new HashMap<String, Integer>();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof WorkerMsgs.TcpData) {
            WorkerMsgs.TcpData msg = (WorkerMsgs.TcpData) message;

            int count = freq.containsKey(msg.direction) ? freq.get(msg.direction) : 0;
            freq.put(msg.direction, count + 1);
//            //Thread.sleep(100);
//            //System.out.println(freq);

        }
        else if (message instanceof Integer){
            finishedJobs++;
            System.out.println(String.format("%s/%s jobs finished", finishedJobs, (Integer)message));
            if (finishedJobs == (Integer)message - 1) {
                freq.entrySet().stream().sorted(HashMap.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                        .forEach(System.out::println);
                //System.out.println(freq);
            }
        } else {
            unhandled(message);
        }
    }
    public void preStart(){
        log.error("Aggregate actor started");
    }
    public void postStop(){
        log.error("Aggregate actor stopped");
    }
}
