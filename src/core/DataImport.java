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
	rewardVal = Double.parseDouble(getLine[9]);
        lmpVal = Double.parseDouble(getLine[3]);
        timeStr = getLine[2];
        date = sdf.parse("1970-01-01 " + timeStr);
        Double ccp, pcp, r, perfScore;
        ccp = Double.parseDouble(getLine[4]);
        pcp = Double.parseDouble(getLine[5]);
        r = Double.parseDouble(getLine[8]);
        perfScore = Double.parseDouble(getLine[7]);
        Double[] valArray = {ccp, pcp, r, perfScore};
        rewardTrace.put((double)(date.getTime()-28800000)/1000, valArray);
        elecCostTrace.put((double)(date.getTime()-28800000)/1000, lmpVal);
        ++count;
      }
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
   * Note: It is assumed that power consumption data found in the file has units of watts.
   * @param fileName The name of the file in "workloads/" from which to import data
   * @return A map with keys: utilization values and values: power consumption in MWatts
   */
  public static TreeMap importPowerConsumptionData(String fileName) {
    TreeMap<Double,Double> powerConsumptionMap = new TreeMap<Double,Double>();
    BufferedReader bufferedReader = null;
    String line = null;
    String[] lineTokens = null;
    int lineNum = 0;
    double[] utilArray = null;
    double scaleVal = 0.0;
    
    int target = 6; // which line to save from file; this saves power data for 2.0GHz server from "workloads/AvgPower.csv"
    
    try {
      bufferedReader = new BufferedReader(new FileReader("workloads/"+fileName));
      
      while((line = bufferedReader.readLine()) != null) {
        if(lineNum == 0) {
          // save data headers as the util values
          lineTokens = line.split(",");
          utilArray = new double[lineTokens.length];
          for (int i = 1, j = 0; i < lineTokens.length; ++i,++j) {
            lineTokens[i] = lineTokens[i].trim();
            utilArray[j] = Double.parseDouble(lineTokens[i]);
          }
        }

        // if we are not at correct row, then skip
        if(lineNum != target) {
          ++lineNum;
          continue;
        }
        lineTokens = line.split(",");
        
        // scaleVal is the value by which to nomalize the scale; we want scale to be 0-1 so scaleVal is the maximum util value for a given row
        // we use an index of lineTokens.length-2 because there is an extra element in the row (the row's header)
        scaleVal = utilArray[lineTokens.length-2];
        
        // place values for correct row with util values scaled from 0-1
        // note this loop may not use all values in util array; we only put values in map for the amount of values in the current row 
        for (int i = 1; i < lineTokens.length; ++i) { // i start at 1 because first column contains labels
          lineTokens[i] = lineTokens[i].trim();
          powerConsumptionMap.put(utilArray[i-1] / scaleVal, Double.parseDouble(lineTokens[i]) / 1000000); // place in map, converting consumption values from watts to MWatts
        }
          
        break;
      }
    }
    catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
    return powerConsumptionMap;
  }
}
