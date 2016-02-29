package br.agora.prioritizesn.entities;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import br.agora.prioritizesn.utils.Common;

public class PegelonlineRestAPI extends HttpServlet
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		
	@Override
	public void init() throws ServletException 
	{	
		// TODO Auto-generated method stub
		new Thread() {
			    @Override
				public void run() 
			    {
			    	try {
						StationToDB();
			    	} catch (Exception e) {
						// TODO Auto-generated catch block
						System.out.println("Error init - Pegelonline Stations - "+e.getMessage());
					}
			    }
		}.start();
		
		new Thread() {
		    @Override
			public void run() 
		    {
		    	try {
					MeasurementToDB();
		    	} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Error init - Pegelonline Measurements - "+e.getMessage());
				}
		    }
		}.start();
		
	}
	
	@Override
	public void destroy()
	{
	
		try {
			
			/************************** DATABASE CONNECTION **************************/			
			Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/GermanyPrioritization", "postgres", "anta200");
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
		
			Statement sq_stmt1 = conn.createStatement();	
			String sql_str1 = "UPDATE flooded_areas SET flooded_final_time='"+dateFormat.format(date)+"' WHERE flooded_final_time is NULL;";
			sq_stmt1.executeUpdate(sql_str1);
			sq_stmt1.close();
			conn.close();
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error destroy - Pegelonline - "+e.getMessage());
		}	
		
	}
	
	public static void CatchmentsToDB()
	{
		
		
	}
	
	public static void StationToDB()
	{			
		
		while (true) 
		{
			
			try
			{	
			
				/************************** READ JSON PAGE **************************/		
				
				Object jsonStation = Common.URLjsonToObject("http://www.pegelonline.wsv.de/webservices/rest-api/v2/stations.json");
				
				if(jsonStation != null)
				{
				
					JSONArray array = (JSONArray)jsonStation;
								
					/************************** DATABASE CONNECTION **************************/
					Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/GermanyPrioritization", "postgres", "anta200");
					
					if (conn != null)
	    			{
					
						/************************** VARIABLES INITIALIZATION **************************/
						
						int count = 0;		
						
						JSONObject station, water;
											
						long number = 0;
						
						String uuid = null, shortname = null, longname = null, agency = null, water_shortname = null, water_longname = null;
						
						Double lat = null, lon = null, km = null;
						
						/************************** FOR EACH STATION **************************/
						while (count < array.size()-1) 
						{
						
							// json station
							station = (JSONObject)array.get(count);
							
							// if json station contains uuid
							if (station.get("uuid") != null)
								uuid = station.get("uuid").toString();
							
							// verifying if station is already within the database
							Statement sq_stmt0 = conn.createStatement();	
							String sql_str0 = "SELECT * FROM stations WHERE id = '"+uuid+"'";
							ResultSet rs0 = sq_stmt0.executeQuery(sql_str0);
							boolean station_isDatabase = rs0.next();
							rs0.close();
							sq_stmt0.close();
							
							if (!station_isDatabase)
							{						
								
								// if json station contains number
								if (station.get("number") != null)
									number = Long.parseLong(station.get("number").toString());
								
								// if json station contains shortname
								if (station.get("shortname") != null)
									shortname =  station.get("shortname").toString();
								
								// if json station contains longname
								if (station.get("longname") != null)
									longname = station.get("longname").toString();
								
								// if json station contains km
								if (station.get("km") != null)
									km = Double.parseDouble(station.get("km").toString());				
								
								// if json station contains agency
								if (station.get("agency") != null)
									agency = station.get("agency").toString();
								
								// if json station contains water
								if (station.get("water") != null)
								{
									water = (JSONObject)station.get("water");
									// if json station contains water shortname
									if (station.get("shortname") != null)
								    	water_shortname = water.get("shortname").toString();
									// if json station contains water longname
									if (station.get("longname") != null)
									   water_longname = water.get("longname").toString();
								}
								
								String sql_str;
								
								// if json station contains latitude and longitude
								if (station.get("latitude") != null && station.get("longitude") != null)
								{
									// latitude and longitude
									lat = Double.parseDouble(station.get("latitude").toString());
									lon = Double.parseDouble(station.get("longitude").toString());
																
									/************************** SELECT CATCHMENT GID WHICH THIS STATION BELONGS TO **************************/
									Statement sq_stmt2 = conn.createStatement();
									String sql_str2 = "SELECT gid FROM catchments WHERE ST_WITHIN(ST_SetSRID(ST_MakePoint("+lon+","+lat+"), 4326), ST_TRANSFORM(area, 4326));";
									ResultSet rs2 = sq_stmt2.executeQuery(sql_str2);											
	
									// station is inside one catchment
									if (rs2.next()) 
										// insert into the database all fields
										sql_str = "INSERT INTO stations (id, number, shortname, longname, km, agency, spatial_data, water_shortname, water_longname, catchments_gid) VALUES ('"+uuid+"',"+number+",'"+shortname+"','"+longname+"',"+km+",'"+agency+"', ST_SetSRID(ST_MakePoint("+lon+","+lat+"), 4326),'"+water_shortname+"','"+water_longname+"',"+rs2.getString("gid")+");";
									else
										// station is not inside any catchment
										sql_str = "INSERT INTO stations (id, number, shortname, longname, km, agency, spatial_data, water_shortname, water_longname) VALUES ('"+uuid+"',"+number+",'"+shortname+"','"+longname+"',"+km+",'"+agency+"', ST_SetSRID(ST_MakePoint("+lon+","+lat+"), 4326),'"+water_shortname+"','"+water_longname+"');";
									
									// close result set and statement
									rs2.close();
									sq_stmt2.close();
																
								}
								else
									// insert the station into the database without latitude, longitude and associated catchment
									sql_str = "INSERT INTO stations (id, number, shortname, longname, km, agency, water_shortname, water_longname) VALUES ('"+uuid+"',"+number+",'"+shortname+"','"+longname+"',"+km+",'"+agency+"','"+water_shortname+"','"+water_longname+"');";
								
								// inserting station into the database
								Statement sq_stmt = conn.createStatement();	
								sq_stmt.executeUpdate(sql_str);
								sq_stmt.close();
							}			
							
							// increment so the next station can be inserted 
							count++;		
							
						}		
					
						// close connection
						conn.close();
						
						// waiting 1 hour to run again
						Thread.sleep(3600000);
	    			}
					else
	    				System.out.println("Pegelonline Stations - connection refused!"); 
				}
				else
					System.out.println("Pegelonline Stations - station null!");
				
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error Pegelonline stations - "+e);
			} 
		
			
		}		
		
	}
	
	public static void MeasurementToDB()
	{		
		
		/*************************** WHILE LOOP READ MEASUREMENT STATIONS ***************************/ 
		while (true)
		{	
			
			try 
			{
				/************************** READ JSON PAGE **************************/		
				
				Object jsonStation = Common.URLjsonToObject("http://www.pegelonline.wsv.de/webservices/rest-api/v2/stations.json");
				
				if (jsonStation != null)
				{
					JSONArray array = (JSONArray)jsonStation;
					
					Iterator <JSONObject> stations = array.listIterator();
					
					/************************** DATABASE CONNECTION **************************/
					Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/GermanyPrioritization", "postgres", "anta200");
					
					if (conn != null)
	    			{
					
						// variables initialization
						String URL_name_station, URL_encoded;
										
						/*************************** WHILE LOOP READ MEASUREMENT STATIONS ***************************/
						while(stations.hasNext())
				        {
							
							try {
								
								JSONObject sensor = stations.next();
																		
								/************************** READ JSON STATION MEASUREMENT PAGE **************************/
								// define station name						
								URL_name_station = sensor.get("shortname").toString();
								URL_encoded = URL_name_station.replaceAll(" ", "%20");
								URL_encoded = URL_encoded.replaceAll("�", "%C3%BC");
								URL_encoded = URL_encoded.replaceAll("�", "%C3%B6");
								URL_encoded = URL_encoded.replaceAll("�", "%C3%A4");
								URL_encoded = URL_encoded.replaceAll("-", "%2D");								
								
								// transform URL json page into an jsonObject
								Object objectMeasurement = Common.URLjsonToObject("http://www.pegelonline.wsv.de/webservices/rest-api/v2/stations/"+URL_encoded+"/W/currentmeasurement.json");
								
								if (objectMeasurement != null)
								{	
								
									JSONObject jsonMeasurement = (JSONObject)objectMeasurement;
																				
									// variables initialization
									String id, time_stamp, stateMnwMhw = null, stateNswHsw = null; 
									
									Double value = null;
									
									// if json measurement contains timestamp
									if (jsonMeasurement.get("timestamp") != null)
									{
										id = sensor.get("uuid").toString();
										time_stamp = jsonMeasurement.get("timestamp").toString();
									
										// if json measurement contains value
										if (jsonMeasurement.get("value") != null)
											value = Double.parseDouble(jsonMeasurement.get("value").toString());
										
										// if json measurement contains stateMnwMhw
										if (jsonMeasurement.get("stateMnwMhw") != null)
											stateMnwMhw = jsonMeasurement.get("stateMnwMhw").toString();
										
										// if json measurement contains stateNswHsw
										if (jsonMeasurement.get("stateNswHsw") != null)
											stateNswHsw = jsonMeasurement.get("stateNswHsw").toString();
										
										// verifying if measurement is already within the database
										Statement sq_stmt0 = conn.createStatement();	
										String sql_str0 = "SELECT * FROM measurements WHERE stations_id = '"+id+"' AND time_stamp = '"+time_stamp+"';";
										ResultSet rs0 = sq_stmt0.executeQuery(sql_str0);
										boolean measurement_isDatabase = rs0.next();
										rs0.close();
										sq_stmt0.close();
										
										if (!measurement_isDatabase)
										{
											
											/************************** INSERT JSON STATION MEASUREMENT VALUES INTO THE DATABASE **************************/															
											Statement sq_stmt2 = conn.createStatement();
											String sql_str2 = "INSERT INTO measurements (stations_id, shortname, time_stamp, value, stateMnwMhw, stateNswHsw) VALUES ('"+id+"','"+URL_name_station+"','"+time_stamp+"',"+value+",'"+stateMnwMhw+"','"+stateNswHsw+"');";
											sq_stmt2.executeUpdate(sql_str2);			
											sq_stmt2.close();
																			
											/************************** SEARCH FOR THE CATCHMENTS_GID OF THE STATION WHICH IS MEASURING **************************/
											Statement sq_stmt4 = conn.createStatement();
											String sql_str4 = "SELECT catchments_gid FROM stations WHERE stations.id = '"+id+"';";
											ResultSet rs4 = sq_stmt4.executeQuery(sql_str4);
											
											/************************** VERIFY IF THE STATION IS MEASURING A HIGH VALUE == FLOOD!!! **************************/
											if(rs4.next())
											{
												if (stateMnwMhw.equals("high"))
												{			
													/************************** VERIFY IF EXISTS FLOOD RELATED TO THIS STATION WITHIN THE DATABASE **************************/
													Statement sq_stmt3 = conn.createStatement();
													String sql_str3 = "SELECT * FROM flooded_areas, stations WHERE '"+id+"' = stations.id AND stations.catchments_gid = flooded_areas.catchments_gid AND flooded_areas.flooded_final_time is NULL;";
													ResultSet rs3 = sq_stmt3.executeQuery(sql_str3);
													boolean station_isFlooded = rs3.next();
													rs3.close();
													sq_stmt3.close();
													
													// if there is no current flood then insert that into the database
													if (!station_isFlooded)
													{
														// initiating the flood within a row using the catchments_gid above and the started time measured
														Statement sq_stmt5 = conn.createStatement();
														String sql_str5 = "INSERT INTO flooded_areas (catchments_gid, flooded_initial_time) VALUES ('"+rs4.getString("catchments_gid")+"', '"+time_stamp+"')";
														sq_stmt5.executeUpdate(sql_str5);
														sq_stmt5.close();
													}										
												}
												else
												{
													
													/************************** VERIFY IF THERE IS ANOTHER STATION MEASURING HIGH WITHIN THE FLOODED AREA **************************/
													Statement sq_stmt7 = conn.createStatement();
													String sql_str7 = "SELECT * FROM measurements as s1 INNER JOIN (SELECT stations_id, max(time_stamp) as ts FROM stations, measurements WHERE stations.id = measurements.stations_id AND catchments_gid = "+rs4.getString("catchments_gid")+" GROUP BY stations_id) as s2 on s1.stations_id = s2.stations_id AND s1.time_stamp = s2.ts AND s1.stateMnwMhw = 'high';";
													ResultSet rs7 = sq_stmt7.executeQuery(sql_str7);
													boolean catchment_hasAnotherStation = rs7.next();
													rs7.close();
													sq_stmt7.close();
													
													// if the flood is done by another stations
													if (!catchment_hasAnotherStation)
													{
														
														/************************** VERIFY IF EXISTS FLOOD RELATED TO THIS STATION WITHIN THE DATABASE **************************/
														Statement sq_stmt3 = conn.createStatement();
														String sql_str3 = "SELECT * FROM flooded_areas, stations WHERE '"+id+"' = stations.id AND stations.catchments_gid = flooded_areas.catchments_gid AND flooded_areas.flooded_final_time is NULL;";
														ResultSet rs3 = sq_stmt3.executeQuery(sql_str3);
														boolean station_isFlooded = rs3.next();
														rs3.close();
														sq_stmt3.close();
														
														// if there is no current flood then insert that into the database
														if (station_isFlooded)
														{
															// finalizing the flood within a row using the the finished time measured
															Statement sq_stmt5 = conn.createStatement();
															String sql_str5 = "UPDATE flooded_areas SET flooded_final_time = '"+time_stamp+"' WHERE catchments_gid = '"+rs4.getString("catchments_gid")+"' AND flooded_final_time is NULL;";
															sq_stmt5.executeUpdate(sql_str5);
															sq_stmt5.close();
														}
													}
												}
											}
											
											// close result set and statement
											rs4.close();
											sq_stmt4.close();
											
										}							
																	
									}
									
								}
								else
									System.out.println("Pegelonline Measurements - "+URL_encoded+" measurement null!");
													
							} catch (Exception e) {
								// TODO Auto-generated catch block
								System.out.println("Error Pegelonline measurements - "+e);
							} 		        
						
						}
						
						// close connection
						conn.close();
						//System.out.println("Pegelonline Measurements connection closed!");
									
						// waiting 10 minutes to run again
						Thread.sleep(600000);
						
	    			}
					else
			    		System.out.println("Pegelonline Measurements - connection refused!");
				}
				else
					System.out.println("Pegelonline Measurements - stations null!");
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error Pegelonline measurements cycle - "+e);
			} 		
			
		}
		
	}
	
}