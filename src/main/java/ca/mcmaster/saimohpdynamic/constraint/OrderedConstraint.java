/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saimohpdynamic.constraint;

import static ca.mcmaster.saimohpdynamic.Constants.*; 
import ca.mcmaster.saimohpdynamic.Parameters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class OrderedConstraint {
    
    public String constraint_Name ;
    
    //Variables and their coefficients in this constraint
    //We use a sorted map because these maps are looked up by key very often
    //   
        
    //Vars that reduce the objective (or leave it unchanged) when they 
    //move towards making the constraint infeasible.
    //
    private TreeMap <String, Double > primaryVarMap = new TreeMap <String, Double >();
    //and vars that are not primary
    private TreeMap <String, Double > secondaryVarMap = new TreeMap <String, Double >();
    
    //Primary vars are also maintained in a sorted list.
    //
    //List is sorted on the coefficient magnitude in the constraint.
    //The secondary sort criteria is the absolute value of objective coefficient
    //    
    // Note , java maps not used because of the DOUBLE key
    //
    private List <Triplet> coefficientList =   new   ArrayList <Triplet>  ();
    private TreeMap < String , Integer> indexInto_coefficientList = 
            new         TreeMap < String , Integer>();
    
    private double lhs_LargestPossibleValue = ZERO;
    private double lhs_SmallestPossibleValue = ZERO;
     
    
    private double lowerBound ;
    
    public OrderedConstraint (double lowerBound , String name) {
        this.lowerBound = lowerBound;
        constraint_Name = name;
    }
   
    public int getNumberOfPrimaryVariables (){
        return this.primaryVarMap.size();
    }
    
    public void add (Triplet triplet ) {
       
        boolean cond1= (triplet.objectiveCoeffcient > ZERO && triplet.constraintCoefficient > ZERO);
        boolean cond2= (triplet.objectiveCoeffcient < ZERO && triplet.constraintCoefficient < ZERO);
        boolean cond3 = (triplet.objectiveCoeffcient==ZERO);
        if (cond1|| cond2 || cond3){
                    
            primaryVarMap.put (triplet.varName,  triplet.constraintCoefficient) ;
             
            this.indexInto_coefficientList.put (triplet.varName, coefficientList.size() );
            this.coefficientList.add (triplet) ;
            
            if (triplet.constraintCoefficient>ZERO) {
                this.lhs_LargestPossibleValue += triplet.constraintCoefficient;
            }else {
                this.lhs_SmallestPossibleValue += triplet.constraintCoefficient;
            }
            
        }else {
            secondaryVarMap.put (triplet.varName,  triplet.constraintCoefficient) ;
            if (triplet.constraintCoefficient>ZERO)  lowerBound-= triplet.constraintCoefficient;
             
        }
        
    }
         
    // copy this constraint into another
    //
    // used by every node in the cplex search tree to get its own copy of every constraint
    //
    // free variables and their fractional values can be used to calculate priority, which is
    //held in the objective coeffecient field
    public OrderedConstraint getCopy (  TreeMap<String, Double>  freeVariables ) throws Exception{
        OrderedConstraint twin = new OrderedConstraint(lowerBound, this.constraint_Name) ;
        twin.lhs_LargestPossibleValue = this.lhs_LargestPossibleValue;
        twin.lhs_SmallestPossibleValue =lhs_SmallestPossibleValue;
        
        twin.primaryVarMap.putAll(primaryVarMap);
        twin.secondaryVarMap.putAll(secondaryVarMap);
        
        if (Parameters.USE_FRACTIONAL_VALUE_FOR_CALULATING_PRIORITY){
            for (Triplet trplet : coefficientList){
                Triplet newTriplet = new Triplet () ;
                newTriplet.constraintCoefficient = trplet.constraintCoefficient;
                newTriplet.varName =trplet.varName;
                Double frac = freeVariables.get (newTriplet.varName);
                if (null!=frac)  {
                    newTriplet.objectiveCoeffcient = trplet.objectiveCoeffcient *
                            ( trplet.constraintCoefficient>ZERO ? frac: ONE-frac) ;
                } else {
                    //this variable will be fixed later because it is not free, 
                    //so set any value for the objectiveCoeffcient
                    newTriplet.objectiveCoeffcient =  trplet.objectiveCoeffcient;
                }
                
                twin.coefficientList.add (newTriplet);
            }
        } else {
            twin.coefficientList.addAll(coefficientList);
        }
        
        twin.indexInto_coefficientList.putAll(indexInto_coefficientList);
   
        return twin;
    }
    
    //this method destroys the index into the coeff list. The index is never 
    // used  after using this method.
    //
    //argument is variables that are already fixed to either 0 or 1
    public void applyKnownFixings (TreeMap<String, Boolean> fixings ) {
        
        //primary vars
        TreeSet<Integer> listPositions = new TreeSet<Integer> ( );
        for (Map.Entry<String, Integer> entry: this.indexInto_coefficientList.entrySet()){
            String varName = entry.getKey();
            Boolean value = fixings.get(varName);
            if (null!=value){
                //
                listPositions.add ( entry.getValue());
                //
                Double constraintCoeff = this.primaryVarMap.remove(varName );
                 
                if (value)  this.lowerBound -= constraintCoeff;
                if  (constraintCoeff>ZERO ) {
                    this.lhs_LargestPossibleValue -= constraintCoeff;
                }else {
                    this.lhs_SmallestPossibleValue -= constraintCoeff;
                }
            }
        }
        //remove the list element at the largest index first
        for (int position: listPositions.descendingSet()){
            this.coefficientList.remove(position);
        }
        
        //secondary vars
        Set<String> fixedSecondaryVars = new HashSet <String> ( );
        for (Map .Entry <String, Double > entry: this.secondaryVarMap.entrySet()){
            String varName = entry.getKey();
            double constraintCoeff = entry.getValue();
            Boolean value = fixings.get(varName);
            if (null!=value){
                fixedSecondaryVars.add (varName);
                  
                if ((constraintCoeff<ZERO && value) || 
                        (constraintCoeff>ZERO && !value) )
                        lowerBound += Math.abs (constraintCoeff);
            }
        }
        for (String var: fixedSecondaryVars){
            this.secondaryVarMap.remove(var);
        }
        
    }
   
   
    //return type key is number of variable fixes in the nogood
    //List is sorted on sum of objective coeff magnitudes in the nogood
    //List element has variable name, 0 for constraint coeff, and the sum of obj coeff magnitudes
    //
    // Important to note that we do not translate the constraint into 
    //nogoods and then count variable frequencies in these nogoods. We directly
    // count the variable frequency without physically preparing the nogood
    public TreeMap <Integer, List<Triplet> > getNogoods ( int MAX_DEPTH ){
        
        TreeMap <Integer, List<Triplet> > result = new TreeMap <Integer, List<Triplet> > ();
                   
        if (lhs_SmallestPossibleValue>= lowerBound){
            //already feasible, no chance of getting any infeasible cubes
        } else {
            //sort
            Collections.sort (this.coefficientList );
            
            double slack = this.lhs_LargestPossibleValue- this.lowerBound;
            int numberOfVarFixes  = ZERO ;
            double objCoeffMagnitude=ZERO;
            double constraintCoeffMagnitude=ZERO;
            double sumOfObjCoeffMagnitudes = ZERO;
            final int coefficientListSize = coefficientList.size();
            List<Triplet> candidates = new ArrayList<Triplet> ();
            Triplet element = null;
            
            while (slack >= ZERO){
                //fix last element of sorted list
                numberOfVarFixes++ ;
                if (numberOfVarFixes > MAX_DEPTH) break;
                element = this.coefficientList.remove( coefficientListSize-numberOfVarFixes);
                candidates.add (element );
                objCoeffMagnitude=Math.abs (element.objectiveCoeffcient);
                sumOfObjCoeffMagnitudes +=  objCoeffMagnitude;
                constraintCoeffMagnitude=Math.abs (element.constraintCoefficient);
                slack -=  constraintCoeffMagnitude ;
            }
            
            if (numberOfVarFixes <= MAX_DEPTH){
                
                for (Triplet  elt :candidates ){
                    convertToPrioritListElement(elt, sumOfObjCoeffMagnitudes);                 
                }

                //continue traversing coeff list until the coeff is the same as last element collected
                while (coefficientList.size() > ZERO ){
                    element = this.coefficientList.remove( coefficientList.size()  - ONE ) ;
                    if (Math.abs (element.constraintCoefficient)==constraintCoeffMagnitude){
                        double thisOobjCoeffMagnitude=Math.abs (element.objectiveCoeffcient);
                        convertToPrioritListElement( element, 
                                sumOfObjCoeffMagnitudes - objCoeffMagnitude +thisOobjCoeffMagnitude);  
                        candidates.add (element );
                    } else break;
                }

                result.put (numberOfVarFixes, candidates) ; 
            }
                   
        }
        
         
        return result;
    }
     
    public String toString (){
        String result  =" Constarint " + this.constraint_Name + " LowerBOund is "+ lowerBound + 
                " and max possible LHS is " + this.lhs_LargestPossibleValue
                +" and min possible LHS is " + this.lhs_SmallestPossibleValue;
        
        result+= "\n  Primary map\n";
        for ( Map .Entry<String, Double >  entry :primaryVarMap.entrySet()){
             result+= (entry.getValue() + "*"+ entry.getKey()+" ") ;
        }
        
        result+= "\n Secondary map\n";           
        for ( Map .Entry<String, Double >  entry :secondaryVarMap.entrySet()){
             result+= (entry.getValue() + "*"+ entry.getKey()+" ") ;
        }
        result+= "\n Coefficient List\n";
        
        for ( Triplet triplet: this.coefficientList ){
            result+= "(" + triplet.varName +","+ triplet.constraintCoefficient 
                    + "," + triplet.objectiveCoeffcient +")" +"  \n";
        }        
    
        return result;
    }
    
    private void convertToPrioritListElement(Triplet triplet, double sumOfObjCoeffMagnitudes){
        triplet.objectiveCoeffcient = sumOfObjCoeffMagnitudes;
        triplet.constraintCoefficient = DOUBLE_ZERO;
        
    }
     
}

