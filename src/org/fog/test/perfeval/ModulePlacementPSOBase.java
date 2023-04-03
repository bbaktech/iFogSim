//ModulePlacementPSOBase
package org.fog.test.perfeval;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.utils.TimeKeeper;

class Particle {
	
	AppLoop apploop;
	List<FogDevice>  no_devises;
	List<AppModule> no_modules;
	List <AppEdge> listedges; 
	
	double fitness ;
	double pBestfitness ;
	
	int[][] indextble = new int[8][8];
	int[][] pBestindextble = new int[8][8];
	
	Particle(List<FogDevice> fds, List<AppModule> mdls, AppLoop apploop, List <AppEdge> listedges) {
		 this.no_devises =fds ;  
		 this.no_modules =mdls; 
		 this.apploop = apploop;
		 this.listedges = listedges;
		 this.fitness =0 ;
		 this.pBestfitness=0 ;	 
	}

	int Get(int i,int j) {
		return indextble[i][j];
	}
	
	int pBestGet(int i,int j) {
		return pBestindextble[i][j];
	}

	void RandumInitialize() {		
		for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++)
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 indextble[i][j] =0;
		    	 pBestindextble[i][j]=0;
		     }
		Random rand = new Random();
		
		for (int i = 0; i< no_modules.size(); i++) {
	        int r = rand.nextInt(ModulePlacementPSOBase.MAX_NO_R_LEVELS);		
	        indextble[r][i]++;
	        pBestindextble[r][i]++;
		}
		pBestfitness = ComputeFitness();
	}
	
	double ComputeFitness() {
		
		double loopdelay = 0.0;
		List <String> loop_modules = apploop.getModules();			
		for (int i =0; i < loop_modules.size()-1; i++) {

			for(AppEdge appedge : listedges){
				if (appedge.getSource().equalsIgnoreCase(loop_modules.get(i)) && appedge.getDestination().equalsIgnoreCase(loop_modules.get(i+1))) {
					double tplCpuLenth = appedge.getTupleCpuLength();
					double tplNwLenth= appedge.getTupleNwLength();
//					System.out.println(appedge.getDestination() + " CPU ln:" +tplCpuLenth + " NW L"+ tplNwLenth);	
					
					double latncy = 0;
					double cpuMips =0;

					if (appedge.getEdgeType() == AppEdge.ACTUATOR) {
						latncy =1;
						latncy = latncy + GetLatencySourcToDest(loop_modules.get(i),null);
						loopdelay = loopdelay + latncy*tplNwLenth  ;
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth);	

					} else if (appedge.getEdgeType() == AppEdge.SENSOR) {
						latncy = 6;						
						latncy = latncy + GetLatencySourcToDest(null,loop_modules.get(i+1));
						cpuMips = GetModuleMapedResoureMIPS(appedge.getDestination());	
						loopdelay = loopdelay + latncy*tplNwLenth + tplCpuLenth /cpuMips*1000 ;					
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth+" CPU-L"+ tplCpuLenth + " Speed:"+cpuMips );	

					} else {
						cpuMips = GetModuleMapedResoureMIPS(appedge.getDestination());	
						latncy = GetLatencySourcToDest(loop_modules.get(i),loop_modules.get(i+1));
						loopdelay = loopdelay + latncy*tplNwLenth + tplCpuLenth /cpuMips*1000  ;
//						System.out.println("latncy:"+latncy +" tplNwLenth:"+tplNwLenth+" CPU-L"+ tplCpuLenth + " Speed:"+cpuMips );	
						//latency in milliseconds, network_length in Bytes, CPU_length in bytes	,	CPU(speed) in Million Instruction Per Second
					}
					break;
				}
			}
		}
