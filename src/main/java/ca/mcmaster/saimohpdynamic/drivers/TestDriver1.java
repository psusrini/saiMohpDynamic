/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saimohpdynamic.drivers;
  
import static ca.mcmaster.saimohpdynamic.Constants.*;
import ca.mcmaster.saimohpdynamic.Parameters;
import static ca.mcmaster.saimohpdynamic.Parameters.*;
import ca.mcmaster.saimohpdynamic.constraint.OrderedConstraint; 
import ca.mcmaster.saimohpdynamic.constraint.Triplet;
import ca.mcmaster.saimohpdynamic.mohp.Heuristic;
import ca.mcmaster.saimohpdynamic.utilities.CplexUtils;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class TestDriver1 {
        
    protected static Logger logger;
    protected static  IloCplex cplex;
     
    
    public static TreeMap<String, Double> base_objectiveFunctionMap =null;
    public static  TreeMap<String, IloNumVar> base_mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    //constraints, smallest first
    public static  TreeMap<Integer, HashSet<OrderedConstraint>> base_mapOfAllConstraintsInTheModel = 
            new TreeMap<Integer, HashSet<OrderedConstraint>> ();
     
    static {
        logger=Logger.getLogger(TestDriver1.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+TestDriver1.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public static void init ( ) throws Exception{
        cplex = new IloCplex ();
        cplex.importModel( PRESOLVED_MIP_FILENAME);
        CplexUtils.setCplexParameters(cplex) ;
        
        base_objectiveFunctionMap = CplexUtils.getObjective(cplex);
        for ( IloNumVar var : CplexUtils.getVariables(cplex)){
            base_mapOfAllVariablesInTheModel.put (var.getName(), var);
        }
       
        List<OrderedConstraint> lbcList = CplexUtils.getConstraints(cplex,base_objectiveFunctionMap );
        
        for (OrderedConstraint lbc: lbcList){
            HashSet<OrderedConstraint> current = base_mapOfAllConstraintsInTheModel.get (lbc.getNumberOfPrimaryVariables());
            if (null==current) current = new HashSet<OrderedConstraint>();
            current.add (lbc) ;
            base_mapOfAllConstraintsInTheModel.put (lbc.getNumberOfPrimaryVariables(), current);
        }
        
        
        TreeMap<String, Boolean> fixings = new TreeMap<String, Boolean> ();                 
        fixings.put ("x5", true) ;                 
        //fixings.put ("x1", false) ;
        fixings.put ("x2", false) ;
        fixings.put ("x6", false) ;
        
        TreeMap<String, Double>  fractionalValues = new  TreeMap<String, Double>  () ;
        fractionalValues.put ("x1", 0.0);
        fractionalValues.put ("x3", 0.1);
        fractionalValues.put ("x4", 0.6);
        fractionalValues.put ("x7",0.4);
        fractionalValues.put ("x8", 1.0);
        
        System.out.println(" Branching variable recommendation is" );
        System.out.println(  (new Heuristic(fixings, fractionalValues)) .getBranchingVariable());
        
    }
       
    public static void main(String[] args) throws Exception{
        init();
        
        /*
        
            Minimize
            -70 x1 - 70 x2 - 40 x3 - 10 x4 + 30.2 x5  - 9 x7 + 10 x8

            Subject To

              c1: x1  + 2 x2 - 2 x3 + 2 x4  + 2 x5 -2 x6 -2 x7 + 9 x8 >= 5 
              c2: -x1 -x2 - x3 - x4 - x7 >= -3
              c3: -x1 -x2 - x3 - x4 - x7 >= -2
              c4: -x1 -x2 - x3 - x4 - x7 >= -1

            Binary
              x1 x2 x3 x4 x5 x6 x7 x8
            End

            TEST: fractional value used , and not used, for priority calculation

            TEST: print winner variable using MOHP heuristic

            TEST : constraint order in file        

        */
    }
}
