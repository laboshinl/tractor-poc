package ru.ownrobot.tractor;

import akka.util.ByteString;

import java.io.Serializable;

/**
 * Created by laboshinl on 8/2/16.
 */
public class WorkerMsgs {
    public static class FileChunk implements Serializable{
        public ByteString data;
        public String filename;
        public FileChunk(String filename, ByteString data){
            this.filename = filename;
            this.data = data;
        }
    }

    public static class ByteRequest implements Serializable {
        public Integer chunkname;
        public Integer size;
        public ByteRequest(Integer chunkname, Integer size){
            this.chunkname = chunkname;
            this.size = size;
        }
    }
//    public static class ByteResponce implements Serializable {
//        public Integer chunkname;
//        public ByteString data;
//        public ByteResponce(Integer chunkname, ByteString data){
//            this.chunkname = chunkname;
//            this.data = data;
//        }
//    }

//    public class MapTask {
//        public String address;
//        public String nextAddress;
//        public String chunkname;
//        public Integer nextSize;
//
//        public MapTask(String address, String chunkname, String nextAddress, Integer nextSize){
//            this.address = address;
//            this.chunkname = chunkname;
//            this.nextAddress = nextAddress;
//            this.nextSize = nextSize;
//        }
//        @Override
//        public String toString(){
//            return String.format("Address (%s) chunkname (%s) nextAddress (%s) nextSize (%s)",  address, chunkname, nextAddress, nextSize);
//        }
//    }
}
