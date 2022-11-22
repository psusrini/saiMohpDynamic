/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saimohpdynamic.constraint;

import static ca.mcmaster.saimohpdynamic.Constants.ONE;
import static ca.mcmaster.saimohpdynamic.Constants.ZERO;

/**
 *
 * @author tamvadss
 */
public class Triplet implements Comparable<Triplet >  {
    
    public String varName;
    public Double objectiveCoeffcient;
    public Double constraintCoefficient;    

    public Triplet(){
        
    }
            
    public Triplet (String varName,Double constraintCoefficient, Double objectiveCoeffcient) {
        this.varName = varName;
        this.constraintCoefficient =constraintCoefficient;
        this.objectiveCoeffcient =objectiveCoeffcient;

    }    
    
    public int compareTo(Triplet another) {    
        int result = ZERO;
        double val = Math.abs( another.constraintCoefficient) - Math.abs(this.constraintCoefficient) ;
        if (val > ZERO) {
            result = -ONE;
        } else if (val < ZERO){
            result = ONE;
        } else /* (ZERO==val)*/ {
            double diff =  Math.abs(another.objectiveCoeffcient) - Math.abs(this.objectiveCoeffcient) ;
            if (diff > ZERO) result =- ONE;
            if (diff < ZERO) result = ONE;
        }
         
        return result;
    }
    
}
