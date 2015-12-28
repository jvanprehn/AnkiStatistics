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

import indexmappers.IndexToLabelMapper;

public class NInStateChartWindow extends ApplicationFrame {
	
	public NInStateChartWindow ( String applicationTitle, String chartTitle, int[][] n_in_state, IndexToLabelMapper indexToLabelMapper)
    {
      super( applicationTitle );        
      
      CategoryDataset dataset = createDataset(n_in_state, indexToLabelMapper);
      
      final JFreeChart barChart = ChartFactory.createStackedBarChart(
    		  chartTitle, 				   						// chart title
              "Day",                  							// domain axis label
              "# cards",                    					// range axis label
              dataset,   										// data
              PlotOrientation.VERTICAL,    						// the plot orientation
              true,                        						// legend
              true,                        						// tooltips
              false                        						// urls
          );
      
      CategoryPlot plot = barChart.getCategoryPlot();
      BarRenderer barRenderer = (BarRenderer) plot.getRenderer();
      
      barRenderer.setSeriesPaint(dataset.getRowIndex("Unseen"), Color.black);
      barRenderer.setSeriesPaint(dataset.getRowIndex("Young"), new Color(119, 204, 119));
      barRenderer.setSeriesPaint(dataset.getRowIndex("Mature"), new Color(0, 119, 0));
      
      ChartPanel chartPanel = new ChartPanel( barChart );        
      chartPanel.setPreferredSize(new java.awt.Dimension( 750 , 367 ) );        
      setContentPane( chartPanel ); 
      
      pack( );        
      RefineryUtilities.centerFrameOnScreen( this );        
      setVisible( true );
    }
	
	private CategoryDataset createDataset(int[][] n_in_state, IndexToLabelMapper indexToLabelMapper)
	{	
		String[] cardTypes = new String[]{"Unseen", "Young", "Mature"};
		        
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset( );  
		
		for(int cardType = 0; cardType < n_in_state.length; cardType++) {
			for(int day = 0; day < n_in_state[cardType].length; day++) {
				dataset.addValue( n_in_state[cardType][day], cardTypes[cardType], indexToLabelMapper.mapIndexToLabel(day));
			}
		}
		
		return dataset; 
	}
}
