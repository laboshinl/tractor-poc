package ru.ownrobot.tractor;

import java.io.Serializable;
import java.util.Date;

import akka.actor.Address;

/**
 * Created by laboshinl on 8/2/16.
 */
public class DatabaseMsgs {
    public static class DatabaseWrite implements Serializable {
        public Long chunkname;
        public Integer offset;
        public Date timestamp;
        public String filename;
        public String address;

        public DatabaseWrite(String filename, Date timestamp, Long chunkname, Integer offset, String address){
            this.chunkname = chunkname;
            this.offset = offset;
            this.timestamp = timestamp;
            this.filename = filename;
            this.address = address;
        }
        @Override
        public String toString() {
            return String.format("Name (%s) time(%s) chunk(%s) offset(%s) address(%s)", filename, timestamp, chunkname, offset, address );
        }

    }


//    public static class NextChunkRequest implements Serializable {
//        public String chunkname;
//        public String filename;
//        public NextChunkRequest(String filename, String chunkname){
//            this.chunkname = chunkname;
//            this.filename = filename;
//        }
//        @Override
//        public String toString() {
//            return String.format("File (%s) chunk (%s)", filename, chunkname);
//        }
//    }
    public static class FileListRequest implements Serializable {

    }
    public static class JobListRequest implements Serializable {

    }
//
//    public static class NextChunkResponce implements Serializable {
//        public String chunkname;
//        public String address;
//        public Integer size;
//        public NextChunkResponce(String chunkname, String address, Integer size ){
//            this.chunkname = chunkname;
//            this.address = address;
//            this.size = size;
//        }
//        @Override
//        public String toString() {
//            return String.format("Chunk (%s) address (%s)", chunkname, address);
//        }
//    }

    public static class FileJob implements Serializable {
        public String filename;
        public String jobId;
        public FileJob(String filename, String jobId){
            this.filename = filename;
            this.jobId = jobId;
        }
    }

    public static class FileJobResponce implements Serializable {
        public String address;
        public String nextAddress;
        public Long chunkname;
        public Integer nextOffset;
        public Integer offset;
        public Long nextChunkname;
        public Integer chunkCount;
        public String jobId;

        public FileJobResponce(String address, Long chunkname, Integer offset, String nextAddress, Long nextChunkname, Integer nextOffset, Integer chunkCount, String jobId){
            this.address = address;
            this.chunkname = chunkname;
            this.nextAddress = nextAddress;
            this.nextOffset = nextOffset;
            this.offset = offset;
            this.nextChunkname = nextChunkname;
            this.chunkCount = chunkCount;
            this.jobId = jobId;
        }
        @Override
        public String toString(){
            return String.format("Address (%s) chunkname (%s) nextAddress (%s) nextSize (%s)",  address, chunkname, nextAddress, nextOffset);
        }
    }
}
