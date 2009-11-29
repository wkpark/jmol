package com.sparshui.gestures;

//import java.io.*;

import java.util.Vector;

import com.sparshui.common.Location;
import com.sparshui.common.messages.events.DragEvent;
import com.sparshui.common.messages.events.FlickEvent;

/**
 * 
 */
public class Flick extends StandardDynamicGesture {

	
	
	//****INNER CLASS for location****//
	class Locations{
		double X, Y;
		public Locations(){
			X=Y=0;}
		public Locations(double x, double y){
			X = x;
			Y= y;}
	}
	//****END INNER CLASS****//
	
	

	//****INNER CLASS for displacements****//
	class Displacements{
		double dx, dy, disp;
		public Displacements(){
			dx=dy=disp;}
		public Displacements(Locations first, Locations second){
			double x1 = first.X;
			double y1 = first.Y;
			double x2 = second.X;
			double y2 = second.Y;
			//calculate displacement
			dx = x2-x1;
			dy = y2-y1;
			disp = Math.sqrt((dx*dx) + (dy*dy));
		}
	}
	//****END INNER CLASS****//
	
	

	//****INNER CLASS for velocity****//
	public class Velocity{
		double vx, vy, v;	
		public Velocity(){
			vx = 0;
			vy = 0;
			v = 0;
		}		
		public Velocity(double xVelocity, double yVelocity, double velocity){
			vx = xVelocity;
			vy = yVelocity;
			v= velocity;
		}
	}
	//****END INNER CLASS****//

	
	
	//****INNER CLASS for acceleration****//
	class Acceleration{
		double ax, ay;
		public Acceleration(){
			ax=0;
			ay=0;
		}
		public Acceleration(double xAcceleration, double yAcceleration){
			ax = xAcceleration;
			ay = yAcceleration;
		}
	}
	//****END INNER CLASS****//

	
	
	
	//**** START - Parameter to send to flickEvent/application ****//
	
	int xDirection, yDirection;
	int flickSpeed; //1=slow, 2=med, 3=fast
	private Velocity[] runningVelocities;	//holds the last 5 running velocities
	//private Acceleration[] runningAccelerations;//holds the last 5 running accelerations
	
	//**** END -  Parameter to send to flickEvent/application ****//
	
	
	
	
	protected Location _offset = null;
	protected Location _offsetCentroid = null;
	private long time[];
	private Locations[] location;
	int numMoves;
	
	private Displacements[]displacements;
	int displacementPointer;//index of the displacements arrat
	int rVelocityPointer;	//index of the running velocity array
	int timePointer;		//index of the time array
	
	public Flick() {
		super();
		displacements = new Displacements[4];
		displacementPointer = 0;	//only the last 4 displacements will be stored
		rVelocityPointer = 0; 		//onlt the last 6 running velocities will be stored
		timePointer = 0;			//only the last 5 times will be stored
		//runningAccelerations = new Acceleration[5];	//the last five running accelerations
		location = new Locations[2];	//stores the last two move locations
		runningVelocities = new Velocity[7];
		numMoves = 0;
		time = new long[5];	//stores the last five times
	}

	//@override
	public String getName() {
		return "FlickGesture";
	}
	
	//@override
	public int getGestureType() {
		return GestureType.FLICK_GESTURE;
	}

	//@override
	protected Vector processBirth(TouchData touchData) {
		if(_offset == null) {
			_offset = new Location(0,0);
			_offsetCentroid = _newCentroid;
		} else {
			adjustOffset();
		}
		return null;
	}

