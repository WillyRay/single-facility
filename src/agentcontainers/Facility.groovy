package agentcontainers;
import agents.Person;
import disease.FacilityOutbreak;
import disease.PersonDisease;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GammaDistribution
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import java.util.ArrayList;
import java.util.LinkedList;
public class Facility extends AgentContainer{
  
   public int currentPopulationSize = 0;
   public double betaIsolationReduction;
   public double timeBetweenMidstaySurveillanceTests = -1.0;
   boolean onActiveSurveillance = false;
   public int type;
   public Region region;
   public double newPatientAdmissionRate;
   public double avgPopTarget;
   public double meanLOS;
   double avgPopulation;
   int numDaysTallied = 0;
   double patientDays;
   int numAdmissions = 0;
   double admissionSurveillanceAdherence = 0.911;
   double midstaySurveillanceAdherence = 0.954;
   ExponentialDistribution distro;
   ISchedule schedule;
   ISchedulableAction nextAction;
   public ArrayList<FacilityOutbreak> outbreaks = new ArrayList<>();
   LinkedList<Person> currentPatients = new LinkedList<>();
   boolean stop = false
   double meanIntraEventTime;
   public Facility(double intra_event_time, ISchedule schedule) {
	   super(intra_event_time)
	   meanIntraEventTime = intra_event_time
	   if(meanIntraEventTime>0) {
		   distro = new ExponentialDistribution(intra_event_time)
	   }
	   this.schedule = schedule;
	   region = new Region(meanIntraEventTime, this);
	   
   }
   
   public void admitNewPatient(ISchedule sched) {
	   	schedule = sched
	   	stop = false
        double currTime = schedule.getTickCount()
        double elapse = distro.sample()
        ScheduleParameters params = ScheduleParameters.createOneTime(currTime + elapse)
        nextAction = schedule.schedule(params, this, "doNewPatientAdmission")
		currentPopulationSize++;
}
   void doNewPatientAdmission(){
	   	System.out.println("Do new patient admission");
		region.importToFacility(this);
		currentPopulationSize++;
		if(!stop) {
			admitPatient(new Person(meanIntraEventTime,schedule));
		}
	}
   void admitPatient(Person p){
		p.admitToFacility(this);

		p.startDischargeTimer(getRandomLOS());

		currentPopulationSize++;
		 
		for(PersonDisease pd : p.diseases){
			if(pd.colonized){
				if(pd.disease.isActiveSurveillanceAgent() && onActiveSurveillance){ 
					if(uniform() < pd.disease.getProbSurveillanceDetection() * admissionSurveillanceAdherence){
						pd.detected = true;
						if(pd.disease.isolatePatientWhenDetected()) p.isolate();
					}
				}
				pd.startClinicalDetectionTimer();
			}
		}
		currentPatients.add(p);

		if(onActiveSurveillance && !p.isolated && timeBetweenMidstaySurveillanceTests > 0) 
			p.startNextPeriodicSurveillanceTimer();

		p.updateAllTransmissionRateContributions();

		if(!region.inBurnInPeriod) updateAdmissionTally(p);
		admitNewPatient(schedule);
	}
   void dischargePatient(Person p){
		currentPopulationSize--;
		currentPatients.remove(p);
		updateTransmissionRate();

		if(!region.inBurnInPeriod) updateStayTally(p);

		p.destroyMyself(region);
	}

	void updateTransmissionRate(){
		for(FacilityOutbreak fo : outbreaks) fo.updateTransmissionRate();
	}

   double getRandomLOS(){
		if(type==0){
			
			double shape1 = 7.6019666;
			double scale1 = 3.4195217;
			double shape2 = 1.2327910;
			double scale2 = 23.5214724;
			double prob1 = 0.6253084;
	
			if(uniform() < prob1) return gamma(shape1,scale1);
			else return gamma(shape2,scale2);
		}
		else{
			return -1.0;
		}
	}	

	void admitInitialPatient(Person p){
		p.admitToFacility(this);
		p.startDischargeTimer(exponential(1.0/meanLOS));

		currentPopulationSize++;

		boolean doSurveillanceTest = false;
		if(onActiveSurveillance) doSurveillanceTest = true;
		 
		for(PersonDisease pd : p.diseases){
			if(pd.colonized){
				pd.startClinicalDetectionTimer();
			}
		}
		currentPatients.add(p);

		p.updateAllTransmissionRateContributions();
		currentPopulationSize++;
	}

	void updatePopulationTally(){
		avgPopulation = (avgPopulation * numDaysTallied + currentPopulationSize) / (numDaysTallied + 1);
		numDaysTallied++;

		for(FacilityOutbreak fo : outbreaks) fo.updatePrevalenceTally();
	}

	void updateStayTally(Person p){
		patientDays += p.currentLOS;

		for(int i=0; i<outbreaks.size(); i++)
			outbreaks.get(i).updateStayTally(p.diseases.get(i));

	}

	void updateAdmissionTally(Person p){
		numAdmissions++;

		for(int i=0; i<outbreaks.size(); i++)
			outbreaks.get(i).updateAdmissionTally(p.diseases.get(i));
	}

	void startActiveSurveillance(){
		onActiveSurveillance = true;
	}
	double uniform() {
		return Math.random();
	}
	double gamma(double shape, double scale) {
		GammaDistribution gammaDistribution = new GammaDistribution(shape, scale);
		return gammaDistribution.sample();
	}
	double exponential(double rate) {
		ExponentialDistribution exponentialDistribution = new ExponentialDistribution(rate);
		return exponentialDistribution.sample();
	}
	public FacilityOutbreak addOutbreaks() {
    FacilityOutbreak newOutbreak = new FacilityOutbreak(meanIntraEventTime);
    newOutbreak.setFacility(this);

    outbreaks.add(newOutbreak);

    return newOutbreak;
}
public int getType() {
	return type;
	}

public void addOutbreak(FacilityOutbreak outbreak) {
	outbreaks.add(outbreak)
	
}

public int getCapacity() {
	return capacity;
}

public void setIsolationEffectiveness(double isolationEffectiveness) {
	this.isolationEffectiveness = isolationEffectiveness;
	
}
}
