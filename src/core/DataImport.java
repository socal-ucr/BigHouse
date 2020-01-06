package core;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Handles importing of trace values from a file
 */
public class DataImport {
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

  /**
   * Reads market regulation metrics from files formatted similarly to "BigHouse/workloads/reward-cost.prn"
   * File data must be in columns separated by one or more spaces
   * Column data should be as follows:
   * 	datetime_beginning_ept, LMP Realtime, CCP, PCP, Mileage, Perf Score, R, Reward, Pavg
   * Data is stored in HashMaps using timestamps as keys
   * @param fileName The name of the file in "BigHouse/workloads/" from which to read.
   * @return Returns an array of 2 HashMaps: first is map for electricity cost, second is map containing metrics used in reward computation
   */
  public static HashMap[] getTraceFromFile_RC(String fileName) {
    HashMap rewardTrace = new HashMap<Double, Double[]>();
    HashMap elecCostTrace = new HashMap<Double, Double>();
    HashMap[] returnArr = new HashMap[2];

    int count = 0; 
    String line;
    Double rewardVal = 0.0;
    Double lmpVal = 0.0; // LMP realtime: electricity price ($/MWatt) per time slot
    String timeStr = "";
    BufferedReader bufReader = null;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm");

    try {
      bufReader = new BufferedReader(new FileReader("workloads/"+fileName));
      bufReader.readLine(); // ignore first line (for table headers)
      while( (line = bufReader.readLine()) != null && count < 24) {
        String[] getLine = line.split(" +"); // split strings separated by 1 or more spaces 
        /*for(int i = 0; i < getLine.length; ++i) {
		System.out.println("getLine["+i+"]:"+getLine[i]);
	}*/
	//System.exit(0);
	rewardVal = Double.parseDouble(getLine[9]);
        lmpVal = Double.parseDouble(getLine[3]);
        timeStr = getLine[2];
        date = sdf.parse("1970-01-01 " + timeStr);
        Double ccp, pcp, r, perfScore;
        ccp = Double.parseDouble(getLine[4]);
        pcp = Double.parseDouble(getLine[5]);
        //System.out.println("From CSV, pcp: "+pcp);
        r = Double.parseDouble(getLine[8]);
        perfScore = Double.parseDouble(getLine[7]);
        Double[] valArray = {ccp, pcp, r, perfScore};
        //System.out.println("valArray: " + Arrays.toString(valArray));
        //System.exit(0);
        rewardTrace.put((double)(date.getTime()-28800000)/1000, valArray);
        elecCostTrace.put((double)(date.getTime()-28800000)/1000, lmpVal);
        ++count;
      }
      //System.out.println("End of "+rewardTrace.size() + " map entries)");
      //System.out.println(elecCostTrace.toString());
      //System.exit(0);
      //return TraceValues;
    }
    catch (ParseException e) {
      System.out.println("Problem parsing during trace_rc import\n");
      System.exit(0);
    }
    catch(Exception e) {
      System.out.println("NON PARSE EXCEPTION!:");
      e.printStackTrace();
      System.exit(1);
    }
    returnArr[0] = elecCostTrace;
    returnArr[1] = rewardTrace;
    return returnArr;
  }

  /**
   * Imports power consumption data for a server.
   * Note: It is assumed that power consumption data has units of watts.
   * @param fileName The name of the file in "workloads/" from which to import data
   * @return A map with keys: utilization values and values: power consumption in MWatts
   */
  public static TreeMap importPowerConsumptionData(String fileName) {
    TreeMap<Double,Double> powerConsumptionMap = new TreeMap<Double,Double>();
    BufferedReader bufferedReader = null;
    String line = null;
    String[] lineTokens = null;
    int target = 6; // which line to save from file; this saves power for 2.0GHz from "workloads/AvgPower.csv"
    int lineNum = 0;
    double[] utilArray = null;
    double scaleVal = 0.0;
    try {
      bufferedReader = new BufferedReader(new FileReader("workloads/"+fileName));
      
      while((line = bufferedReader.readLine()) != null) {
        if(lineNum == 0) {
          // save data headers as the util val
          lineTokens = line.split(",");
          utilArray = new double[lineTokens.length];
          for (int i = 1, j = 0; i < lineTokens.length; ++i,++j) {
            lineTokens[i] = lineTokens[i].trim();
            utilArray[j] = Double.parseDouble(lineTokens[i]);
            //System.out.println("Util["+j+"]:"+utilArray[j]);
          }
          //System.exit(0);
        }

        // if we are not at correct row, then skip
        if(lineNum != target) {
          ++lineNum;
          continue;
        }
        lineTokens = line.split(",");
        
        // scaleVal is the val by which to nomalize scale; we want scale to be 0-1
        // we use an index of lineTokens.length-2 because there is an extra element in the row for the data header 
        scaleVal = utilArray[lineTokens.length-2];
        
        // place values for correct row with util values scaled from 0-1
        // note this loop may not use all values in util array; we only put values in map for the amount of values in the current row 
        for (int i = 1; i < lineTokens.length; ++i) { // i start at 1 because first column contains labels
          lineTokens[i] = lineTokens[i].trim();
          //System.out.println("Util["+(i-1)+"]:"+(utilArray[i-1]/scaleVal));
          powerConsumptionMap.put(utilArray[i-1] / scaleVal, Double.parseDouble(lineTokens[i]) / 1000000); // place in map, converting consumption values from watts to MWatts
        }
          
        break;
      }
    }
    catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
    //System.out.println("Power consumption map complete. Printing:\n"+powerConsumptionMap.toString());
    //System.exit(0);
    return powerConsumptionMap;
  }
}
