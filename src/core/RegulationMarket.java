package core;

import java.io.*;
import java.util.HashMap;

public class RegulationMarket {
   /**
    * A map storing timestamps and the cost of electricity for those times.
    * Keys: <Double> timestamps in seconds 
    * Values: <Double> Cost of electricity $/MWatt for this timestamp
    */ 
   private static HashMap<Double,Double> elecCostMap = new HashMap<Double,Double>();
   
   /**
    * A map storing timestamps and an array of values including ccp, pcp, r, performance score.
    * Keys: <Double> timestamps in seconds
    * Values: <Double[]> CCP, PCP, Performance Score, R
    */
   private static HashMap<Double,Double[]> timeRewardMap = new HashMap<Double,Double[]>();
   
   /**
    * Populates elecCostMap and timeRewardMap.
    */
   public static void importTraces() {
      HashMap[] maps = new HashMap[2];
      maps = DataImport.getTraceFromFile_RC("reward-cost.prn");
      elecCostMap = maps[0];
      timeRewardMap = maps[1];
   }

   /**
    * Computes cost of power consumption.
    * @param powerConsumption A value of power consumption in MWatts.
    * @param timeInSec A time in seconds.
    * @return The cost of consumption $/MWatt per hour. 
    */
   public static double getCostofConsumption(double powerConsumption, double timeInSec) {
      // elecCostMap keys are in hours, so get current hour given timeInSec
      double curHour = 0;
      curHour = Math.floor(timeInSec / 3600); 
      if(elecCostMap.containsKey(curHour)) {
         return powerConsumption * elecCostMap.get(curHour);
      }
      else {
         //return getCostofConsumption(powerConsumption, curHour+1); // check next highest time
         System.out.println("[Error: RegulationMarket.getCostofConsumption] No electricity cost entry found for time:"+curHour+"!");
         System.exit(0);
      }
      return -1;
   }

  /**
   * Computes the reward for a given time and regulation D signals. 
   * The time determines electricity cost which changes by the hour.
   * Reward is computed using mileage which is computed as the difference between subsequent regulation D values.
   * @param time The time in seconds.
   * @param regD The current regulation D signal.
   * @param regDprev The previous regulation D signal
   * @return The reward in dollars.
   */
  public static double getReward(double time, double regD, double regDprev) {
    Double[] rewardArr = new Double[4]; // CCP, PCP, PerfScore, R
    Double ccp, pcp, mileage, r, performanceScore, reward;
    ccp = pcp = mileage = r = performanceScore = reward = 0.0;

    // Reward = (CCP + PCP * Mileage) * R * Performance Score
    try {
      time = Math.floor(time / 3600) * 3600; // normalize time to the hour for elecCostMap
      rewardArr = timeRewardMap.get(time); // map get returns array [lmpVal, reward]
      if(rewardArr == null) {
        throw new NullPointerException("[getReward] No map entry for this time: "+time+"!");
      }      
      ccp = rewardArr[0];
      pcp = rewardArr[1];
      r = rewardArr[2];
      performanceScore = rewardArr[3];
      //mileage = regD - regDprev > 0 ? (regD - regDprev) : -1 * (regD - regDprev);
      mileage = RegDHandler.getCurrentMileage(time);
      if(mileage == 0) {
        System.out.println("NOTE: No change in regulation D signal! Mileage = 0.");
      }
      System.out.println("Reward = ("+ccp+" + ("+pcp+" * "+mileage+")) * "+r+" * "+performanceScore);
      reward = (ccp + (pcp * mileage)) * r * performanceScore;
    } 
    catch (NullPointerException e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }
    catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
    return reward;
  }
} 
