package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class Solution {
	private Map<Vehicle, TaskList> tasksPerVehicle;
	
	public Solution() {
		this.tasksPerVehicle = new HashMap<Vehicle, TaskList>();
	}
	
	// Deep copy
	public Solution(Solution solution) {
		this();
		for(Map.Entry<Vehicle, TaskList> entry : solution.tasksPerVehicle.entrySet()) {
			Vehicle v = entry.getKey();
			TaskList taskList = new TaskList(entry.getValue());
			
			this.tasksPerVehicle.put(v, taskList);
		}
	}
	
	// Set the tasks assigned to a vehicle.
	public void putVehicle(Vehicle vehicle, List<Task> tasks) {
		List<OrderedTask> taskList = new ArrayList<OrderedTask>();
		
		for (int i = 0; i < tasks.size(); i++) {
			taskList.add(new OrderedTask(tasks.get(i), 2*i, 2*i + 1));
		}
		
		this.tasksPerVehicle.put(vehicle, new TaskList(taskList));
	}
	
	// Remove the idx-th task of vehicle 1 and give it to vehicle 2.
	private boolean swapTaskVehicle(Vehicle v1, Vehicle v2, int idx) {		
		TaskList taskList = tasksPerVehicle.get(v1);
		
		Task task = taskList.removeTask(idx);
		
		TaskList taskList2 = tasksPerVehicle.get(v2);
		
		taskList2.addTask(task);
		
		return task.weight <= v2.capacity();
	}
	
	// Get all solutions neighbour of this one.
	public List<Solution> getNeighbours() {
		List<Solution> neighbours = new ArrayList<Solution>();
		
		Random random = new Random();
		List<Vehicle> nonEmptyVehicles = new ArrayList<Vehicle>();
		
		for(Vehicle v : tasksPerVehicle.keySet()) {
			if(!tasksPerVehicle.get(v).tasks.isEmpty()) {
				nonEmptyVehicles.add(v);
			}
		}
		
		int id = random.nextInt(nonEmptyVehicles.size());
		Vehicle vehicle = nonEmptyVehicles.get(id);
		
		neighbours.addAll(this.getPermutatedActionNeighbours(vehicle));
		neighbours.addAll(this.getPermutatedVehicleNeighbours(vehicle));
		
		return neighbours;
	}
	
	// Get all possible neighbours which are the result of permuting two actions of
	// one vehicle.
	private List<Solution> getPermutatedActionNeighbours(Vehicle vehicle) {
		List<Solution> neighbours = new ArrayList<Solution>();
		Solution neighbour;
		
		TaskList taskList = tasksPerVehicle.get(vehicle);
		
		List<TaskList> permutatedTaskLists = taskList.getAllPermutations(vehicle.capacity());
		for(TaskList pTaskList : permutatedTaskLists) {
			neighbour = new Solution(this);
			neighbour.tasksPerVehicle.put(vehicle, pTaskList);
			
			neighbours.add(neighbour);
		}
		
		return neighbours;
	}
	
	// Get all neighbours which are the result of changing the vehicle attributed to
	// one task.
	private List<Solution> getPermutatedVehicleNeighbours(Vehicle v1) {
		List<Solution> neighbours = new ArrayList<Solution>();
		Solution neighbour;
		
		TaskList taskList = tasksPerVehicle.get(v1);
		
		for(int i = 0; i < taskList.tasks.size(); i++) {
			for(Vehicle v2 : tasksPerVehicle.keySet()) {
				if(v1 == v2)
					continue;
				
				neighbour = new Solution(this);
				
				if(neighbour.swapTaskVehicle(v1, v2, i))
					neighbours.add(neighbour);
			}
		}
		
		return neighbours;
	}
	
	// Get all solutions neighbour of this one.
	public List<Solution> getAllNeighbours() {
		List<Solution> neighbours = new ArrayList<Solution>();
		
		neighbours.addAll(this.getAllPermutatedActionNeighbours());
		neighbours.addAll(this.getAllPermutatedVehicleNeighbours());
		
		return neighbours;
	}
	
	// Get all possible neighbours which are the result of permuting two actions of
	// one vehicle.
	private List<Solution> getAllPermutatedActionNeighbours() {
		List<Solution> neighbours = new ArrayList<Solution>();
		Solution neighbour;
		
		for(Map.Entry<Vehicle, TaskList> entry : tasksPerVehicle.entrySet()) {
			Vehicle v = entry.getKey();
			TaskList taskList = entry.getValue();
			
			List<TaskList> permutatedTaskLists = taskList.getAllPermutations(v.capacity());
			for(TaskList pTaskList : permutatedTaskLists) {
				neighbour = new Solution(this);
				neighbour.tasksPerVehicle.put(v, pTaskList);
				
				neighbours.add(neighbour);
			}
		}
		
		return neighbours;
	}
	
	// Get all neighbours which are the result of changing the vehicle attributed to
	// one task.
	private List<Solution> getAllPermutatedVehicleNeighbours() {
		List<Solution> neighbours = new ArrayList<Solution>();
		Solution neighbour;
		
		for(Map.Entry<Vehicle, TaskList> entry : tasksPerVehicle.entrySet()) {
			Vehicle v1 = entry.getKey();
			TaskList taskList = entry.getValue();
			
			for(int i = 0; i < taskList.tasks.size(); i++) {
				for(Vehicle v2 : tasksPerVehicle.keySet()) {
					if(v1 == v2)
						continue;
					
					neighbour = new Solution(this);
					
					if(neighbour.swapTaskVehicle(v1, v2, i))
						neighbours.add(neighbour);
				}
			}
		}
		
		return neighbours;
	}
	
	// Return the cost of this solution.
	public double getCost() {
		double cost = 0;
		
		for(Map.Entry<Vehicle, TaskList> entry : tasksPerVehicle.entrySet()) {
			Vehicle v = entry.getKey();
			TaskList taskList = entry.getValue();
			
//			cost = Math.max(cost, v.costPerKm()*taskList.getDistance(v.getCurrentCity()));
			cost += v.costPerKm()*taskList.getDistance(v.getCurrentCity());
		}
		
		return cost;
	}
	
	// Return the plan of each vehicle corresponding to this solution.
	public Map<Vehicle, Plan> getPlans() {
		Map<Vehicle, Plan> plans = new HashMap<Vehicle, Plan>();
		
		for(Map.Entry<Vehicle, TaskList> entry : tasksPerVehicle.entrySet()) {
			Vehicle v = entry.getKey();
			TaskList taskList = entry.getValue();
			
			Plan plan = taskList.getPlan(v.getCurrentCity());
			plans.put(v, plan);
		}
		
		return plans;
	}
	
	public String toString() {
		String msg = "Total cost : " + this.getCost() + "\n";
		
		for(Map.Entry<Vehicle, TaskList> entry : tasksPerVehicle.entrySet()) {
			Vehicle v = entry.getKey();
			TaskList taskList = entry.getValue();
			msg += "Vehicle " + v.id() + " : ";
			for(TaskAction a : taskList.actions) {
				msg += a.isPickUp ? "Pickup " : "Deliver ";
				msg += a.task.task.id + ", ";
			}
			msg += "\n";
		}
		
		return msg;
	}
	
	
	
	// Class representing a list of tasks and the order in which they are picked up
	// and delivered.
	private class TaskList {
		public List<OrderedTask> tasks;
		public List<TaskAction> actions;
		
		public TaskList() {
			this.tasks = new ArrayList<OrderedTask>();
			this.actions = new ArrayList<TaskAction>();
		}
		
		public TaskList(List<OrderedTask> tasks) {
			this.tasks = new ArrayList<OrderedTask>(tasks);
			this.actions = new ArrayList<TaskAction>();
			for(int i = 0; i < 2*tasks.size(); i++)
				this.actions.add(null);
			
			for(OrderedTask t : tasks) {
				TaskAction pickUp = new TaskAction(t, true);
				TaskAction delivery = new TaskAction(t, false);
				
				this.actions.set(t.pickUpOrder, pickUp);
				this.actions.set(t.deliverOrder, delivery);
			}
		}
		
		// Create a deep copy
		public TaskList(TaskList taskList) {
			
			this.tasks = new ArrayList<OrderedTask>();
			
			for(OrderedTask t : taskList.tasks) {
				this.tasks.add(new OrderedTask(t));
			}

			this.actions = new ArrayList<TaskAction>();
			for(int i = 0; i < 2*tasks.size(); i++)
				this.actions.add(null);
			
			for(OrderedTask t : tasks) {
				TaskAction pickUp = new TaskAction(t, true);
				TaskAction delivery = new TaskAction(t, false);
				
				this.actions.set(t.pickUpOrder, pickUp);
				this.actions.set(t.deliverOrder, delivery);
			}
		}
		
		// Get all possible permutations of two actions
		public List<TaskList> getAllPermutations(int vehicleCapacity) {
			List<TaskList> permutations = new ArrayList<TaskList>();
			
			for(int i = 0; i < actions.size() - 1; i++) {
				for(int j = i + 1; j < actions.size(); j++) {
					TaskList permuted = new TaskList(this);
					
					permuted.swapActions(i, j);
					
					if(permuted.checkOrder() && permuted.checkWeights(vehicleCapacity))
						permutations.add(permuted);
				}
			}
			
			return permutations;
		}
		
		// Check if the actions are feasible with the given capacity
		public boolean checkWeights(int vehicleCapacity) {
			int weight = 0;
			
			for(TaskAction a : actions) {
				weight += a.isPickUp ? a.task.task.weight : -a.task.task.weight;
				if(weight > vehicleCapacity)
					return false;
			}
			
			return true;
		}
		
		// Check if the order of the actions is feasible, i.e. tasks are always picked
		// up before being delivered.
		public boolean checkOrder() {
			for(OrderedTask t : tasks) {
				if(!t.checkOrder())
					return false;
			}
			
			return true;
		}
		
		// Swap the i-th and the j-th actions.
		public void swapActions(int i, int j) {
			TaskAction a1 = actions.get(i);
			TaskAction a2 = actions.get(j);
			
			this.actions.set(i, a2);
			this.actions.set(j, a1);
			
			a1.setOrder(j);
			a2.setOrder(i);
		}
		
		// Add a new task to the list. The pickup and delivery actions are done after
		// the ones already present in the list.
		public void addTask(Task task) {
			OrderedTask oTask = new OrderedTask(task, actions.size(), actions.size() + 1);
			
			this.tasks.add(oTask);
			
			TaskAction pickUp = new TaskAction(oTask, true);
			TaskAction delivery = new TaskAction(oTask, false);
			
			this.actions.add(pickUp);
			this.actions.add(delivery);
		}
		
		// Remove the idx-th task from the list.
		public Task removeTask(int idx) {			
			OrderedTask task = this.tasks.remove(idx);
			this.actions.remove(task.deliverOrder);
			this.actions.remove(task.pickUpOrder);
			
			for(int i = task.pickUpOrder; i < actions.size(); i++) {
				this.actions.get(i).setOrder(i);
			}
			
			return task.task;
		}
		
		// Return the distance to pick up and deliver all tasks, starting in the
		// provided city
		public double getDistance(City currentCity) {
			double dist = 0;
			
			City lastCity = currentCity;
			for(TaskAction a : actions) {
				dist += a.getCity().distanceTo(lastCity);
				lastCity = a.getCity();
			}
			
			return dist;
		}
		
		// Return the corresponding plan.
		public Plan getPlan(City initCity) {
			Plan plan = new Plan(initCity);
			
			City lastCity = initCity;
			for(TaskAction a : this.actions) {
				List<City> path = lastCity.pathTo(a.getCity());
				
				for(City c : path) {
					plan.appendMove(c);
				}
				
				if(a.isPickUp) {
					plan.appendPickup(a.task.task);
				}
				else {
					plan.appendDelivery(a.task.task);
				}
				
				lastCity = a.getCity();
			}
			
			return plan;
		}
	}
	
	
	
	
	
	
	// Class representing a task and the indexes at which it is picked up and delivered.
	private class OrderedTask {
		public final Task task;
		public int pickUpOrder;
		public int deliverOrder;
		
		public OrderedTask(Task task, int pickUpOrder, int deliverOrder) {
			this.task = task;
			this.pickUpOrder = pickUpOrder;
			this.deliverOrder = deliverOrder;
		}
		
		public OrderedTask(OrderedTask task) {
			this(task.task, task.pickUpOrder, task.deliverOrder);
		}
		
		// Check if the task is picked up before being delivered.
		public boolean checkOrder() {
			return pickUpOrder < deliverOrder;
		}
	}
	
	
	
	
	
	// Class representing an action for a task, i.e. pick it up or deliver it.
	private class TaskAction {
		public final OrderedTask task;
		public final boolean isPickUp;
		
		public TaskAction(OrderedTask task, boolean isPickUp) {
			this.task = task;
			this.isPickUp = isPickUp;
		}
		
		// Set the order of the action in its list.
		public void setOrder(int i) {
			if(isPickUp) {
				this.task.pickUpOrder = i;
			}
			else {
				this.task.deliverOrder = i;
			}
		}
		
		// Get the city in which the action must be performed.
		public City getCity() {
			if(isPickUp) {
				return task.task.pickupCity;
			}
			else {
				return task.task.deliveryCity;
			}
		}
	}
}