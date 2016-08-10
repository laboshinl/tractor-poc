package ru.ownrobot.tractor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ApplicationMain {

    public static void main(String[] args) {
        //Create 3 worker nodes
        Integer [] ports = { 2551, 2552, 2553 };
        for (Integer port : ports) {
            new WorkerMain(port);
        }

		JFileChooser fileopen = new JFileChooser("/home/laboshinl/Downloads");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".pcap", "pcap");
        fileopen.setFileFilter(filter);
		int ret = fileopen.showDialog(null, "Open File");

		if (ret == JFileChooser.APPROVE_OPTION) {
		    new FileUploadMain(fileopen.getSelectedFile().getAbsolutePath());
		}
//        ActorSystem system = ActorSystem.create("ClusterSystem", ConfigFactory.load());

        new FileProcessMain(fileopen.getSelectedFile().getName());

    }
}