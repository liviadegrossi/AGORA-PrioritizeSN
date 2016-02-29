package br.agora.prioritizesn.entities;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import br.agora.prioritizesn.utils.Common;

public class CemadenRestAPI extends HttpServlet
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
		    		PluviometerToDB();
		    	} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Error init - Cemaden - "+e);
				}
		    }
		}.start();
		
	}
	
	@Override
	public void destroy()
	{
	
		try {
			
			/************************** DATABASE CONNECTION **************************/			
			Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/SaoPauloPrioritization", "postgres", "anta200");
						
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();
			
			Statement sq_stmt1 = conn.createStatement();	
			String sql_str1 = "UPDATE flooded_areas SET flooded_final_time='"+dateFormat.format(date)+"' WHERE flooded_final_time is NULL;";
			sq_stmt1.executeUpdate(sql_str1);
			sq_stmt1.close();
			conn.close();
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error destroy - Cemaden - "+e.getMessage());
		} 
		
	}
	
	public static void CatchmentsToDB()
	{
		
	}
	
	public static void PluviometerToDB() throws SQLException, IOException, ClassNotFoundException
	{

		while (true)
		{
			
			try {
			
				/************************** VARIABLES INITIALIZATION **************************/
						
				String codestacao = null, cidade = null, nome = null, nivel = null, tipo = null, uf = null, dataHora = null;
				
				Double latitude = null, longitude = null, chuva = null;
				
				// defining the federal units
				//StringTokenizer siglasEstados = new StringTokenizer("AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO");
				StringTokenizer siglasEstados = new StringTokenizer("SP");
				
				//Main loop for all the Brazilian states				
				while(siglasEstados.hasMoreTokens()) 
				{
		
					// Reading json in a web page
					JSONObject jsonPluviometer = (JSONObject) Common.URLjsonToObject("http://150.163.255.240/CEMADEN/resources/parceiros/" + siglasEstados.nextToken() + "/1");
					
					if (jsonPluviometer != null)
					{
						JSONArray array = (JSONArray) jsonPluviometer.get("cemaden");
			
						Iterator <JSONObject> all_measurements = array.listIterator();
						
						/************************** DATABASE CONNECTION **************************/		
						Connection conn = Common.dbConnection("jdbc:postgresql://localhost:5433/SaoPauloPrioritization", "postgres", "anta200");
					    
						if (conn != null)
						{
							// Main loop for all the parsing the pluviomether data
							while(all_measurements.hasNext())
					        {
								
								try {
									
									JSONObject pluviometer = all_measurements.next();
																			
									if (pluviometer.get("codestacao") != null)
										codestacao = pluviometer.get("codestacao").toString();
									
									if (pluviometer.get("latitude") != null)
										latitude = Double.parseDouble(pluviometer.get("latitude").toString());
								
									if (pluviometer.get("longitude") != null)
										longitude = Double.parseDouble(pluviometer.get("longitude").toString());
									
									if (pluviometer.get("cidade") != null)
										cidade = pluviometer.get("cidade").toString();
									
									if (pluviometer.get("nome") != null)
									    nome = pluviometer.get("nome").toString();
									
									if (pluviometer.get("tipo") != null)
										tipo = pluviometer.get("tipo").toString();
									
									if (pluviometer.get("uf") != null)
										uf = pluviometer.get("uf").toString();
									
									if (pluviometer.get("chuva") != null)
										chuva = Double.parseDouble(pluviometer.get("chuva").toString());
									
									if (pluviometer.get("nivel") != null)
										nivel = pluviometer.get("nivel").toString();
															
									if (pluviometer.get("dataHora") != null)
										dataHora = pluviometer.get("dataHora").toString();
									
									// verifying if station is already within the database
									Statement sq_stmt1 = conn.createStatement();	
									String sql_str1 = "SELECT * FROM stations WHERE id = '"+codestacao+"'";
									ResultSet rs1 = sq_stmt1.executeQuery(sql_str1);
									boolean station_isDatabase = rs1.next();
									rs1.close();
									sq_stmt1.close();
								
									if (!station_isDatabase)
									{
																				
										String sql_str3;
										
										/************************** SELECT CATCHMENT GID WHICH THIS STATION BELONGS TO **************************/
										Statement sq_stmt2 = conn.createStatement();
										String sql_str2 = "SELECT gid FROM catchments WHERE ST_WITHIN(ST_SetSRID(ST_MakePoint("+longitude+","+latitude+"), 4326), ST_TRANSFORM(area, 4326));";
										ResultSet rs2 = sq_stmt2.executeQuery(sql_str2);
												
										System.out.println("Cemaden Stations - shortname begin: "+nome);
										// station is inside one catchment
										if (rs2.next())
											sql_str3 = "INSERT INTO stations (id, shortname, agency, spatial_data, water_shortname, catchments_gid) VALUES ('"+codestacao+"', '"+nome+"', '"+uf+"', ST_SetSRID(ST_MakePoint("+longitude+","+latitude+"), 4326),'"+cidade+"','"+rs2.getString("gid")+"');";
										else
											sql_str3 = "INSERT INTO stations (id, shortname, agency, spatial_data, water_shortname) VALUES ('"+codestacao+"', '"+nome+"', '"+uf+"', ST_SetSRID(ST_MakePoint("+longitude+","+latitude+"), 4326),'"+cidade+"');";						
																	
										// close result set and statement
										rs2.close();
										sq_stmt2.close();						
										
										// execute one of the sql string
										Statement sq_stmt3 = conn.createStatement();
										sq_stmt3.executeUpdate(sql_str3);
										sq_stmt3.close();
										
									}
								
									// verifying if measurement is already within the database
									Statement sq_stmt0 = conn.createStatement();	
									String sql_str0 = "SELECT * FROM measurements WHERE stations_id = '"+codestacao+"' AND time_stamp = '"+dataHora+"';";
									ResultSet rs0 = sq_stmt0.executeQuery(sql_str0);
									boolean measurement_isDatabase = rs0.next();
									rs0.close();
									sq_stmt0.close();
								
									if (!measurement_isDatabase)
									{
										
										/************************** INSERT JSON STATION MEASUREMENT VALUES INTO THE DATABASE **************************/															
										Statement sq_stmt4 = conn.createStatement();
										String sql_str4 = "INSERT INTO measurements (stations_id, shortname, time_stamp, value, stateMnwMhw) VALUES ('"+codestacao+"','"+nome+"','"+dataHora+"',"+chuva+",'"+nivel+"');";
										sq_stmt4.executeUpdate(sql_str4);			
										sq_stmt4.close();
										
										/************************** SEARCH FOR THE CATCHMENTS_GID OF THE STATION WHICH IS MEASURING **************************/
										Statement sq_stmt6 = conn.createStatement();
										String sql_str6 = "SELECT catchments_gid FROM stations WHERE stations.id = '"+codestacao+"';";
										ResultSet rs6 = sq_stmt6.executeQuery(sql_str6);
										
										/************************** VERIFY IF THE STATION IS MEASURING A HIGH VALUE == FLOOD!!! **************************/
										if(rs6.next())
										{
											
											if (chuva > 3)
											{							
												
												/************************** VERIFY IF EXISTS FLOOD RELATED TO THIS STATION WITHIN THE DATABASE **************************/
												Statement sq_stmt5 = conn.createStatement();
												String sql_str5 = "SELECT * FROM flooded_areas, stations WHERE '"+codestacao+"' = stations.id AND stations.catchments_gid = flooded_areas.catchments_gid AND flooded_areas.flooded_final_time is NULL;";
												ResultSet rs5 = sq_stmt5.executeQuery(sql_str5);
												boolean station_isFlooded = rs5.next();
												rs5.close();
												sq_stmt5.close();
												
												// if there is no current flood then insert that into the database
												if (!station_isFlooded)
												{
													// initiating the flood within a row using the catchments_gid above and the started time measured
													Statement sq_stmt7 = conn.createStatement();
													String sql_str7 = "INSERT INTO flooded_areas (catchments_gid, flooded_initial_time) VALUES ('"+rs6.getString("catchments_gid")+"', '"+dataHora+"')";
													sq_stmt7.executeUpdate(sql_str7);	
													sq_stmt7.close();
												}
												
											}
											else
											{
																									
												/************************** VERIFY IF THERE IS ANOTHER STATION MEASURING HIGH WITHIN THE CATCHMENT **************************/
												Statement sq_stmt8 = conn.createStatement();
												String sql_str8 = "SELECT * FROM measurements as s1 INNER JOIN (SELECT stations_id, max(time_stamp) as ts FROM stations, measurements WHERE stations.id = measurements.stations_id AND catchments_gid = "+rs6.getString("catchments_gid")+" GROUP BY stations_id) as s2 on s1.stations_id = s2.stations_id AND s1.time_stamp = s2.ts AND s1.value > 3;";
												ResultSet rs8 = sq_stmt8.executeQuery(sql_str8);
												boolean catchment_hasAnotherStation = rs8.next();
												rs8.close();
												sq_stmt8.close();
												
												// if the flood is done by another stations
												if (!catchment_hasAnotherStation)
												{
													
													/************************** VERIFY IF EXISTS FLOOD RELATED TO THIS STATION WITHIN THE DATABASE **************************/
													Statement sq_stmt5 = conn.createStatement();
													String sql_str5 = "SELECT * FROM flooded_areas, stations WHERE '"+codestacao+"' = stations.id AND stations.catchments_gid = flooded_areas.catchments_gid AND flooded_areas.flooded_final_time is NULL;";
													ResultSet rs5 = sq_stmt5.executeQuery(sql_str5);
													boolean station_isFlooded = rs5.next();
													rs5.close();
													sq_stmt5.close();
													
													// if there is no current flood then insert that into the database
													if (station_isFlooded)
													{
														// finalizing the flood within a row using the the finished time measured
														Statement sq_stmt7 = conn.createStatement();
														String sql_str7 = "UPDATE flooded_areas SET flooded_final_time = '"+dataHora+"' WHERE catchments_gid = '"+rs6.getString("catchments_gid")+"' AND flooded_final_time is NULL;";
														sq_stmt7.executeUpdate(sql_str7);
														sq_stmt7.close();
													}
												}
											
											}
											
										}
										
										// close result set and statement
										rs6.close();
										sq_stmt6.close();
					
									}
									
								} catch (Exception e) {
									// TODO Auto-generated catch block
									System.out.println("Error Cemaden measurement - "+e);
								} 
								
							}
					
							// close connection
							conn.close();
						}
						else
							System.out.println("Cemaden - connection refused!");
					}
					else
						System.out.println("Cemaden - pluviometer null!");
				}	
				
				// waiting 10 minutes to run again
				Thread.sleep(600000);		
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error Cemaden cycle - "+e);
			} 
			
		}
		
	}
}