package chart;
import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class NReviewsChartWindow extends ApplicationFrame {
	public NReviewsChartWindow ( String applicationTitle, String chartTitle, int[][] n_reviews)
    {
      super( applicationTitle );        
      
      CategoryDataset dataset = createDataset(n_reviews);
      
      final JFreeChart barChart = ChartFactory.createStackedBarChart(
    		  chartTitle, 				   	// chart title
              "Day",                  		// domain axis label
              "# reviews",                  // range axis label
              dataset,   					// data
              PlotOrientation.VERTICAL,    	// the plot orientation
              true,                        	// legend
              true,                        	// tooltips
              false                        	// urls
          );
      
      CategoryPlot plot = barChart.getCategoryPlot();
      BarRenderer barRenderer = (BarRenderer) plot.getRenderer();
      
      barRenderer.setSeriesPaint(dataset.getRowIndex("Learn"), new Color(0, 0, 255));
      barRenderer.setSeriesPaint(dataset.getRowIndex("Young"), new Color(119, 204, 119));
      barRenderer.setSeriesPaint(dataset.getRowIndex("Mature"), new Color(0, 119, 0));
      barRenderer.setSeriesPaint(dataset.getRowIndex("Relearn"), new Color(218, 75, 75));
      
      ChartPanel chartPanel = new ChartPanel( barChart );        
      chartPanel.setPreferredSize(new java.awt.Dimension( 750 , 367 ) );        
      setContentPane( chartPanel ); 
      
      pack( );        
      RefineryUtilities.centerFrameOnScreen( this );        
      setVisible( true );
    }
	
	private CategoryDataset createDataset(int[][] n_reviews)
	{	
		String[] cardTypes = new String[]{"Learn", "Young", "Mature", "Relearn"};
		        
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset( );  
		
		for(int cardType = 0; cardType < n_reviews.length; cardType++) {
			for(int day = 0; day < n_reviews[cardType].length; day++) {
				dataset.addValue( n_reviews[cardType][day], cardTypes[cardType], Integer.toString(day));
			}
		}
		
		return dataset; 
	}
}
