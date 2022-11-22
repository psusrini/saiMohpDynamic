/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saimohpdynamic;

/**
 *
 * @author tamvadss
 */
public class Parameters {
        
    //cplex config related
    public static final int  MIP_EMPHASIS = 2 ;    
    public static final int  FILE_STRATEGY= 3;  
    public static final int MAX_THREADS =  System.getProperty("os.name").toLowerCase().contains("win") ? 2 : 32;
    public static boolean USE_BARRIER_FOR_SOLVING_LP = false;
    
    public static final String PRESOLVED_MIP_FILENAME =              
            System.getProperty("os.name").toLowerCase().contains("win") ?
            /*"F:\\temporary files here recovered\\knapsackTiny.lp":*/
            
            "F:\\temporary files here recovered\\knapsackTiny.lp":
            "PBO.pre.sav";
    
    //nogood collection related
    public static final int MAX_MOMS_LEVELS = 2;
    public static final boolean USE_FRACTIONAL_VALUE_FOR_CALULATING_PRIORITY = true;
     
    //for perf variability testing  
    public static final long PERF_VARIABILITY_RANDOM_SEED = 1;
    public static final java.util.Random  PERF_VARIABILITY_RANDOM_GENERATOR =             
            new  java.util.Random  (PERF_VARIABILITY_RANDOM_SEED);   
            
    
}
