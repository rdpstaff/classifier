/*
 * Copyright (C) 2014 wangqion
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package edu.msu.cme.rdp.classifier.train.validation.distance;

import java.awt.BasicStroke;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author wangqion
 */
public class BoxPlotUtils {
    
    public static void readData(String inFile, File outdir, String xAxisLabel, String yAxisLabel) throws IOException{
        XYSeriesCollection dataset = new XYSeriesCollection();
        DefaultBoxAndWhiskerCategoryDataset scatterDataset = new DefaultBoxAndWhiskerCategoryDataset();

        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        String line = reader.readLine();
        
        while ( (line = reader.readLine()) != null){
            String[] values = line.split("\\t");
            XYSeries series = new XYSeries(values[2]);
            dataset.addSeries(series);
                
            double average = Double.parseDouble(values[4]);
            int Q1 = Integer.parseInt(values[6]);;
            int median = Integer.parseInt(values[7]);
            int Q3 = Integer.parseInt(values[8]);
            int pct_98 = Integer.parseInt(values[9]);
            int pct_2 = Integer.parseInt(values[10]);
            int minOutlier = 0;  // we don't care about the outliers
            int maxOutlier = 0;  //
            
            BoxAndWhiskerItem item = new BoxAndWhiskerItem(average, median, Q1, Q3, pct_2, pct_98,  minOutlier,  maxOutlier, new ArrayList());
            scatterDataset.add(item, values[2], "");
        }
        
 
        String title = new File(inFile).getName();
        int index = title.indexOf(".");
        if ( index != -1){
            title = title.substring(0, index);
        }
          
        Font lableFont = new Font("Helvetica", Font.BOLD, 30);
        createBoxplot(scatterDataset, new PrintStream(new File(outdir, title + "_boxchart.png")), title, xAxisLabel, yAxisLabel, lableFont);
       
    }
    
    public static void createBoxplot(DefaultBoxAndWhiskerCategoryDataset scatterDataset, PrintStream outStream, 
            String title, String xAxisLabel, String yAxisLabel, Font lableFont ) throws IOException{
       
        CategoryAxis xAxis = new CategoryAxis(xAxisLabel);
        xAxis.setLabelFont(lableFont);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setTickLabelFont(lableFont);
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setRange(0, 100);
        yAxis.setLabelFont(lableFont);
        
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(true);
        renderer.setBaseLegendTextFont(lableFont);
        renderer.setStroke(new BasicStroke( 5.0f ));
        
        CategoryPlot plot = new CategoryPlot(scatterDataset, xAxis, yAxis, renderer);
        JFreeChart boxchart = new JFreeChart(title, new Font("Helvetica", Font.BOLD, 40), plot, true);

        // higher scale factor gives higher resolution
        ChartUtilities.writeScaledChartAsPNG(outStream, boxchart, 800, 1000, 3, 3);
    }
      
    public static void main(String[] args) throws IOException{
        String usage = "data.txt outdir xAxisLabel yAxisLabel";
        if ( args.length != 4){
            System.err.println(usage);
            return;
        }
        readData(args[0], new File(args[1]), args[2], args[3]);
    }
    
}