//		System.out.println("Total loopdelay(seconds)"+loopdelay/1000);
		fitness = loopdelay/1000;
    	return fitness;	
	}
		
	private FogDevice GetModuleMapedResoure(String m) {
		FogDevice fd = null;		
//		List<AppModule> no_modules;
		int k = 0;
		for (k = 0; k< no_modules.size(); k++) {
			if ( no_modules.get(k).getName().equalsIgnoreCase(m))
				break;  
		}
		
		int level = 0;
		for ( level = 0 ; level< ModulePlacementPSOBase.MAX_NO_R_LEVELS; level++) {
		    	 if (indextble[level][k] == 1) 
		    		 break;
		}		
		
		for(FogDevice device : no_devises){
			if (device.getLevel() == level) {
				fd = device;
				break;
			}
		}
		
		return fd;
	}

	private double GetLatencySourcToDest(String source, String dest) {
		// TODO Auto-generated method stub
		
		FogDevice srcDev = null, destDev = null;
		
		for(FogDevice device : no_devises){
			if (device.getLevel() == ModulePlacementPSOBase.MAX_NO_R_LEVELS - 1) {
				srcDev = device;
				destDev = device;
				break;
			}
		}

		double latency = 0.0;
		
		if (source != null) srcDev = GetModuleMapedResoure(source);		
		if (dest != null) destDev = GetModuleMapedResoure(dest);
		
		int srcL= srcDev.getLevel();
		int destL=destDev.getLevel();
		
		while (srcL != destL ) {
			
			if (destL > srcL) {
					srcL++ ;
					for(FogDevice device : no_devises){
						if (device.getLevel() == srcL) {
							srcDev = device;
							break;
						}
					}
					latency = latency + srcDev.getUplinkLatency();
				}
			if (destL < srcL) {		
					for(FogDevice device : no_devises){
						if (device.getLevel() == srcL) {
							srcDev = device;
							latency = latency + srcDev.getUplinkLatency();							
							break;
						}
					}
					srcL-- ;
				}
		}
			
		return latency;
	}
	
	private int GetModuleMapedResoureMIPS(String m) {
		// TODO Auto-generated method stub
		int cpuMips = 0;		
//		List<AppModule> no_modules;
		int k = 0;
		for (k = 0; k< no_modules.size(); k++) {
//			System.out.println(m + " compare-K"+ k + no_modules.get(k).getName());			
			if ( no_modules.get(k).getName().equalsIgnoreCase(m))
				break;  
		}
		int level = 0;
		for ( level = 0 ; level< ModulePlacementPSOBase.MAX_NO_R_LEVELS; level++) {
		    	 if (indextble[level][k] == 1) 
		    		 break;
		}
		
		for(FogDevice device : no_devises){
			if (device.getLevel() == level) {
				cpuMips = device.getHost().getTotalMips();
				break;
			}
		}
		return cpuMips;
	}
	
	void printMap() {
		for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++) {
		     System.out.print("Level("+i+"): ");
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 System.out.print(indextble[i][j]+" ");
		     }
		     System.out.println();
		}
	}

	void printpBestMap() {
		for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++) {
		     System.out.print("Level("+i+"): ");
		     for ( int j = 0 ;j< no_modules.size();  j++) {
		    	 System.out.print(pBestindextble[i][j]+" ");
		     }
		     System.out.println();
		}
		
	}
	
	void MoveParticle(int dist , Particle toParticle ) {
		
		if (toParticle != null) {
			for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++) {
			     for ( int j = 0 ;j< no_modules.size();  j++) {
			    	if  (dist == 0) break;    	
			    	if ( indextble[i][j] != toParticle.indextble[i][j]) {
			    		int temp =indextble[i][j];
			    		 //moved towords toParticle
			    		if (temp == 0) {
			    			for ( int k = 0 ; k<ModulePlacementPSOBase.MAX_NO_R_LEVELS; k++) {
			    				indextble[k][j] = temp;
			    			}
			    		indextble[i][j] = toParticle.indextble[i][j];
			    		} else {   indextble[i][j] = toParticle.indextble[i][j];
				    		if (i+1 < ModulePlacementPSOBase.MAX_NO_R_LEVELS)
				    			indextble[i+1][j] = temp;
				    		else indextble[0][j] = temp;	

			    		}
			    		dist --;
			    	}
			     }
			}
		} else {
			for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++) {
			     for ( int j = 0 ;j< no_modules.size();  j++) {
			    	if  (dist == 0) break;    	
			    	if ( indextble[i][j] != pBestindextble[i][j]) {
			    		int temp =indextble[i][j];
			    		 //moved towords toParticle
			    		if (temp == 0) {
			    			for ( int k = 0 ; k<ModulePlacementPSOBase.MAX_NO_R_LEVELS; k++) {
			    				indextble[k][j] = temp;
			    			}
			    		indextble[i][j] = pBestindextble[i][j];
			    		} else {   indextble[i][j] = pBestindextble[i][j];
				    		if (i+1 < ModulePlacementPSOBase.MAX_NO_R_LEVELS)
				    			indextble[i+1][j] = temp;
				    		else indextble[0][j] = temp;	

			    		}
			    		dist --;
			    	}
			     }
			}
		}
		
		//new position is better than old pBest then set new position as pBest
		if (pBestfitness > ComputeFitness()) {
			for ( int i = 0 ;i< ModulePlacementPSOBase.MAX_NO_R_LEVELS;i++) {
			     for ( int j = 0 ;j< no_modules.size();  j++) {
			    	 pBestindextble[i][j]=indextble[i][j];
			     }
			}			
			pBestfitness = fitness;
		}
	}
}

