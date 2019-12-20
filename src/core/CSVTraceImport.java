package core;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Handles importing of trace values from a file
 */
public class CSVTraceImport {
  /**
  * Map to store trace values for a given timestamp
  * Keys: timestamps (in milliseconds) <type:long> 
  * Values: trace values <type:double>
  */
  //private static HashMap<Long,Double> TraceValues = new HashMap<Long, Double>();

  /**
  * Reads trace values from csv file; stores values and corresponding timestamps in TraceValues
  */
  public static HashMap getTraceFromFile(String fileName) {
    /**
    * Map to store trace values for a given timestamp
    * Keys: timestamps (in milliseconds) <type:long> 
    * Values: trace values <type:double>
    */
    HashMap<Long, Double> TraceValues = new HashMap<Long, Double>();

    String line;
    Double traceVal = 0.0;
    String timeStr = "";
    BufferedReader bufReader = null;

    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    try {
      bufReader = new BufferedReader(new FileReader("workloads/"+fileName));
      while( (line = bufReader.readLine()) != null) {
	String[] getLine = line.split(",");
	
	// save val/timestamp in map for later lookup
	traceVal = Double.parseDouble(getLine[1]);
	// we make use of simple data format library to convert time string to milliseconds
	timeStr = getLine[0];
	date = sdf.parse("1970-01-01 " + timeStr);
	
	TraceValues.put(date.getTime()-28800000, traceVal);
      }
      System.out.println("End of trace import");
      System.out.println("Printing map contents:");
      for(Map.Entry<Long,Double> entry : TraceValues.entrySet()) {
        System.out.println("Time: " + entry.getKey() + ", Trace Val: " + entry.getValue());
      }
      System.out.println("End of "+ TraceValues.size()+" map entries\nExiting...");
      //return TraceValues;
    }
    catch(ParseException e) {
      System.out.println("Problem reading date during trace value import\n");
    }
    catch(Exception e) {
      System.out.println("NON PARSE EXCEPTION!:");
      e.printStackTrace();
      System.exit(1);
    }
    return TraceValues;
  }

  public static HashMap getTraceFromFile_RC(String fileName) {
    HashMap TraceValues = new HashMap<Long, Double[]>();

    String line;
    Double rewardVal = 0.0;
    Double lmpVal = 0.0; // LMP realtime: electricity price ($/MWatt) per time slot
    String timeStr = "";
    BufferedReader bufReader = null;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    try {
      bufReader = new BufferedReader(new FileReader("workloads/"+fileName));
      bufReader.readLine(); // ignore first line (just has headers)
      while( (line = bufReader.readLine()) != null) {
        String[] getLine = line.split(" +"); // split strings separated by 1 or more spaces 
        rewardVal = Double.parseDouble(getLine[8]);
        lmpVal = Double.parseDouble(getLine[2]);
        timeStr = getLine[1];
        date = sdf.parse("1970-01-01 " + timeStr);
        Double[] valArray = {lmpVal,rewardVal};
        TraceValues.put(date.getTime()-28800000, valArray);
      }
      System.out.println("End of "+TraceValues.size() + " map entries)");
      //return TraceValues;
    }
    catch (ParseException e) {
      System.out.println("Problem parsing during trace_rc import\n");
    }
    catch(Exception e) {
      System.out.println("NON PARSE EXCEPTION!:");
      e.printStackTrace();
      System.exit(1);
    }
    return TraceValues;
  }
}
