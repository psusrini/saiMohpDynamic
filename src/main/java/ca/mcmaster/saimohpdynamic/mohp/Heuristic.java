/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saimohpdynamic.mohp;

import static ca.mcmaster.saimohpdynamic.Constants.*; 
import ca.mcmaster.saimohpdynamic.Parameters;
import static ca.mcmaster.saimohpdynamic.Parameters.MAX_MOMS_LEVELS;
import static ca.mcmaster.saimohpdynamic.Parameters.USE_FRACTIONAL_VALUE_FOR_CALULATING_PRIORITY;
import ca.mcmaster.saimohpdynamic.constraint.OrderedConstraint;
import ca.mcmaster.saimohpdynamic.constraint.Triplet;
import static ca.mcmaster.saimohpdynamic.drivers.TestDriver1.base_mapOfAllConstraintsInTheModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class Heuristic {
    
    //fixed variables and their values
    private TreeMap<String, Boolean> fixedVariables ;
    //free variables and their fractional values
    private TreeMap<String, Double>  freeVariables ;
    
    public Heuristic (TreeMap<String, Boolean> fixings ,TreeMap<String, Double>  freeVariables){
        this.fixedVariables=fixings;
        this. freeVariables=freeVariables;
    }
    
    public String getBranchingVariable ( ) throws Exception{
               
        //branching candidates and their score
        Map<String, Integer> refcountMap = new HashMap <String, Integer> ();
        for (Map.Entry<String, Double> entry: freeVariables.entrySet()){
            // we only branch on free variables that are fractional
            double val = entry.getValue();
            if (val >= ONE || val <= ZERO) continue;
            refcountMap.put( entry.getKey(), ZERO);
        }
        final int NUM_FRACTIONAL_VARS = refcountMap.size();
        
         
        if (NUM_FRACTIONAL_VARS>ONE) {
            
            TreeMap <Integer, List<Triplet> > priorityMap=getBranchingPriorities();
                       
            for (Map.Entry <Integer, List<Triplet> > entry : priorityMap.entrySet()){
                               
                List<Triplet>  tripletList = entry.getValue();
                final int size = tripletList.size(); 
                double currentObjectiveSum = BILLION;
                 
                for (int index = size-ONE; index>=ZERO; index --){
                    Triplet triplet = tripletList.get(index);
                    if (triplet.objectiveCoeffcient < currentObjectiveSum) {
                        //remove all but the highest refcounts and check if there is a clear winner
                        refcountMap=removeAllButHighestRefcount(refcountMap) ;
                        if (refcountMap.size()==ONE) break;
                        
                        currentObjectiveSum = triplet.objectiveCoeffcient;
                        updateRefCountMap(refcountMap, triplet.varName) ;
                    }else {
                        //update refcount map
                        updateRefCountMap(refcountMap, triplet.varName) ;
                    }
                }
                
                //remove all but the highest refcounts and check if there is a clear winner
                refcountMap=removeAllButHighestRefcount(refcountMap) ;
                if (refcountMap.size()==ONE) break;
            }            
        }
        
       
        List<String> candidateList = new ArrayList<String> ();
        candidateList.addAll(refcountMap.keySet() );
        Collections.shuffle(candidateList,  Parameters.PERF_VARIABILITY_RANDOM_GENERATOR);
        return  candidateList.get(ZERO);
    }
    
    private void updateRefCountMap(Map<String, Integer> refcountMap, String varName){
        Integer current = refcountMap.get(varName) ;
        if (current !=null) refcountMap.put(varName, ONE+current) ;
    }
    
    private Map<String, Integer>   removeAllButHighestRefcount( Map<String, Integer> refcountMap){
        Map<String, Integer> newMap = new HashMap <String, Integer> ();
        int highestKnown = -ONE;
        for (  Map.Entry<String, Integer> entry : refcountMap.entrySet()){
            if (highestKnown < entry.getValue()){
                newMap.clear();
                newMap.put (entry.getKey(), entry.getValue());
                highestKnown=entry.getValue();
            }else if (highestKnown == entry.getValue()){
                newMap.put (entry.getKey(), entry.getValue());
            }
            
        }
        return newMap;
    }
    
    //return value key is number of fixings in the infeasible hypercube
    private   TreeMap <Integer, List<Triplet> > getBranchingPriorities (  ) throws Exception{
        
        TreeMap <Integer, List<Triplet> > results = new  TreeMap <Integer, List<Triplet> > ();
        
        int maxDepth = BILLION;
        
        for ( HashSet<OrderedConstraint> constraintSet : base_mapOfAllConstraintsInTheModel.values()){
            for (OrderedConstraint lbc:  constraintSet){
                
                //get priorities from this lbc and merge into results, update maxDepth
                
                //get a copy
                OrderedConstraint  lbcCopy = lbc.getCopy(this.freeVariables);
                
                //System.out.println("Printing constraint BEFORE fixings "   + "\n " + lbcCopy) ;
                
                if (this.fixedVariables.size()>ZERO) lbcCopy.applyKnownFixings(fixedVariables);
                
                //System.out.println("Printing constraint AFTER fixings " +  "\n " + lbcCopy) ;
                      
                
                TreeMap <Integer, List<Triplet> > bp = lbcCopy.getNogoods (  maxDepth );
                               
                for (Map .Entry<Integer, List<Triplet> > entry : bp.entrySet()) {
                    //actually bp will have only 1 entry 
                    
                    //System.out.println (" Cube size : "+ entry.getKey() );
                    /*for (Triplet triple : entry.getValue()){
                        System.out.println (" Triplet : "+  triple.varName + ","
                                + triple.constraintCoefficient + ","
                                + triple.objectiveCoeffcient );
                    }*/
                    
                    
                    List<Triplet> current = results.get( entry.getKey() );
                    if (null == current) current = new ArrayList<Triplet> ();
                    current.addAll (entry.getValue()) ;
                    results.put( entry.getKey(),current );
                    
                }
                
                //System.out.println("--------------------------");
                
                if (results.size()>= MAX_MOMS_LEVELS){
                    int index = MAX_MOMS_LEVELS;
                    for (int key: results.keySet()){
                        index -- ;
                        if (index == ZERO ) {
                            maxDepth =  key;
                            break;
                        }
                    }
                    
                } else {
                    maxDepth = BILLION;
                }
                
            }
        }
        
        
        for (Map.Entry <Integer, List<Triplet> > entry :  results.entrySet() ){
            
            Collections.sort(entry.getValue());
        }
            
        /*System.out.println("\n PRINTING RESULTS \n");
        for (Map.Entry <Integer, List<Triplet> > entry :  results.entrySet() ){
            //print results
            
            System.out.println (" Cube size : "+ entry.getKey() );
            for (Triplet triple : entry.getValue()){
                System.out.println (" Triplet : "+  triple.varName + ","
                        + triple.constraintCoefficient + ","
                        + triple.objectiveCoeffcient );
            }
        }*/
        
        
        return results;    
    }
    
    
}
