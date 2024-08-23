# NIO-Survey-Stats-App

## Overview
This application has been developed for the **National Institute of Oceanography (NIO)**. The app interfaces with a GPS module via a USB serial port, receives data as NMEA strings, and displays formatted information like date-time, latitude, longitude, and altitude.

## Features

### 1. GPS Data Handling
- **NMEA String Parsing**: Parses NMEA sentences like `$GPGGA` and `$GPRMC` to extract latitude, longitude, altitude, and time.
- **Real-time Display**: Displays parsed GPS data in real-time.

### 2. Google Maps Integration
- **Map Display**: Shows the current GPS location on Google Maps.
- **Marker Placement**: Automatically places markers at the current location.
- **Area Calculation**: Calculates areas based on selected points.
- **Distance Measurement**: Measures distances between points.

### 3. Serial Terminal
- **Raw Data Viewing**: Views raw NMEA strings from the GPS module.
- **Pause/Resume Data Stream**: Allows pausing or resuming data streaming.
- **Clear Data**: Clears terminal data with a single click.

### 4. GPS Status Information
- **Valid Fix Indicator**: Indicates reliable GPS data reception.
- **Internal GPS Data**: Displays internal GPS data from the device.

### 5. Data Saving and Retrieval
- **Save GPS Data**: Saves parsed GPS data into a text file.
- **Data Export**: Accesses saved files from the Documents directory.

### 6. User Interface
- **Dynamic UI**: Updates the UI based on received GPS data.
- **Navigation Drawer**: Provides easy access to different app sections.

## Installation

1. Download the APK file from the provided link.
2. Transfer the APK to your Android device.
3. Install the APK on your device. Ensure installation from unknown sources is enabled.

## Usage

1. Connect the GPS module via USB OTG.
2. Grant necessary permissions.
3. Start or stop data streaming using the toggle button.
4. View and save the GPS data as needed.
5. To access the area and ruler tools disconnect the device using the connect/disconnect button.
6. the data taps are added ( 1, 2, 3, 4 ) these are the tags used to mark a real entity while surveying like tree, building etc.
7. The internal GPS terminal displays data from the mobile.
8. On clicking the resume button the raw data starts to display vice versa if clicked again (pause) and clear button clears the terminal.

## Additional Information

- **Documentation**: Access downloadable documentation at [Documentation Link](https://drive.google.com/file/d/1CNUzxTM7EQfK299h_iXMmiRnMkRl4JGR/view?usp=sharing).
- **References**: Utilizes the [Kai-Morich Simple USB Terminal](https://github.com/kai-morich/SimpleUsbTerminal) package.
- **Additional Reference**: The app also leverages the [USB-Serial-for-Android](https://github.com/mik3y/usb-serial-for-android) library.

---

**Developed by Tushar Raju Nagpure**
