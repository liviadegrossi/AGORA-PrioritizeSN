package br.agora.prioritizesn.entities;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import br.agora.prioritizesn.utils.Common;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class CollectTweetsGER extends HttpServlet 
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException 
	{	
		receiveTweet();
	}
	
	public static void receiveTweet()
	{

		// A builder that can be used to construct a twitter4j configuration with desired settings.
		ConfigurationBuilder cb = new ConfigurationBuilder();
		
	    cb.setDebugEnabled(true);
	    cb.setOAuthConsumerKey("jProkL6CvWN9KDDeHDwvgmxR3");
	    cb.setOAuthConsumerSecret("2LphoxXamFhXJpvP1GVC5NDrGDQ8ZvzYyzGGn9zZ5OajDbuxJF");
	    cb.setOAuthAccessToken("1240286418-q1EOqEInPfAPXQvBnstfvnMFalgNOQnb7OnWNQP");
	    cb.setOAuthAccessTokenSecret("OHIqrOjP8XnVhXW2WwYX5SBU2d2curojjRpmCH9EEDSQ3");	 
	    
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    
		StatusListener listener = new StatusListener() {
			
	        @Override
			public void onStatus(Status status) {
	        	   	
     
	            try {    				            	
	    			
	            	if (status.getGeoLocation() != null) 
	            	{	  
	            		
	            		/* ****************** PERFORMANCE TRACKING ****************** */
	    				long middlSt =  System.nanoTime();
	    				
	            		// removing special characters of the status text in order to store into the database
		    			String statusText = status.getText().replaceAll("'", " ");
		    			
		    			// connection to the database
		    			Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/Twitter", "postgres", "anta200");
		    			//Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5432/Twitter", "postgres", "anta200");
		    			//Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5432/Twitter", "postgres", "agora");
		    			
		    			//System.out.println("Tweets_temp_ger");
		    			
		    			// creating a statement
	    				Statement sq_stmt4 = conn.createStatement();
	    				String sql_str4  = "INSERT INTO tweets_temp_GER (tweet_id, tweet_time, username, tweet_text, location) VALUES ("+status.getId()+", '"+
	    						status.getCreatedAt()+"', '"+status.getUser().getScreenName()+"', '"+statusText+"', ST_SetSRID(ST_MakePoint("+
	    						status.getGeoLocation().getLongitude()+","+status.getGeoLocation().getLatitude()+"), 4326));";
						sq_stmt4.executeUpdate(sql_str4);
						sq_stmt4.close();
						conn.close();
						
						 /* ****************** PERFORMANCE TRACKING ****************** */
			           /* long middlEn = System.nanoTime();
			            
			            double x = Math.pow(10, -18);
						double a = ((Long.parseLong(String.valueOf(middlEn)) - Long.parseLong(String.valueOf(middlSt))) / x);
						
						a = a * Math.pow(10, -(Math.floor(Math.log10(a) - 18))); 
						
						SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
						String date = dt.format(Calendar.getInstance().getTime());           
						
						String line = "CollectTweetsGER;" + date + ";" + middlSt + ";" + middlEn + ";" + (middlEn-middlSt) + ";" +
								 a + ";" + status.getId() + ";";    
						
						Common.updateTwitterPerformanceMeasurement(line);*/
		    				
	    			}
	    				    			
	            } catch (Exception e) {
	    			System.out.print("Erro Twitter-CollectTweets"+e.getStackTrace());
	    		}           
	            
	        }

	        // called upon deletionNotice notices.
	        @Override
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
	            System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
	        }
	
	        // this notice will be sent each time a limited stream becomes unlimited. 
	        // If this number is high and or rapidly increasing, it is an indication that your predicate is too broad, and you should consider a predicate with higher selectivity.
	        @Override
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
	            System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
	        }
	
	        // called upon location deletion messages.
	        @Override
			public void onScrubGeo(long userId, long upToStatusId) {
	            System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
	        }
	
	        @Override
			public void onException(Exception ex) {
	        	System.out.println("Exception");
	            ex.printStackTrace();
	        }
	
	        // called when receiving stall warnings.
			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub				
			}
	    };
	
	 // calculating bounding box
    	List<double[]> rowList = new ArrayList<double[]>();
    	
    	double xmin = 4.50029, ymin = 47.191, xmax = 15.383, ymax = 54.9049;
    	
		double xmin_i, ymin_i, xmax_i, ymax_i;
		
		for (int i=0; i<5;i++)
			for (int j=0; j<5; j++)
			{
				xmin_i = xmin + (i*(xmax-xmin)/5);
				ymin_i = ymin + (j*(ymax-ymin)/5);
				xmax_i = xmin + ((i+1)*(xmax-xmin)/5);
				ymax_i = ymin + ((j+1)*(ymax-ymin)/5);
				rowList.add(new double[] {xmin_i, ymin_i});
				rowList.add(new double[] {xmax_i, ymax_i});
			}    
	    
	    double loc[][] = new double [50][2];
	    
	    for (int i = 0; i < 50; i++) {
	        loc[i][0] = rowList.get(i)[0];
	        loc[i][1] = rowList.get(i)[1];
	    }
	    
	    // creates a new FilterQuery
	    FilterQuery fq = new FilterQuery(); 
            
        // add a listener
        twitterStream.addListener(listener);        
       
        // sets locations for the filter query
        fq.locations(loc);

        // starts consuming public statuses that match one or more filter predicates
        twitterStream.filter(fq);
        
	}
    
}