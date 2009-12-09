/**
 *  JmolMultiTouchDriver.cpp//HPTouchSmartDriver.cpp
 *  
 *  Authors:  Andrew Koehring
 *            Jay Roltgen
 *  
 *  Date:     July 24th, 2009
 *
 *  This file uses the NextWindow MultiTouch API to receive touch event 
 *  info from the HP TouchSmart computer and send via a socket connection
 *  to the Sparsh-UI Gesture Server.
 *
 *
 *  Modified for Jmol by Bob Hanson 12/8/2009
 *
 *  -- for use in association with jmol.sourceforge.net using -Msparshui startup option
 *     (see http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/com/sparshui) 
 *  -- consolidates touchInfos, lastTimes, and touchAlive as "TouchPoint" structure
 *  -- uses SystemTimeToFileTime to generate a "true" event time
 *  -- delivers event time as a 64-bit unsigned integer (ULONGLONG)
 *  -- ignores the NextWindow repetitions at a given location with slop (+/-1 pixel)
 *  -- delivers true moves as fast as they can come (about every 20 ms)
 *  -- times out only for "full death" events (all fingers lifted) after 75 ms
 *  -- automatically bails out if started with a server and later loses service
 *  --  (e.g. applet page closes)
 *  -- operates with or without server for testing
 *  -- not fully inspected for memory leaks or exceptions
 *
 * It's been a LONG time since I did any C++ programming; feel free to criticize!
 *
 */

#include "stdafx.h"
#include "math.h"
#include "NWMultiTouch.h"
#include "time.h"
#include <string>


// The desired time to wait for more touch move events before sending the touch death.
#define TOUCH_WAIT_TIME_MS (75)
#define TOUCH_SLOPXY (1)
ULONGLONG time_last = 0;

// Flag to stop execution
bool running = true;

// Socket for sending information to the gesture serve
SOCKET sockfd;
bool haveSocket;
#define PORT (5945)

// The height and width of the screen in pixels.
int displayWidth, displayHeight;

using namespace std;

// Holds touch point information - this format is compatible with the Sparsh-UI
// gesture server format. (Expanded by Bob Hanson)
struct TouchPoint {

 // for delivery (network endian):

    char _type;
    int _id; 
    float _x;
    float _y;
    ULONGLONG _time;
 
 // local only:

    int _index;
    bool _alive;
    point_t _touchPos;
    ULONGLONG _timeReceived;
    ULONGLONG _timeSent;
};

// Holds the last touch point received on each particular touch ID.
TouchPoint touchPoints[MAX_TOUCHES];

// The type of the touch received from the device.
typedef enum TouchDeviceDataType {
   POINT_BIRTH,
   POINT_DEATH,
   POINT_MOVE,
};

ULONGLONG getTimeNow() {

 // thank you: http://en.allexperts.com/q/C-1040/time-milliseconds-Windows.htm

    SYSTEMTIME st;
    GetSystemTime(&st);
    FILETIME fileTime;
    SystemTimeToFileTime(&st, &fileTime);
    ULARGE_INTEGER uli;
    uli.LowPart = fileTime.dwLowDateTime;
    uli.HighPart = fileTime.dwHighDateTime;
    ULONGLONG systemTimeIn_ms(uli.QuadPart/10000);
    return systemTimeIn_ms;
}

/**
 * Initialize the last times
 */
void inittouchPoints() {
        for (int i = 0; i < MAX_TOUCHES; i++) {
                touchPoints[i]._timeReceived = getTimeNow();
                touchPoints[i]._index = i;
                touchPoints[i]._alive = false;
        }
}

/**
 * Swaps float byte order.
 */
float swapFloatEndian(float x) {
        union u {
                float f; 
                char temp[4];
        } un, vn;

        un.f = x;
        vn.temp[0] = un.temp[3];
        vn.temp[1] = un.temp[2];
        vn.temp[2] = un.temp[1];
        vn.temp[3] = un.temp[0];
        return vn.f;
}

/**
 * Swaps int byte order.
 */