	//@override
	protected Vector processMove(TouchData touchData) {
		Vector events = new Vector();
		updateOffsetCentroid();
		events.add(new DragEvent(_offsetCentroid.getX(), _offsetCentroid.getY()));
		//--------------------
		
		
		/*save current time */
		if(timePointer>4){
			for(int i=0;i<4;i++){
				time[i] = time[i+1];
			}
			time[4] = System.currentTimeMillis();
			timePointer++;
		}else{
			time[timePointer] = System.currentTimeMillis(); 
			timePointer++;
		}
		
		if (numMoves==0){
				
			/*save location*/
			location[0] = new Locations(touchData.getLocation().getX(), touchData.getLocation().getY());
			numMoves++;
		
		}else{
		
			numMoves++;
			
			//store the x,y, location
			if(numMoves>2){
				location[0] = location[1];
			}
			location[1] = new Locations(touchData.getLocation().getX(), touchData.getLocation().getY());
			
			//compute and store the displacement of the x,y
			if(displacementPointer>3){
				//shift displacements to make room for incoming displacement data
				for(int i=0;i<3;i++){
					displacements[i] = displacements[i+1];
				}
				//enter incoming displacement data
				displacements[3] = new Displacements(location[0], location[1]);
				displacementPointer++;
			}else{
				displacements[displacementPointer] = new Displacements(location[0], location[1]);
				displacementPointer++;
			}

			//compute running velocity
			if(displacementPointer>4){
				double sum=0,sum_x=0,sum_y = 0;			
				
				for(int i=0;i<4;i++){
					sum = sum+displacements[i].disp;
					sum_x = sum_x+displacements[i].dx;
					sum_y = sum_y+displacements[i].dy;
				}			
				
				double runningV = 1000000*(sum / ((time[4]-time[0])));	//pixels/millisecond
				double runningV_x = 1000000*(sum_x / ((time[4]-time[0])));
				double runningV_y = 1000000*(sum_y / ((time[4]-time[0])));
				
				if(rVelocityPointer>6){
					//shift runningVelocity array to make room for incoming velocity data
					for(int i=0; i<6;i++){
						runningVelocities[i] = runningVelocities[i+1];
					}
					runningVelocities[6] = new Velocity(runningV_x,runningV_y,runningV);
					rVelocityPointer++;
	
				}else{
									
					//write to runningVelocities array and increase size
					runningVelocities[rVelocityPointer] = new Velocity(runningV_x,runningV_y,runningV);
					rVelocityPointer++;
				}
			}

			
			/*compute running acceleration*/
			/**
			if(velocity.length > 2){
				//compute acceleration = velocity/ deltaT and increase array capacity to hold next acceleration data
				acceleration[acceleration.length-1].ax = Math.abs(((velocity[velocity.length-2].vx)
					-(velocity[velocity.length-3].vx))/ deltaT);
				acceleration[acceleration.length-1].ay = Math.abs(((velocity[velocity.length-2].vy)
						-(velocity[velocity.length-3].vy))/ deltaT);
				Acceleration[] tempAcceleration = new Acceleration[acceleration.length+1];
				System.arraycopy(acceleration, 0, tempAcceleration, 0, acceleration.length);
				acceleration = tempAcceleration;
				acceleration[acceleration.length-1] = new Acceleration();
			}
			**/
							
			
		}
		
		
		//-------------------
		
		
		return events;
	}

	//@override
	protected Vector processDeath(TouchData touchData) {
		
		//FlickDetectionNotice a= new FlickDetectionNotice();
		//BufferedWriter out = null;
		
		if(_knownPoints.size() == 0) {
			_offset = null;
			_offsetCentroid = null;
		} else {
			adjustOffset();
		}
		
		//---------------------------------
		
		Vector events = new Vector();
		

/*		try {
			out = new BufferedWriter(new FileWriter("C:/Documents and Settings/Ankit Patel/My Documents/Multitouch/FlickData/FlickData_SLOW.csv",true));
	    	//out.write("------------\n");
			String data = "0.000,0.000\n";
			out.write(data);
			out.flush();
		
		//print testing data to textfile to find out threshold
		int flag = 0;
		if(numMoves>=9){
			flag = 1;
		}
		for(int i = 0; i<runningVelocities.length && flag==1; i++){
			data = i + "," + runningVelocities[i] + "\n";
				
			//write to file
			out.write(data);
			out.flush();			
		}
		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	*/
		int positive = 0;

		for(int j = 7, i=0; rVelocityPointer>=6 && j>2;j--, i++){
			double temp = runningVelocities[runningVelocities.length-j+1].v - runningVelocities[runningVelocities.length-j].v;
			if (temp>=0){
				positive++;
			}
		}
		
		if (positive>=3){
			
/**			IT'S A FLICK
			Send the following to flick event:
				->Speed Level: 
					1:Slow(0-500 pixels/millisecond)
					2:Medium(501-1000 pixels/millisecond)
					3:Fast(1001+ pixels/millisecond)
				->runningVelocities
				->direction X
				->direction Y
*/
			
			/***Calculate Speed Level by taking average of last 5 runningVelocites, ignoring the very last one***/
			double avgRV=0;
			for(int i=1; i<=5; i++){
				avgRV = avgRV + runningVelocities[runningVelocities.length-1-i].v;
			}
			avgRV = avgRV/5;
			
			int speedLevel;
			if(avgRV<=500)	//threshold set to 500s
				speedLevel = 1;
			else if (avgRV<=1000)
				speedLevel = 2;
			else
				speedLevel = 3;
			/*************************************************/
			
			
			/**Calculate X and Y directions**/
			int xDirection, yDirection;
			if(location[1].X - location[0].X>0)
				xDirection = 1;
			else
				xDirection = -1;
			
			if(location[1].Y - location[0].Y>0)
				yDirection = 1;
			else
				yDirection = -1;
			/***************************/
			
			
			events.add(new FlickEvent(speedLevel, xDirection, yDirection));
			//String text;
			if (speedLevel==1){
				//text = "SLOW!";
			}else if (speedLevel ==2){
				//text = "MEDIUM!";
			}else{
				//text = "FAST!";
			}
//			a.show(text);	//shows gui pop up to hint a flick gesture has taken place
/*			try {
				out.write("FLICK/n");
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
*/			
		}
				

		return events;
	}
	
	/**
	 * 
	 */
	protected void adjustOffset() {
		_offset = new Location(
				_newCentroid.getX() - _oldCentroid.getX() + _offset.getX(),
				_newCentroid.getY() - _oldCentroid.getY() + _offset.getY()
		);
	}
	
	/**
	 * 
	 */
	protected void updateOffsetCentroid() {
		float x = _newCentroid.getX() - _offset.getX();
		float y = _newCentroid.getY() - _offset.getY();
		_offsetCentroid = new Location(x, y);
	}

}