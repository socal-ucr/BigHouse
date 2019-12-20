package core;

import java.io.*;
import java.util.HashMap;

public class RegulationMarket {
   private static HashMap<Long,Double> elecCostMap = new HashMap<Long,Double>();
   private static HashMap<Long,Double[]> timeRewardMap = new HashMap<Long,Double[]>();
   public static void importTraces() {
      elecCostMap = CSVTraceImport.getTraceFromFile("ElectricityCostTimeTrace.csv");
   }

   public static double getCostofConsumption(double powerConsumption, long timeInMS) {
      if(elecCostMap.containsKey(timeInMS)) {
         return elecCostMap.get(timeInMS);
      }
      else {
         return getCostofConsumption(powerConsumption, timeInMS+1); // check next highest time
      }
   }

  public static double getReward(double regDSignal) {
    System.out.println("\nNOTE: Getting default reward!:");
    Long time = 0L;
    Double[] rewardArr = null;
    try {
      rewardArr = timeRewardMap.get(time); // map get returns array [lmpVal, reward]
      if(rewardArr == null) {
        throw new NullPointerException("[getReward] No map entry for this time: "+Long.toString(time)+"!");
      }
    } 
    catch (NullPointerException e) {
      System.out.println(e.getMessage());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    return rewardArr[1];
  }
} 