int swapIntEndian(int x)
{
        union u {
                int f; 
                char temp[4];
        } un, vn;

        un.f = x;
        vn.temp[0] = un.temp[3];
        vn.temp[1] = un.temp[2];
        vn.temp[2] = un.temp[1];
        vn.temp[3] = un.temp[0];
        return vn.f;
}

/**
 * Swaps long byte order.
 */
ULONGLONG swapLongEndian(ULONGLONG x)
{
        union u {
                 ULONGLONG f; 
                char temp[8];
        } un, vn;

        un.f = x;
        vn.temp[0] = un.temp[7];
        vn.temp[1] = un.temp[6];
        vn.temp[2] = un.temp[5];
        vn.temp[3] = un.temp[4];
        vn.temp[4] = un.temp[3];
        vn.temp[5] = un.temp[2];
        vn.temp[6] = un.temp[1];
        vn.temp[7] = un.temp[0];
        return vn.f;
}


void dumpTouchPoint(TouchPoint *tpp) {
       cout << tpp->_index
        << " state: " << (int) tpp->_type
        << " time: " << swapLongEndian(tpp->_time)
        << " X Coordinate:  " << (int) tpp->_touchPos.x
        << " Y Coordinate:  " << (int) tpp->_touchPos.y
        << endl;
}

/**
 * Send touch point information to the gesture server
 * 
 * @param touchpoint
 *     The touch point we want to send to the Sparsh-UI Gesture Server.
 */
bool sendPoint(TouchPoint *tpp) {

        tpp->_timeSent = tpp->_timeReceived;
        dumpTouchPoint(tpp);

        if (!haveSocket)
          return true;
          
        int tempsize = sizeof(int) + 2 * sizeof(float) + sizeof(char) + sizeof(ULONGLONG); // 25 bytes
        int totalsize = tempsize + sizeof(int);

        char* buffer = (char*) malloc(totalsize); 
        char* bufferptr = buffer;

        // Number of touch points in this packet
        int temp2 = htonl(1);
        memcpy(bufferptr, &temp2, sizeof(int));
        bufferptr += sizeof(int);

        // TouchPoint id
        memcpy(bufferptr, &(tpp->_id), sizeof(int));
        bufferptr += sizeof(int);
        
        // TouchPoint x
        memcpy(bufferptr, &(tpp->_x), sizeof(float));
        bufferptr += sizeof(float);

        // TouchPoint y
        memcpy(bufferptr, &(tpp->_y), sizeof(float));
        bufferptr += sizeof(float);  

        // TouchPoint type
        memcpy(bufferptr, &(tpp->_type), sizeof(char));
        bufferptr += sizeof(char);

        // TouchPoint time
        memcpy(bufferptr, &(tpp->_time), sizeof(ULONGLONG));
        bufferptr += sizeof(ULONGLONG);
     
        // Send the touchpoint.
        int nSent = send(sockfd, buffer, totalsize, 0); 
        cout << "[JmolMultiTouchDriver] send " << nSent;
        free(buffer);
        if (nSent < 0) {
            // Jmol should automatically restart this.
            cout << "[JmolMultiTouchDriver] send failed -- exiting " << endl;
            running = false;
            return true;
        }
        return (nSent > 0);
}

/**
 * Initialize the socket connection to the gesture server.
 */

bool initSocket() {
            //Set up socket information
        cout << "[JmolMultiTouchDriver] initSocket" << endl;
        if (haveSocket)
            WSACleanup();
        cout << "Setting up socket library" << endl;
        WSADATA wsaData;
        if(WSAStartup(0x101,&wsaData) != 0) {
                cout << "Error initializing socket library." << endl;
                return false;
        }
        sockaddr_in inetAddress;
        sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    
        if(sockfd == INVALID_SOCKET) {
            printf("invalid socket\n");
            return false;
        }
        
        // Set up the socket information
        inetAddress.sin_family = AF_INET;
        inetAddress.sin_port = htons((u_short) (PORT));
        
        unsigned long addr = inet_addr("127.0.0.1");
        hostent* host =  gethostbyaddr((char*) &addr, sizeof(addr), AF_INET);
        inetAddress.sin_addr.s_addr = *((unsigned long*)host->h_addr);
        
        // Connect to the gesture server.
        if(connect(sockfd, (struct sockaddr*) &inetAddress, sizeof(sockaddr)) != 0) {
                printf("[JmolMultiTouchDriver] Connect error.\n");
                return false;
        }

        // Send the device type.
        const char one = 1;
        int nBytes = send(sockfd, &one, sizeof(char), 0);
        if (nBytes < 1) {
            cout << "JmolMultiTouchDriver connect refused." << endl;
            return false;
        }
        cout << "JmolMultiTouchDriver connect succeeded." << endl;
        return true;
}


