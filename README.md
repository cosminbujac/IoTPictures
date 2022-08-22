# IoTPictures
This project was meant to help me familiarize with the MQTT protocol used in IoT development. The developed app helps with getting live pictures from certain devices. The user has to set up one or more devices as a "Camera" and he acts as the "Supervisor", sending a request. The devices that act as a "Camera" take a picture and send it back to the supervisor, the latter getting to see all the responses.
The devices communicate using the MQTT protocol.

## Pre-requisites
-2 smartphones that operate using at least Android 8.1 

## Setup
To set up, follow the steps bellow:

1. [Optional]  You can edit the values of REQUEST_TOPIC, RESPONSE_TOPIC, and SERVER in the MQTTConstants file and make them of your own choices if you wish to use different Server / Topics and take advantage of the Prefill and Auto-config Options.
 
2. Clone the project and install the app on the desired devices.

3. Connect to the MQTT server. You can complete the fields with your own choices or you can use the Prefill Option. The later one facilitates the setup.

4. Subscribe the Camera device/s to the topic that you'll use to send the request.
   1. [Optional] Edit the value of RESPONSE_TOPIC in MQTTConstants to receive the "Success"
   2. [Optional] Use the auto-config option
5. Set up the Supervisor's topic to be the same as the one the Camera device/s is/are listening to
   1. [Optional] Use the auto-config option
7.  Send a request link and try it out!
 
## Demo
[(Watch the demo on YouTube)][demo-yt]



[demo-yt]: https://youtu.be/tUxqvn9rjVo
