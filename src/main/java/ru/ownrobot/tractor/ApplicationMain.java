package ru.ownrobot.tractor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ApplicationMain {

    public static void main(String[] args) {

		JFileChooser fileopen = new JFileChooser("/home/laboshinl/Downloads");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".pcap", "pcap");
        fileopen.setFileFilter(filter);
		int ret = fileopen.showDialog(null, "Open File");

		if (ret == JFileChooser.APPROVE_OPTION) {
		    new FileUploadMain(fileopen.getSelectedFile().getAbsolutePath());
		}

    }
}