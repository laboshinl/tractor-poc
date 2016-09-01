package ru.ownrobot.tractor;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;

import java.awt.List;
import java.io.File;
import java.util.*;


import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.itextpdf.awt.DefaultFontMapper;

import com.itextpdf.text.Paragraph;
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


public class ReduceActor extends UntypedActor {
    final private Config config = ConfigFactory.load();
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    //final private Integer aggregatorCount = 9;
    private HashMap<String, HashMap<Long, KryoMessages.FlowStat>> aggregatedResults = new HashMap<>();
    private HashMap <String, Integer> counter = new HashMap<>();

    private final ActorSystem system = getContext().system();
    private final Cluster cluster = Cluster.get(system);

    private final Integer aggregatorCount = getAggregatorCount();

    private Integer getAggregatorCount() {

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Integer aggregatorCount = cluster.state().members().size() * config.getInt("workers.count");
//        cluster.state().getMembers().forEach(m -> {
//            for (int i =0; i< config.getInt("workers.count"); i++) {
//                aggregatorCount + 1;
//            }
//        });

        return aggregatorCount;
    }

    private void generatePdf(HashMap<Long, KryoMessages.FlowStat> result, int width, int height, String fileName) {
        DefaultPieDataset dataSet = new DefaultPieDataset();
        HashMap<String,Integer> stat = new HashMap<>();
        //Integer flows = 0;
        result.entrySet().forEach(i -> {
            int count = stat.containsKey(i.getValue().getProtocol())?stat.get(i.getValue().getProtocol()):0;
            count ++;
            stat.put(i.getValue().getProtocol(), count);});
//        result.getFlows().entrySet().forEach(i -> i.getValue().ge);
        stat.entrySet().forEach(i -> dataSet.setValue(i.getKey(), i.getValue()));
//        dataSet.setValue("China", 19.64);
//        dataSet.setValue("India", 17.3);
//        dataSet.setValue("United States", 4.54);
//        dataSet.setValue("Indonesia", 3.4);
//        dataSet.setValue("Brazil", 2.83);
//        dataSet.setValue("Pakistan", 2.48);
//        dataSet.setValue("Bangladesh", 2.38);

        JFreeChart chart = ChartFactory.createPieChart(
                "Traffic by application types", dataSet, true, true, false);
//
//        return chart;
//    }

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
//    private void writeChartToPDF(JFreeChart chart, int width, int height, String fileName) {
        PdfWriter writer = null;

        Document document = new Document();

        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(
                    fileName));
            document.open();
            document.add(new Paragraph(String.format("%s flows", result.size())));
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
    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof KryoMessages.JobResult){
            log.error("Im here!!");
            KryoMessages.JobResult result = (KryoMessages.JobResult) message;
            HashMap<Long, KryoMessages.FlowStat> aggregatedResult = aggregatedResults.containsKey(result.getJobId()) ? aggregatedResults.get(result.getJobId()) : new HashMap<>();
            aggregatedResult.putAll(result.getFlows());
            aggregatedResults.put(result.getJobId(), aggregatedResult);
            Integer count = counter.containsKey(result.getJobId()) ? counter.get(result.getJobId()) : 0;
            count ++;
            counter.put(result.getJobId(), count);
            if (count == aggregatorCount) {
                log.error("Saving file!!!!!!!!!!!");
                generatePdf(aggregatedResult, 500, 400, config.getString("filesystem.path") + File.separator + result.getJobId() + ".pdf");
                aggregatedResults.remove(result.getJobId());
            }
        }
        else {
            log.error("Unhandled message of type {}", message.getClass());
            unhandled(message);
        }
    }
    public void preStart(){
        log.info("Reduce actor started");
    }
    public void postStop(){
        log.info("Reduce actor stopped");
    }
}

