package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import ptolemy.plot.Plot;
import ptolemy.plot.PlotFrame;
import logist.plan.Action.Move;
/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
        List<Plan> plans = new ArrayList<Plan>();
        
        Solution initSol = this.getInitialSolution(vehicles, tasks);
        
        Map<Vehicle, Plan> planMap = this.computeSLS(initSol, true);
        
        for(Vehicle v : vehicles) {
        	Plan plan = planMap.get(v);
        	plans.add(plan);
        	
        	String msg = "Vehicle " + v.id() + " with capacity " + v.capacity()
        			+ " has plan:\n" + plan.toString() + "\n";
        	System.out.println(msg);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }

    private Solution getInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
    	Solution initSol = new Solution();
    	
//    	boolean first = true;
//    	for(Vehicle v : vehicles) {
//    		List<Task> vehicleTasks = new ArrayList<Task>();
//    		
//    		if(first) {
//    			vehicleTasks.addAll(tasks);
//    			first = false;
//    		}
//    		
//    		initSol.putVehicle(v, vehicleTasks);
//    	}
    	
    	Random random = new Random();
    	Map<Vehicle, List<Task>> tasksPerVehicle = new HashMap<Vehicle, List<Task>>();
    	
    	for(Vehicle v : vehicles) {
    		tasksPerVehicle.put(v, new ArrayList<Task>());
    	}
    	
    	for(Task t : tasks) {
    		List<Vehicle> admissibleVehicles = new ArrayList<Vehicle>();
    		for(Vehicle v : vehicles) {
    			if(v.capacity() >= t.weight)
    				admissibleVehicles.add(v);
    		}
    		
    		if(admissibleVehicles.isEmpty())
    			return null;
    		
    		int i = random.nextInt(admissibleVehicles.size());
    		tasksPerVehicle.get(admissibleVehicles.get(i)).add(t);
    	}
    	
    	for(Map.Entry<Vehicle, List<Task>> entry : tasksPerVehicle.entrySet()) {
    		initSol.putVehicle(entry.getKey(), entry.getValue());
    	}
    	
    	return initSol;
    }
    
    private Map<Vehicle, Plan> computeSLS(Solution initSolution, boolean showPlot) {
    	Solution A = initSolution, best = initSolution, localBest = initSolution;
    	double randomFactor = 0.;
    	int maxIter = 50000, maxStagnationIter = 10000, maxLocalStagnationIter = 20;
    	Random random = new Random();
    	int iter = 0, stagnationIter = 0, localStagnationIter = 0;
    	int pertubationSteps = 1;

    	JFrame frame;
    	Plot plot = null;
    	if(showPlot) {
    		frame = new JFrame("SLS Algorithm, Cost over iterations");
    		plot = new Plot();
    		plot.setTitle("SLS Algorithm, Cost over iterations");
    		plot.setXLabel("Iteration n°");
    		plot.setYLabel("Cost");
    		plot.addLegend(0, "Current");
    		plot.addLegend(1, "Best");
    		plot.setYLog(true);
        	frame.add(plot);
        	frame.pack();
        	frame.setVisible(true);
    	}
    	
    	while(iter < maxIter && stagnationIter < maxStagnationIter) {
    		List<Solution> neighbours;
    		
//    		if(random.nextDouble() < randomFactor) {
//    			int randomID = random.nextInt(neighbours.size());
//    			A = neighbours.get(randomID);
//    		}
//    		else
    		if(localStagnationIter >= maxLocalStagnationIter) {
    			for(int i = 0; i < pertubationSteps; i++) {
    				neighbours = A.getNeighbours();
	    			int randomID = random.nextInt(neighbours.size());
	    			A = neighbours.get(randomID);
    			}
    			
    			localStagnationIter = 0;
    			localBest = A;
    		}
    		else {
    			double bestCost = Double.POSITIVE_INFINITY;
    			List<Solution> bestSolutions = new ArrayList<Solution>();
    			
    			neighbours = A.getNeighbours();
    			
    			for(Solution s : neighbours) {
    				double cost = s.getCost();
    				
    				if(cost == bestCost) {
    					bestSolutions.add(s);
    				}
    				else if(cost < bestCost) {
    					bestSolutions.clear();
    					bestSolutions.add(s);
    					bestCost = cost;
    				}
    			}
    			
    			int id = random.nextInt(bestSolutions.size());
    			A = bestSolutions.get(id);
    		}
    		
    		if(A.getCost() < localBest.getCost()) {
    			localBest = A;
    			localStagnationIter = 0;
    		}
    		
    		if(A.getCost() < best.getCost()) {
    			best = A;
    			stagnationIter = 0;
    		}
    		
    		if(showPlot) {
	        	plot.addPoint(0, iter, A.getCost(), true);
	        	plot.addPoint(1, iter, best.getCost(), true);
	        	plot.fillPlot();
    		}
    		
    		iter++;
    		stagnationIter++;
    		localStagnationIter++;
    		
    		System.out.println("Current iter: " + iter + " local stagnation: " + localStagnationIter);
            System.out.print(A.toString() + "\n\n\n");
    	}
    	
    	if(iter == maxIter) {
    		System.out.println("Stopped because max iter reached.");
    	}
    	else {
    		System.out.println("Stopped because stagnated for too long. iter = " + iter);
    	}
    	
    	System.out.println("Final cost: " + best.getCost());

    	return best.getPlans();
    }
}