public class ModulePlacementPSOBase extends ModulePlacement{

	private ModuleMapping moduleMapping;
	public static int MAX_NO_MODLS = 4;
	public static int MAX_NO_R_LEVELS = 4;
	public static int NO_PARTICLES = 4;
	public static int NO_ITERATIONS = 10;
	
	@Override

	protected void mapModules() {
			Map<String, List<String>> mapping = moduleMapping.getModuleMapping();
			for(String deviceName : mapping.keySet()){
				FogDevice device = getDeviceByName(deviceName);
				for(String moduleName : mapping.get(deviceName)){
					
					AppModule module = getApplication().getModuleByName(moduleName);
					if(module == null)
						continue;
					createModuleInstanceOnDevice(module, device);
					//getModuleInstanceCountMap().get(device.getId()).put(moduleName, mapping.get(deviceName).get(moduleName));
				}
			}
	}

	public ModulePlacementPSOBase(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		this.setModuleInstanceCountMap(new HashMap<Integer, Map<String, Integer>>());
		
		for(FogDevice device : getFogDevices())
			getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
		
		PSOResourceSheduleing();
		
		mapModules();
	}
	
	private void PSOResourceSheduleing() {
		// TODO Auto-generated method stub

		List <AppModule> apMdls = getApplication().getModules();
		List <AppEdge> appedges = getApplication().getEdges();
		List <AppLoop> apploops = getApplication().getLoops();
		AppLoop apploop = apploops.get(0);
		
		List <Particle> particles = new LinkedList<Particle>(); 
		
//		List <Particle> pBestParicles = new LinkedList<Particle>(); 		
//		double gBestParticle = null;

		for (int i = 0 ; i <NO_PARTICLES; i++) {
			Particle p = new Particle(getFogDevices(),apMdls, apploop, appedges);
			p.RandumInitialize();
			particles.add(p);
		}		

		
//PSO Algorithm	loop	
		Particle gBest_particel = particles.get(0);
		double gBest = gBest_particel.pBestfitness;

		for (int iCont = 0 ; iCont <NO_ITERATIONS; iCont++) {
		
				for (int i = 0 ; i <particles.size(); i++) {
					Particle p = particles.get(i);
					if ( gBest > p.pBestfitness) {
						gBest_particel = p;
						gBest = p.pBestfitness;			
					}
				}
				//compute Velocity and move each particles to pBest or gBest base on random number
				int speed = 2;
				Particle TowardsParticle = null;				
				Random rand = new Random();
				
				int r = rand.nextInt(4);
				if (r == 0) {
					speed = 1;
					TowardsParticle = gBest_particel;
				} else if (r==1) {
					speed = 2;
					TowardsParticle = null;
				} else if (r==2) {
					speed = 1;
					TowardsParticle = gBest_particel;
				} else if (r==3) {
					speed = 2;
					TowardsParticle = null;
				}
				
				//move each particle to new position with speed and diractionTowards				
				for (int i = 0 ; i <particles.size(); i++) {
					Particle p = particles.get(i);
					p.MoveParticle(speed,TowardsParticle);
				}	
		}
	
		Particle selected_particel = gBest_particel;	
		
		System.out.println("gWaightparticle:"+selected_particel.pBestfitness);		
		selected_particel.printpBestMap();		
		for (int i = 0 ;i < ModulePlacementPSOBase.MAX_NO_R_LEVELS; i++) {
			for(FogDevice device : fogDevices){
				if ( device.getLevel()==i) {
					for (int j = 0 ; j < apMdls.size(); j++) {
						if (1==selected_particel.pBestGet(i, j)) 
							moduleMapping.addModuleToDevice(apMdls.get(j).getName(), device.getName());
					}
				}
			}
		}
	}

	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}
	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}
}
