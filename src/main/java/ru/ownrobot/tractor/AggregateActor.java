package ru.ownrobot.tractor;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
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
    HashMap<String, Integer> finJobs = new HashMap<>();
    Integer finishedJobs = 0;

    ActorSystem system = getContext().system();
    ActorMaterializer materializer = ActorMaterializer.create(system);

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof WorkerMsgs.TcpData) {
            WorkerMsgs.TcpData msg = (WorkerMsgs.TcpData) message;

            TreeMap<Long, ByteString> flow = freq.containsKey(msg.direction) ? freq.get(msg.direction) : new TreeMap<>();
            flow.put(msg.seqNumber, msg.tcpData);
            freq.put(msg.direction, flow);

        }
        else if (message instanceof WorkerMsgs.JobStatus){
            WorkerMsgs.JobStatus status = (WorkerMsgs.JobStatus) message;
            Integer count = finJobs.containsKey(status.jobId) ? finJobs.get(status.jobId) : 0;
            finJobs.put(status.jobId, count + 1);
            //finishedJobs++;
            System.out.println(String.format("%s/%s jobs finished ID=%s", count+1, status.numProcessed, status.jobId));
//            double percent = finishedJobs/status.numProcessed;
            if (finJobs.get(status.jobId).equals(status.numProcessed)) {
                //System.out.println(freq.entrySet().iterator().next().getValue().values());
//                Source.from(freq.entrySet()).runForeach(i -> {System.out.println(String.format("%s, %s", i.getKey(), i.getValue()));}, materializer).handle((done, failure) -> {
//                Source.from(freq.entrySet()).runForeach(i -> { i.getValue().forEach((k, v) -> { if (v.utf8String().contains("HTTP")) {System.out.println(k/*v.indexOf(ByteString.fromString("/r/n/r/n", "UTF-8"))*/); /*System.out.println(i.getKey());*/}});}, materializer).handle((done, failure) -> {
                System.out.println("All done! =)");
//                Source.from(freq.entrySet()).runForeach(i -> System.out.println(String.format("%s %s", i.getKey(), i.getValue().size())), materializer).handle((done, failure) -> {

//                    return NotUsed.getInstance();
//                });
                freq.clear();
                finJobs.remove(status.jobId);
                system.actorFor("/user/database").tell(new WorkerMsgs.JobStatus(status.jobId, 100), self());
//
//                freq.entrySet().stream().sorted(HashMap.Entry.<String, Integer>comparingByValue().reversed())
//                .limit(10)
//                        .forEach(System.out::println);
                //System.out.println(freq);
            }/*else
                system.actorFor("/user/database").tell(new WorkerMsgs.JobStatus(status.jobId, (int) percent), self());*/

        } else {
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Aggregate actor started");
    }
    public void postStop(){
        log.info("Aggregate actor stopped");
    }
}
