package ru.ownrobot.tractor;

import akka.actor.UntypedActor;

import java.io.File;
import java.util.HashMap;


import com.itextpdf.awt.DefaultFontMapper;

import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.text.Document;

import java.awt.*;
import java.io.FileOutputStream;

import java.awt.geom.Rectangle2D;


class PdfReport {


    public static JFreeChart generatePieChart(KryoMessages.JobResult result) {
        DefaultPieDataset dataSet = new DefaultPieDataset();
//        result.getFlows().entrySet().forEach(i -> i.getValue().ge);
        dataSet.setValue("China", 19.64);
        dataSet.setValue("India", 17.3);
        dataSet.setValue("United States", 4.54);
        dataSet.setValue("Indonesia", 3.4);
        dataSet.setValue("Brazil", 2.83);
        dataSet.setValue("Pakistan", 2.48);
        dataSet.setValue("Bangladesh", 2.38);

        JFreeChart chart = ChartFactory.createPieChart(
                "World Population by countries", dataSet, true, true, false);

        return chart;
    }

//    public static JFreeChart generateBarChart() {
//        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
//        dataSet.setValue(791, "Population", "1750 AD");
//        dataSet.setValue(978, "Population", "1800 AD");
//        dataSet.setValue(1262, "Population", "1850 AD");
//        dataSet.setValue(1650, "Population", "1900 AD");
//        dataSet.setValue(2519, "Population", "1950 AD");
//        dataSet.setValue(6070, "Population", "2000 AD");
//
//        JFreeChart chart = ChartFactory.createBarChart(
//                "World Population growth", "Year", "Population in millions",
//                dataSet, PlotOrientation.VERTICAL, false, true, false);
//
//        return chart;
//    }

//    public static void main(String[] args) {
//        writeChartToPDF(generateBarChart(), 500, 400, "/home/laboshinl/barchart.pdf");
//        writeChartToPDF(generatePieChart(), 500, 400, "/home/laboshinl/piechart.pdf");
//    }
    public static void writeChartToPDF(JFreeChart chart, int width, int height, String fileName) {
        PdfWriter writer = null;

        Document document = new Document();

        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(
                    fileName));
            document.open();
            PdfContentByte contentByte = writer.getDirectContent();
            PdfTemplate template = contentByte.createTemplate(width, height);
            Graphics2D graphics2d = template.createGraphics(width, height,
                    new DefaultFontMapper());
            Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width,
                    height);

            chart.draw(graphics2d, rectangle2d);

            graphics2d.dispose();
            contentByte.addTemplate(template, 0, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        document.close();
    }

}

public class ReduceActor extends UntypedActor {
    final private Config config = ConfigFactory.load();
    private HashMap<String, KryoMessages.FlowStat> aggregatedResult = new HashMap<>();
    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof KryoMessages.JobResult){
            KryoMessages.JobResult result = (KryoMessages.JobResult) message;
            aggregatedResult.putAll(((KryoMessages.JobResult) message).getFlows());
//            PdfReport.writeChartToPDF(PdfReport.generatePieChart(result), 500, 400, config.getString("filesystem.path") + File.separator + result.getJobId() );
        }
    }
}
