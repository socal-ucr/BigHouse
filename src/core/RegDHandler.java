package core;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/** 
 * This class contains the functions used in fetching the regulation-D signals.
 */
public class RegDHandler {
  /**
   * An array of file names in 'workloads/' from which to pull regulation-D signals
   * These files should contain 1800 entries (1 hour assuming interval of 2 seconds between each signal value)
   */
  private static String[] regDFiles = {"regd_midreg1","regd_midreg2","regd_highreg"};

    /**
     * An array containing the timestamps present in the regulation-D signal file.
     */
  private static ArrayList<Double> timeStamps = new ArrayList<Double>();

    /**
     * An array containing the regulation-D values present in the regulation-D signal file.
     */
  private static ArrayList<Double> regValues = new ArrayList<Double>();
  
  private static int curHour = 0;
  private static double mileage = getNewMileage(nextRegDFile());
  private static int index = 0;

    /**
     * Extracts regulation-D signals and corresponding timestamps from a file and populates
     * the respective arrays <timeStamps, regValues>
     *
     * @param fileName - the file from which the data will be extracted. NOTE: The file must be 
     * 			 present in the /workloads/ directory
     */
  public static void getRegDFromFile(String fileName) {
        if (fileName.startsWith("regd_")) { 
		BufferedReader bufReader = null;
		String line = "";
		double time = 0;

		double interval = 2.0; // interval of timestamps (2 seconds for data in 'regd_' files)
		try {
			bufReader = new BufferedReader(new FileReader("workloads/"+fileName));
			while( (line = bufReader.readLine()) != null) {
				line = line.trim();
				timeStamps.add(time);
				regValues.add(Double.parseDouble(line));
				time += interval;
			}
		} catch (Exception e) {
			System.out.println("[getRegDFromFile]");
 			e.printStackTrace();
			System.exit(0);
		}
        } else {
		String line = "";
		String csvSplitChar = ",";
		BufferedReader bufReader = null;
		
		String timeStr = "";
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			bufReader = new BufferedReader(new FileReader("workloads/" + fileName));
			while( (line = bufReader.readLine()) != null) {
				String[] getLine = line.split(csvSplitChar);
				if(getLine[1].equals("RegDTest")) { //FIXME: prone to errors if file doesnt start with "RegDTest"
					continue; // ignore this line
				}
				else {
					//getLine[0] is in sec
					timeStr = getLine[0];	
					date = sdf.parse("1970-01-01 " + timeStr);
					
					timeStamps.add((double)(date.getTime() - 28800000)/1000); // subtract for timezone diff
					regValues.add(Double.parseDouble(getLine[1]) );
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (ParseException e) {
			System.out.println("Problem reading date\n");
			System.exit(0);
		} catch (Exception e) {
			System.out.println("Problem reading regulation data from file: <" + fileName + ">!");
			e.printStackTrace();
			System.exit(0);
		}
	}
        /*
	System.out.println("[getRegDFromFile] Contents:\n");
	System.out.println(timeStamps.toString());
	System.out.println(regValues.toString());
	System.exit(0);
        */
    }
	
    /**
     * Gets the regulation-D signal corresponding to the given time
     * If no entry in map exists for the time, fetches regulation-D signal for next closest timeStamp
     *
     * @param time - the time in seconds for which we want to get the reg D signal
     *
     * @return the regD value corresponding to <time>
     */
     public static double getRegDSignal(double time) {
	if(regValues.isEmpty() || timeStamps.isEmpty()) {
		System.out.println("Error fetching regulation signal; array is empty!");
		System.exit(1);
	}
	boolean done = false;
	double diff = timeStamps.get(1) - timeStamps.get(0); // time difference between subsequent timeStamps
	double maxTime = timeStamps.get(timeStamps.size() - 1);
	double currentTime = 0.0;
	int index = 0;

	while(!done) {
		if( (currentTime > maxTime + (3*diff)) ) {
			System.out.println("Error: Unable to fetch accurate regualtion signal; maxTime exceeded!");
			System.out.println("maxTime is: " + maxTime + "\ndiff is: " + diff);
			System.exit(1);
		}
		if(currentTime > time) {
			// return reg signal corresponding to the previous timestamp
			return regValues.get(index - 1);
		}
		currentTime += diff; //increment currentTime to next timestamp
		++index;
	}
	System.out.println("Error: Unable to fetch regulation signal; while loop has exited"); // this should never be reached but just in case
	System.exit(1);
	return -1000.0;
    }

  /**
   * Returns mileage for the given time.
   * Mileage is defined as the sum of the change in regulation signal over one hour.
   * Mileage should be updated every hour.
   * @param curTime The time for which to compute mileage
   * @return The mileage for the given time.
   */
  public static double getCurrentMileage(double curTime) {
    // check if time is on the hour
    if(curTime % 3600 == 0) {
      // verify that time is not the same hour
      if(curTime / 3600 != curHour) {
        curHour = (int)curTime / 3600;
        mileage = getNewMileage(nextRegDFile());
      }
    }
    return mileage;
  }
  /**
   * Computes and returns mileage based on regulation signal data
   * Note: The regulation data should be over the course of one hour
   * @param fileName The name of the file containing regulation d signals
   * @return The new mileage
   */
  public static double getNewMileage(String fileName) {
    BufferedReader bufReader = null;
    String line = "";

    boolean initialFlag = true;
    double newMileage = 0.0;
    double absDifference = 0.0;
    double newVal = 0.0;
    double oldVal = 0.0;

    if(!fileName.startsWith("regd_")) {
      System.out.println("File used to update mileage should start with 'regd_'");
      System.exit(0);
    }

    try {
      bufReader = new BufferedReader(new FileReader("workloads/"+fileName));
      while( (line = bufReader.readLine()) != null) {
        line = line.trim();
        newVal = Double.parseDouble(line);
        if(initialFlag) { initialFlag = false;}  // don't compute mileage for inital regD value (no previous)
        else { absDifference = Math.abs(newVal - oldVal);} 
        newMileage += absDifference;
        oldVal = newVal;
      }
    } 
    catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
    return newMileage;
  }

  /** 
   * Returns a string containing the name of a file containing regulation signals
   * @return A string containing the name of a file containing regulation signals
   */
  public static String nextRegDFile() {
    if(index > regDFiles.length-1) { index = 0; } // restart at the beginning of files array
    return regDFiles[index++];
  }
}
