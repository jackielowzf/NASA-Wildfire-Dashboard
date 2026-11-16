# FireWatch

A geospatial wildfire detection and satellite data comparison dashboard powered by NASA FIRMS.

## Overview

FireWatch is a full-stack web application designed to visualize, compare and spatially harmonize active fire detections from MODIS and VIIRS satellite instruments.

The application provides an interactive geographic map, filtering capabilities, statistical comparisons and historical analysis through a unified dashboard.

## Features

- Interactive wildfire detection map
- Geographic, date and satellite source filtering
- Raw satellite detection visualization
- Grid-based spatial harmonization
- Shared detection cell identification
- Spatial overlap analysis
- Satellite detection comparison charts
- Historical activity calendar
- Historical detection trends
- Automatic map centering
- Loading indicators and API error handling

## Technology Stack

**Frontend:** React, Vite, JavaScript, CSS, React Leaflet, Recharts

**Backend:** Java, Spring Boot, Maven, REST API

**Database:** MongoDB, Spring Data MongoDB

**Data Source:** NASA FIRMS

**Version Control:** Git and GitHub

## Architecture

FireWatch uses a layered monolithic backend with a separate React frontend. The frontend communicates with Spring Boot through REST API endpoints, while MongoDB stores imported satellite detection records.

## Harmonization Methodology

MODIS and VIIRS detections are assigned to a common 0.01-degree geographic grid. Each occupied cell stores detection counts for both instruments.

Cells are categorized as MODIS-only, VIIRS-only or shared.

Spatial overlap is calculated by dividing the number of shared cells by the total number of occupied cells.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/fires | Retrieve raw detections |
| GET | /api/harmonized | Retrieve harmonized grid cells |
| GET | /api/history | Retrieve historical detection statistics |
| GET | /api/test-db | Retrieve database document count |

## Running Locally

### Prerequisites

- JDK 21 or compatible newer JDK
- Node.js and npm
- MongoDB Community Server

### Backend

Place the MODIS and VIIRS CSV files in the backend's src/main/resources directory as modis.csv and viirs.csv.

Start MongoDB and run the Spring Boot application.

The backend runs on http://localhost:8080.

### Frontend

Navigate to the frontend directory and execute:

```bash
npm install
npm run dev
```

Open http://localhost:5173.

## Current Limitations

- The application uses imported CSV datasets rather than live satellite feeds.
- Raw detection visualization is limited to 500 records per request.
- The geographic grid uses fixed angular dimensions.
- Shared grid cells do not necessarily represent the same wildfire.
- Individual satellite detections do not necessarily represent unique wildfires.

## Future Improvements

- Live NASA FIRMS API integration
- Advanced spatial and temporal harmonization
- Geospatial database indexing
- Sensor confidence and fire radiative power analysis
- Cloud deployment

## Data Attribution

NASA Fire Information for Resource Management System (FIRMS):
https://firms.modaps.eosdis.nasa.gov/

Map data © OpenStreetMap contributors.

Developed as a NASA Space Apps Challenge project.