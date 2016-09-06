package ru.ownrobot.tractor;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;

import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import ru.ownrobot.tractor.KryoMessages.JobProgress;
import ru.ownrobot.tractor.ProtoMessages.JobFinishedMsg;
import ru.ownrobot.tractor.ProtoMessages.JobStatusMsg;
import ru.ownrobot.tractor.ProtoMessages.NewJobMsg;

import java.util.HashMap;

public class JobTrackActor extends UntypedActor {
    private final HashMap<String, JobProgress> jobs = new HashMap<>();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private final ActorSystem system = getContext().system();
    private final RoutingUtils router = new RoutingUtils(system);

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
                    router.getAggregators().forEach(i -> i.tell(JobFinishedMsg.newBuilder().setJobId(jobStatus.getJobId()).build(), self()));
                    system.actorSelection("/user/database").tell(JobStatusMsg.newBuilder().setFinished(true).setJobId(jobStatus.getJobId()).setProgress(100).build(), self());
                    jobs.remove(jobStatus.getJobId());
                } else
                    jobs.put(jobStatus.getJobId(), progress);
                    system.actorSelection("/user/database").tell(JobStatusMsg.newBuilder().setFinished(false).setJobId(jobStatus.getJobId()).setProgress(progress.getProgress()).build(), self());
            }
            else
                log.error("No such job {}", jobStatus.getJobId());
        } else if (message instanceof ClusterEvent.MemberEvent){
        router.updateMembers();

    }
        else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        Cluster.get(getContext().system()).subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.MemberEvent.class, ClusterEvent.UnreachableMember.class);
        log.info("Tracker actor started");
    }
    public void postStop(){
        log.info("Tracker actor stopped");
    }

}
