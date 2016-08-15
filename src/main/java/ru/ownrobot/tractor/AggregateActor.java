package ru.ownrobot.tractor;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Created by laboshinl on 8/4/16.
 */
public class AggregateActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    HashMap<String, TreeMap<Long, ByteString>> freq = new HashMap<>();
    Integer finishedJobs = 0;
    //HashMap<String, Integer> freq = new HashMap<String, Integer>();
    ActorSystem system = getContext().system();
    ActorMaterializer materializer = ActorMaterializer.create(system);
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof WorkerMsgs.TcpData) {
            WorkerMsgs.TcpData msg = (WorkerMsgs.TcpData) message;

            //int count = freq.containsKey(msg.direction) ? freq.get(msg.direction) : 0;
            //freq.put(msg.direction, count + 1);
            TreeMap<Long, ByteString> flow = freq.containsKey(msg.direction) ? freq.get(msg.direction) : new TreeMap<Long, ByteString>();
            flow.put(msg.seqNumber, msg.tcpData);
            freq.put(msg.direction, flow);
//            //Thread.sleep(100);
//            //System.out.println(freq);

        }
        else if (message instanceof Integer){
            finishedJobs++;
            System.out.println(String.format("%s/%s jobs finished", finishedJobs, (Integer)message));
            if (finishedJobs == (Integer)message) {
                //System.out.println(freq.entrySet().iterator().next().getValue().values());
//                Source.from(freq.entrySet()).runForeach(i -> {System.out.println(String.format("%s, %s", i.getKey(), i.getValue()));}, materializer).handle((done, failure) -> {
                Source.from(freq.entrySet()).runForeach(i -> { i.getValue().forEach((k, v) -> { if (v.utf8String().contains("linkedin.com")) {System.out.println(i.getKey());}});}, materializer).handle((done, failure) -> {

                    freq.clear();
                    finishedJobs = 0;
                    return NotUsed.getInstance();
                });

//                freq.entrySet().stream().sorted(HashMap.Entry.<String, Integer>comparingByValue().reversed())
//                .limit(10)
//                        .forEach(System.out::println);
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
