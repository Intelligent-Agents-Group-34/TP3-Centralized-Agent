package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
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
        
        Map<Vehicle, Plan> planMap = this.computeSLS(initSol);
        
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
    	
    	boolean first = true;
    	for(Vehicle v : vehicles) {
    		List<Task> vehicleTasks = new ArrayList<Task>();
    		
    		if(first) {
    			vehicleTasks.addAll(tasks);
    			first = false;
    		}
    		
    		initSol.putVehicle(v, vehicleTasks);
    	}
    	
    	return initSol;
    }
    
    private Map<Vehicle, Plan> computeSLS(Solution initSolution) {
    	Solution A = initSolution, best = initSolution;
    	double randomFactor = 0.1;
    	int maxIter = 100000, maxStagnationIter = 10000;
    	Random random = new Random();
    	int iter = 0, stagnationIter = 0;
    	JFrame frame = new JFrame();
    	Plot plot = new Plot();
    	frame.add(plot);
    	frame.pack();
    	frame.setVisible(true);
    	
    	while(iter < maxIter && stagnationIter < maxStagnationIter) {
    		List<Solution> neighbours = A.getNeighbours();
    		
    		if(random.nextDouble() < randomFactor) {
    			int randomID = random.nextInt(neighbours.size());
    			A = neighbours.get(randomID);
    		}
    		else {
    			double bestCost = Double.POSITIVE_INFINITY;
    			
    			for(Solution s : neighbours) {
    				double cost = s.getCost();
    				
    				if(cost < bestCost) {
    					A = s;
    					bestCost = cost;
    				}
    			}
    		}
    		
    		if(A.getCost() < best.getCost()) {
    			best = A;
    			stagnationIter = 0;
    		}
    		
        	plot.addPoint(0, iter, A.getCost(), true);
        	plot.addPoint(1, iter, best.getCost(), true);
        	plot.fillPlot();
    		
    		iter++;
    		stagnationIter++;
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
    
    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
}