/**
 * Converts the point information from the device information to the desired
 * Sparsh-UI information.
 */
void convertTouchToSparsh(NWTouchPoint* src, TouchPoint* dst) {
        dst->_id = swapIntEndian((int) src->touchID);
        dst->_x = swapFloatEndian((float) src->touchPos.x / (float) displayWidth);
        dst->_y = swapFloatEndian((float) src->touchPos.y / (float) displayHeight);
        dst->_time = swapLongEndian(getTimeNow());
        dst->_touchPos.x = src->touchPos.x;
        dst->_touchPos.y = src->touchPos.y;
}

/**
 * Process a touch birth that was received from the device.
 */
void processBirth(NWTouchPoint *nwtpp, TouchPoint *tpp) {
        tpp->_type = POINT_BIRTH;
        convertTouchToSparsh(nwtpp, tpp);
        tpp->_alive = true;
        sendPoint(tpp);
}


bool checkMove(NWTouchPoint *nwtpp, TouchPoint *tpp) {
        return tpp->_alive && (tpp->_type == POINT_BIRTH
            || abs(tpp->_touchPos.x - nwtpp->touchPos.x) > TOUCH_SLOPXY
            || abs(tpp->_touchPos.y - nwtpp->touchPos.y) > TOUCH_SLOPXY);
}

/**
 * Process a touch move that was received from the device.
 */
void processMove(NWTouchPoint *nwtpp, TouchPoint *tpp) {
    if (!checkMove(nwtpp, tpp))
        return;
    tpp->_type = POINT_MOVE;
    convertTouchToSparsh(nwtpp, tpp);
    sendPoint(tpp);
}

/**
 * Process a touch death that was received from the device.
 */
void processDeath(TouchPoint *tpp) {
    if (!tpp->_alive)
            return; 
    tpp->_type = POINT_DEATH;
    sendPoint(tpp);
    tpp->_alive = false;
}


/**
 * Receive the multi-touch information from the device
 */
void __stdcall ReceiveMultiTouchData(DWORD deviceID, DWORD deviceStatus, 
                                                                         DWORD packetID, DWORD touches, 
                                                                         DWORD ghostTouches) {

        if(deviceStatus == DS_TOUCH_INFO) {

                ULONGLONG time_now = getTimeNow();

/* testing ************** */

        cout << "received: " << time_now;
        for(int tch = 0; touches > 0 && tch < MAX_TOUCHES; tch++) {
            if(touches & (1 << tch)){
                cout << " " << tch;
            }
        }
        cout << " packetID=" << packetID << endl;

/* ***************testing */

                for(int tch = 0; touches > 0 && tch < MAX_TOUCHES; tch++) {
                        // "touches" contains a bitmask for present touch ids.
                        // If the bit is set then a touch with this ID exists.
                        if(touches & (1 << tch)){
                                //Get the touch information.
                                NWTouchPoint nwtp;
                                DWORD retCode = GetTouch(deviceID, packetID, &nwtp, (1 << tch), 0);
                                
                                if(retCode == SUCCESS){
                                        TouchPoint *tpp = touchPoints + tch;
                                        tpp->_timeReceived = time_now;
                                        time_last = time_now;
                                        switch(nwtp.touchEventType){
                                        case 1:
                                                processBirth(&nwtp, tpp);
                                                break;
                                        case 2:
                                                if (tpp->_alive)
                                                    processMove(&nwtp, tpp);
                                                else
                                                    processBirth(&nwtp, tpp);
                                                break;
                                        case 3:
                                                // basically useless, because in general
                                                // these come long after the finger was
                                                // lifted. Only sent when SOME finger is touching.
                                                processDeath(tpp);
                                                break;
                                        }
                                }
                        }
                }

/* testing ************** */

                cout << "touchpoints active:";
                for (int i = 0; i < MAX_TOUCHES; i++)
                if ((touchPoints + i)->_alive)
                        cout << " " << i;
                cout << endl;

/* ***************testing */

        }
}

