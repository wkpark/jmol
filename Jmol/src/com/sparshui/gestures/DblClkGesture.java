package com.sparshui.gestures;


import java.util.Vector;

import com.sparshui.common.TouchState;
import com.sparshui.common.messages.events.DblClkEvent;
import com.sparshui.server.TouchPoint;

public class DblClkGesture implements Gesture {

	//*******************************************************************************
	//Inner class
	public class TouchPtData{
		TouchPoint touchPt;
		long time;

		//default constructor
		public TouchPtData(){
			time = -1;
			//touchPt = null;
		}

		//specific constructor
		public TouchPtData(TouchPoint tp, long timeP){
			touchPt = tp;
			time = timeP;
		}
	}
	//end Inner class
	//********************************************************************************


	private TouchPtData[] tpData;
	private int nextElement;
	private int numElems;


	//default constructor
	public DblClkGesture(){
		tpData = new TouchPtData[4];
		nextElement = 0;
		numElems = 0;
	}

	//@override
	public int getGestureType() {
		//return the double click gesture type
		return GestureType.DBLCLK_GESTURE;
	}

	//@override
	public String getName() {
		return "DblClkGesture";
	}


	/**
	 * Process a touch point change in the gesture.
	 * 
	 * @param touchPoints
	 * 		The list of touch points that currently belong to this gesture.
	 * @param changedTouchPoint
	 * 		The touch point that has changed.
	 * @return
	 * 		A vector of events that will be delivered to the client.
	 * 		
	 */
	//@override
	public Vector processChange(Vector touchPoints,
			TouchPoint changedTouchPoint) {

		Vector retEvents = new Vector();
		//if its a move, ignore and return from method.
		//if(changedTouchPoint.getState()== TouchState.MOVE){
	//		System.out.println("returning null");
			//return retEvents;
	//	}

		//make a TouchPtData since it's a birth or death
		long currTime = System.currentTimeMillis(); 	//save system time of this touch event

	    TouchPoint tpt = (TouchPoint) changedTouchPoint.clone();
		tpData[nextElement] = new TouchPtData(tpt,currTime) ;
		if(numElems<4){
			numElems++;
		//	System.out.println("NUMELEMS: " + numElems);
		}
		if(nextElement<3){
			nextElement++;	//increment next element
		}

		
     
		//if there are 4 elements, see if the order is BIRTH,DEATH,BIRTH,DEATH (behavior of right click)
	 if(numElems ==4){
        //    System.out.println("***STATES***:\n0:" + tpData[0].touchPt.getState()+ " 1:" + tpData[1].touchPt.getState()
        //    		 + " 2:" + tpData[2].touchPt.getState() + " 3:" + tpData[3].touchPt.getState());
			//make sure the ID for the BIRTH DEATH combination are the same
			boolean isMove = false;
			if(!(tpData[0].touchPt.getState()==TouchState.BIRTH) || !(tpData[1].touchPt.getState()==TouchState.DEATH)
					|| !(tpData[2].touchPt.getState()==TouchState.BIRTH) 
					|| !(tpData[3].touchPt.getState()==TouchState.DEATH)
					&& !(isMove = (tpData[3].touchPt.getState()==TouchState.MOVE))){
				shiftArr();
				return retEvents;
			}

				//check that the change in time is less then a second and more then .2
				long duration = Math.abs(tpData[0].time - tpData[3].time);
				//System.out.println("Duration= " + duration);
				//change in time must be <=1 seconds (1000 milliseconds) in order for it to be considered a double click gesture
				if(duration <= 1000){

					//the two touch events must have occurred within +/- 10%
					//range of each other's x,y coordinate
					if(Math.abs(tpData[0].touchPt.getLocation().getX()-tpData[2].touchPt.getLocation().getX())<=0.1 &&	//comparing X coordinates 
							Math.abs(tpData[0].touchPt.getLocation().getY()-tpData[2].touchPt.getLocation().getY())<=0.1){ //comparing Y coordinates
						

						
						retEvents.add(new DblClkEvent(changedTouchPoint.getID(),
								changedTouchPoint.getLocation().getX(),
								changedTouchPoint.getLocation().getY(), isMove ? 0 : 1));
				//		System.out.println("\nDouble click event occurred: change in TIME> "+ 
				//				duration + " prev> " + tpData[0].touchPt.getID() + " curr> " + tpData[2].touchPt.getID());
				//		System.out.println("Previous (X,Y): ("+tpData[0].touchPt.getLocation().getX()+","+tpData[0].touchPt.getLocation().getY()+")");
				//		System.out.println("Current  (X,Y): ("+tpData[2].touchPt.getLocation().getX()+","+tpData[2].touchPt.getLocation().getX()+")\n");	
						
						//DOUBLE CLICK OCCURED!
						//clear out the tpData array and member variables
						TouchPtData[] temp = new TouchPtData[4];
						tpData = temp;
						numElems = 0;
						nextElement = 0;
					}
				}
				
				
	 }
	
	 if(numElems==4){ //if there's four elements, shift array and store into last position
		 shiftArr();
	 }
		return retEvents;
}


	private void shiftArr(){
		TouchPtData[] temp =  new TouchPtData[4];
		for(int i=0; i<temp.length; i++){
	//		System.out.println("BEFORE SWAP: " +tpData[i].touchPt.getState());
		}
		System.arraycopy(tpData, 1, temp, 0, 3);
	//	temp[3] = new TouchPtData(changedTouchPoint, currTime);	//add new data
		tpData = temp;	//update array
		numElems =3;
		nextElement = 3;
	//	System.out.println("SWAPPED!!!");
		
		for(int i=0; i<temp.length; i++){
	//		System.out.println("AFTER SWAP: " + tpData[i].touchPt.getState());
			
		}
	//	System.out.println("NumElem: " + numElems+ " | Next index: "+nextElement);
	}
}
