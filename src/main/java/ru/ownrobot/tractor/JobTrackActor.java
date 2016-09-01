package ru.ownrobot.tractor;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ru.ownrobot.tractor.KryoMessages.JobProgress;
import ru.ownrobot.tractor.ProtoMessages.JobFinishedMsg;
import ru.ownrobot.tractor.ProtoMessages.JobStatusMsg;
import ru.ownrobot.tractor.ProtoMessages.NewJobMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JobTrackActor extends UntypedActor {
    private final HashMap<String, JobProgress> jobs = new HashMap<>();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = getContext().system();
    private final Cluster cluster = Cluster.get(system);
    private final Config config = ConfigFactory.load();

    private final List<ActorSelection> aggregators = getAggregators();

    private List<ActorSelection> getAggregators() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<ActorSelection> aggregators = new ArrayList<>();
        cluster.state().getMembers().forEach(m -> {
            for (int i =0; i< config.getInt("workers.count"); i++) {
                aggregators.add(system.actorSelection(m.address() + "/user/aggregator" + i));
            }
        });
        return aggregators;
    }

    @Override
    public void onReceive(Object message) throws Throwable {

        if (message instanceof NewJobMsg) {
            NewJobMsg newJob = (NewJobMsg) message;
            jobs.put(newJob.getJobId(), new JobProgress(newJob.getCount()));
            getSender().tell("Ok", self());
        }
        else if (message instanceof JobStatusMsg) {
            JobStatusMsg jobStatus = (JobStatusMsg) message;
            if(jobs.containsKey(jobStatus.getJobId())) {
                JobProgress progress = jobs.get(jobStatus.getJobId());
                progress.addWorker(getSender());
                progress.increment();
                log.info("Job {} {} % complete", jobStatus.getJobId(), progress.getProgress());
                if (progress.isFinished()) {

//                    progress.getWorkers()
//                            .forEach(i -> i.tell(JobFinishedMsg.newBuilder().setJobId(jobStatus.getJobId()).build(), self()));
                    aggregators.forEach(i -> i.tell(JobFinishedMsg.newBuilder().setJobId(jobStatus.getJobId()).build(), self()));
                    jobs.remove(jobStatus.getJobId());
                } else
                    jobs.put(jobStatus.getJobId(), progress);
            }
            else
                log.error("No such job {}", jobStatus.getJobId());
        }
        else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Tracker actor started");
    }
    public void postStop(){
        log.info("Tracker actor stopped");
    }

}