/**
 * Thread responsible for killing touch points promptly after no moves are
 * received for that touch point.  This is really quite bad, but we have
 * no choice.  Modified here to only trigger on both fingers away.
 */
DWORD WINAPI TouchKiller(LPVOID lpParam) { 
        while(running) {
                Sleep(TOUCH_WAIT_TIME_MS);
                ULONGLONG time_now = getTimeNow();
                for (int tch = 0; tch < MAX_TOUCHES; tch++) {
                        TouchPoint *tpp = touchPoints + tch;
                        if (!tpp->_alive)
                            continue;
                        ULONGLONG dt = time_now - time_last;//tpp->_timeReceived;
                        if (dt > TOUCH_WAIT_TIME_MS) {
                             // Declare dead after about 50 ms.
                             if (time_now - tpp->_timeSent > (TOUCH_WAIT_TIME_MS<<1)) {
                                 // and update time if not just within 50 ms.
                                 tpp->_time = swapLongEndian(tpp->_timeReceived);
                             }
                             cout << "killing point " << tch << " after " << dt << " ms" << endl;
                             processDeath(tpp);
                        }
                }
        }
        return 0;
}

int main(int argc, char **argv) {
        //Get the number of connected devices.
        DWORD numDevices = GetConnectedDeviceCount();
        DWORD serialNum = 0;
        DWORD deviceID = 0;
        inittouchPoints();
        bool isOK = false;
        bool testing = (argc > 1 && ((string) argv[1]) == "-test");

        // If we have at least one connected device then try to connect to it.
        if(numDevices > 0) {
                // Get the serial number of the device which uniquely identifies the device.
                cout << "Getting device ID..." << endl;
                serialNum = GetConnectedDeviceID(0);
                
                // Initialize the device, passing in the serial number that uniquely
                // identifies it and the event handler for processing touch packets.
                cout << "Opening device and registering callback function..." << endl;
                deviceID = OpenDevice(serialNum, &ReceiveMultiTouchData);
                if(deviceID == 1)
                    isOK = true;
                else
                    cout << "Failed to connect to device, ID:  " << serialNum << endl;
                //SetKalmanFilterStatus(serialNum, true);
        } else {
                cout << "No valid devices are connected, Bob." << endl;
        }

        if (!isOK)
            return 0;

        // Set up the kill thread
        CreateThread(NULL, 0, TouchKiller, NULL, 0, NULL); 

        // Obtain the display info and the resolution.
        NWDisplayInfo displayInfo;
        DWORD retcode = GetConnectedDisplayInfo(0, &displayInfo);
        displayWidth = (int) displayInfo.displayRect.right;
        displayHeight = (int) displayInfo.displayRect.bottom;
        cout << "Display dimensions:  " << displayWidth << "x" << displayHeight << endl;

        DWORD displayMode = RM_MULTITOUCH; // same as RM_SLOPESMODE ?
        SetReportMode(deviceID, displayMode);

        haveSocket = (testing ? false : initSocket());

        if (!haveSocket && !testing) {
            cout << "No socket and no -test flag -- quitting" << endl;
            return 0;
        }

        cout << "Press ESC to Quit or I to re-initialize socket" << endl;

        // Enter run loop, exit on user hitting "Escape" key.
        while(running)      
        {
                if (_kbhit()) {
                    if (_getch()==0x1B)         
                        running = false;
                    else if (_getch()==0x49)         
                        haveSocket = initSocket();
                }
                if (running)
                    Sleep(200);
        }
        
        cout << "Closing connection to device..." << endl;

        // Close any open devices.
        CloseDevice(serialNum);
        
        //Reset the device connect/disconnect event handlers.
        SetConnectEventHandler(NULL);
        SetDisconnectEventHandler(NULL);

        return 0;
}
