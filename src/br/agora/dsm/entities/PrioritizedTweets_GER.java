package br.agora.dsm.entities;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import br.agora.dsm.utils.Common;

public class PrioritizedTweets_GER extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException 
	{	
		try {
			//System.out.println("Prioritization of Twitter in Germany");
			receiveTweet();			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error init - Twitter Germany - "+e.getMessage());
		}
	}
	
	public static void receiveTweet() throws TwitterException
	{

		// A builder that can be used to construct a twitter4j configuration with desired settings.
		ConfigurationBuilder cb = new ConfigurationBuilder();	
		
	    cb.setDebugEnabled(true);
	    cb.setOAuthConsumerKey("njkpEt2VY49p3HhWmaVhIG7Wh");
	    cb.setOAuthConsumerSecret("n853ektXcHy5OZZJ3PBLgstH1sqfvLG7waeQYGBvC3QM8E9v0r");
	    cb.setOAuthAccessToken("1240286418-8iu0g5ZvfJkGLtIc8ZdbGDuIc7S36FtXXm8tvCT");
	    cb.setOAuthAccessTokenSecret("tKdDKXLPIaquYpHuPSPNSzU7r6BdRJFqDnT29eLw8d2IV");
	    
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    
		StatusListener listener = new StatusListener() {
			
	        @Override
			public void onStatus(Status status) 
	        {
	        	
	        	if (status.getGeoLocation() != null)
	    		{	            	
	        		
	        		/* ****************** PERFORMANCE TRACKING ****************** */
		        	long middlSt =  System.nanoTime();
					
	        		try {
	            		
	            		/****************** VERIFY THE SMALLEST DISTANCE BETWEEN THIS TWEET AND A FLOODED AREA **********************/
		    			
		    			// connection to the database		    			
	        			Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/GermanyPrioritization", "postgres", "anta200");
	        			//Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5432/GermanyPrioritization", "postgres", "anta200");
		    			//Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5432/GermanyPrioritization", "postgres", "agora");
		    			
		    			if (conn != null)
		    			{
		    				//System.out.println("Prioritized Tweets Germany connection open!");
		    				// removing special characters of the status text to store into the database
			    			String statusText = status.getText().replaceAll("'", " ");		    			
		    				
			    			// verifying if the tweet is already within the database
			    			Statement sq_stmt0 = conn.createStatement();	
							String sql_str0 = "SELECT * FROM tweets WHERE tweet_id = '"+status.getId()+"';";
							ResultSet rs0 = sq_stmt0.executeQuery(sql_str0);
							boolean tweet_isDatabase = rs0.next();
							rs0.close();
							sq_stmt0.close();
			    			
							if (!tweet_isDatabase)
							{
								// select all current flooded areas
				    			Statement sq_stmt1 = conn.createStatement();	    			
				    			String sql_str1 = "SELECT * FROM flooded_areas WHERE flooded_final_time is NULL";		    			
				    			ResultSet rs1 = sq_stmt1.executeQuery(sql_str1);
				    			
				    			// define a big distance in order to compare and get the smallest distance between a tweet and a flooded area 
				    			double smallest_distance = 2111111111;
				    			double aux;
				    			
				    			// initialize gid
				    			String gid = "", flooded_initial_time = "";
				    			
				    			// while there are flooded areas do this loop
				    			while(rs1.next())
				    			{		    			
				    				// calculate the distance between the tweet and one flooded area 
				    				Statement sq_stmt2 = conn.createStatement();		    				
				    				String sql_str2 = "SELECT ST_DISTANCE_Sphere(ST_SetSRID(ST_MakePoint("+status.getGeoLocation().getLongitude()+","+status.getGeoLocation().getLatitude()+"), 4326), ST_TRANSFORM(area, 4326)) as distance FROM catchments WHERE gid = "+rs1.getString("catchments_gid")+";";		    				
				    				ResultSet rs2 = sq_stmt2.executeQuery(sql_str2);
				    				rs2.next();
				    				
				    				// assign the distance to a variable
				    				aux = Double.parseDouble(rs2.getString("distance"));
				    				rs2.close();
				    				sq_stmt2.close();
				    				
				    				// compare this distance with the smallest distance and if it is smaller than smallest_distance is aux
				    				if (aux < smallest_distance)
				    				{	
				    					// it is also important to store the gid of the catchment associated with the smallest distance between a flooded area and the tweet  
				    					smallest_distance = aux;
				    					gid = rs1.getString("catchments_gid");
				    					flooded_initial_time = rs1.getString("flooded_initial_time");
				    				}
				    				
				    			}
				    			
				    			
				    			// close result set and connection
				    			rs1.close();
			    				sq_stmt1.close();
			    				
				    			/****************** INSERT THE TWEET AND ITS COMPONENTS INTO THE DATABASE **********************/
				    			// if there is a flooded area, then insert every information of the tweet into the database
			    				//System.out.println("Twitter Germany - "+status.getId());
				    			if (!gid.isEmpty())
				    			{
				    				Statement sq_stmt3 = conn.createStatement();
				    				String sql_str3  = "INSERT INTO tweets (tweet_id, tweet_time, username, tweet_text, location, catchments_gid, flooded_initial_time, distance) VALUES ("+status.getId()+", '"+status.getCreatedAt()+"', '"+status.getUser().getScreenName()+"', '"+statusText+"', ST_SetSRID(ST_MakePoint("+status.getGeoLocation().getLongitude()+","+status.getGeoLocation().getLatitude()+"), 4326), '"+gid+"', '"+flooded_initial_time+"', "+smallest_distance+");";
		    						sq_stmt3.executeUpdate(sql_str3);
		    						sq_stmt3.close();
		    					}
				    			// if there is no flooded area, then insert the tweet without catchments information into the database 
				    			else
				    			{		    				
				    				Statement sq_stmt3 = conn.createStatement();
				    				String sql_str3  = "INSERT INTO tweets (tweet_id, tweet_time, username, tweet_text, location) VALUES ("+status.getId()+", '"+status.getCreatedAt()+"', '"+status.getUser().getScreenName()+"', '"+statusText+"', ST_SetSRID(ST_MakePoint("+status.getGeoLocation().getLongitude()+","+status.getGeoLocation().getLatitude()+"), 4326));";
		    						sq_stmt3.executeUpdate(sql_str3);
		    						sq_stmt3.close();
		    					}
			    			
							}
							
							conn.close();
			    			//System.out.println("Prioritized Tweets Germany connection closed!");
			    	
		    			}
		    			else
		    				System.out.println("Prioritized Tweets Germany - Connection refused!!");
    			   			
		            } catch (Exception e) {
		    			System.out.println("Error Prioritized Tweets Germany - "+e);
		            }
	        		
	        		/* ****************** PERFORMANCE TRACKING ****************** */
		            /*long middlEn = System.nanoTime();
		            
		            double x = Math.pow(10, -18);
					double a = ((Long.parseLong(String.valueOf(middlEn)) - Long.parseLong(String.valueOf(middlSt))) / x);
					
					a = a * Math.pow(10, -(Math.floor(Math.log10(a) - 18))); 
					
					SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
					String date = dt.format(Calendar.getInstance().getTime());           
					
					String line = "PrioritizationTweetsGER;" + date + ";" + middlSt + ";" + middlEn + ";" + (middlEn-middlSt) +";"+
							 a + ";" + status.getId() + ";";    
					
					Common.updateTwitterPerformanceMeasurement(line);*/
	            
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
	        	System.out.println("API Twitter Germany error = "+ex);
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