# Project Data:

* all data will be placed under the Project Folder(named GripForce) which is located under the root of sdcard folder. 

* System SD Card Folder: /storage/sdCard0

* Removable SD Card Folder: /storage/extSdCard

* the app will try to search the Removable SD Card first. if not existed, it will use System SD Card.

# Data Naming and Format:
<ol> <li> For Grip Force Logs: </li>

* Naming: GripForce_{UserID}.txt<br/>

* Format:
{timestamp in milliseconds since experiment view loaded},{sensor strip 1 data point 1},{sensor strip 1 data point 2}, ... {sensor strip 1 data point n}\n
{timestamp in milliseconds since experiment view loaded},{sensor strip 2 data point 1} ... {sensor strip 2 data point n}\n
 ... 
{timestamp in milliseconds since experiment view loaded},{sensor strip m data point 1} ... {sensor strip m data point n}\n<br/>

* Path: GripForce/Logs<br/>

<li> For Tip Force Logs: </li>

* Naming: TipForce_{UserID}_{Grade}_{The order of a character in a file}.txt<br/>

* Format:{timestamp in milliseconds since experiment view loaded},{x coordinate},{y coordinate},{samsung note compatiable pen tip force}<br/>

* Path: GripForce/Logs<br/>

<li> Handwriting Images: </li>

* Naming: {UserID}_{Grade}_{The order of a character in a file}.png<br/>

* Path: GripForce/Images<br/>

<li> User Personal Information: </li>

* Naming: {UserID}.txt<br/>

* Path: GripForce/PersonalInformation<br/>

</ol>
# Testing Example Characters:

* Naming: Grade_{Grade}_Characters.txt

 * ex: Grade_1_Characters.txt means first grade

* Encoding: UTF-8

* Format: {Field Name} : {Field data}

* Path: GripForce/Example_Characters

# Other Configurable Setting:

- Chinese Characters Font:

 * The font of Chinese Example Characters in experiment page can be changed. Just put the font file(.ttf) under the path {System SD Card Path}/GripForce/ and renamed as "chinese_exp.ttf".

 * If you wish to use otf file , please refer to the file named ExperimentActivity.java and search "Typeface" modify the code and compile for your own purpose.   

# Troubleshooting:

1. If you want to reset user ID, use System Application Manager to clean the app setting

2. The features of hiding and showing system bar require the system permission. So the features are disabled if the android device haven't been rooted.

# Set up this project:(For Developers)

* you will need to add "libraryProject" under the root path of this project as a library project in order to successfully compile this project.

# Some issues or problem I have encountered

* SpenSurfaceView or SCanvas view can consume pretty much memory so I have encountered a Out Of Memory Exception during development when creating more than four instances. I suffered from this problem for a while until I found a solution from samsung forum which suggest adding a property  "android:largeHeap="true"" inside the application tag in the Manifest.xml.

* I haven't found a easy way to set background image for SpenSurfaceView or SCanvas, and due to the limit of my available time in this project, the problem remains unsolved.
