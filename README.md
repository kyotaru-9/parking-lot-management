# Parking Lot Management System

A Java-based desktop application for managing parking lot operations with a modern Swing GUI.

## Features

- **Parking Space Management**: Visual representation of 40 parking spaces
- **Vehicle Check-in/Check-out**: Record entry and exit times automatically
- **Real-time Dashboard**: Monitor available spaces, occupied spaces, and revenue
- **Parking History**: View complete parking records with search functionality
- **Revenue Analytics**: Track daily, weekly, and monthly revenue statistics
- **Customizable Rates**: Configure hourly parking rates by vehicle type
- **Dark Mode**: Toggle between light and dark themes
- **Data Persistence**: Automatic saving of parking data, rates, and revenue

## Project Structure

```
Parking Lot System/
├── ParkingSystem.java    # Main application source code
├── data/
│   ├── lot.txt          # Parking lot layout data
│   ├── parked.txt       # Currently parked vehicles
│   ├── parkingrate.txt  # Hourly parking rates
│   └── revenue.txt      # Revenue records
└── img/                 # Image assets (if any)
```

## Requirements

- Java JDK 8 or higher
- Swing (included in standard JDK)

## How to Run

1. Compile the Java file:
   ```bash
   javac ParkingSystem.java
   ```

2. Run the application:
   ```bash
   java ParkingSystem
   ```

## Usage

1. **Park a Vehicle**: Click on an available (green) parking space and enter the license plate
2. **Unpark a Vehicle**: Click on an occupied (red) space to check out and calculate fare
3. **View History**: Navigate to the History tab to see all parking records
4. **Check Revenue**: View revenue statistics in the Revenue dashboard
5. **Configure Rates**: Adjust hourly rates in the Settings panel

## Data Files

The application automatically manages the following data files in the `data/` directory:

- **lot.txt**: Stores parking space states
- **parked.txt**: Tracks currently parked vehicles with entry times
- **parkingrate.txt**: Configurable hourly rates for different vehicle types
- **revenue.txt**: Daily revenue totals

## License

This project is open source and available for personal and educational use.
