import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line,
  Legend
} from "recharts";

import HistoryCalendar from "./HistoryCalendar";
import "./App.css";
import { useEffect, useState, useRef } from "react";

import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Rectangle,
  Popup,
  useMapEvents,
  useMap
} from "react-leaflet";

import "leaflet/dist/leaflet.css";


// Map event handling
function MapEvents({ setBounds }) {

  const map = useMapEvents({
    moveend: () => {
      updateBounds();
    }
  });

  function updateBounds() {
    const bounds = map.getBounds();

    setBounds({
      north: bounds.getNorth(),
      south: bounds.getSouth(),
      east: bounds.getEast(),
      west: bounds.getWest()
    });
  }

  useEffect(() => {
    const bounds = map.getBounds();

    setBounds({
      north: bounds.getNorth(),
      south: bounds.getSouth(),
      east: bounds.getEast(),
      west: bounds.getWest()
    });

  }, [map, setBounds]);

  return null;
}


function FitToDetections({ detections, loading, fitTrigger }) {

  const map = useMap();

  const hasFitted = useRef(false);
  const lastTrigger = useRef(0);

  useEffect(() => {

    if (loading || detections.length === 0) {
      return;
    }

    if (hasFitted.current && lastTrigger.current === fitTrigger) {
      return;
    }

    const coordinates = detections.map(detection => [
      detection.latitude,
      detection.longitude
    ]);

    hasFitted.current = true;
    lastTrigger.current = fitTrigger;

    map.fitBounds(coordinates, {
      padding: [40, 40],
      maxZoom: 10,
      animate: false
    });

  }, [map, detections, loading, fitTrigger]);

  return null;
}


// Main application
function App() {

  // State declarations
  const [fires, setFires] = useState([]);
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedSource, setSelectedSource] = useState("ALL");
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const [dataReady, setDataReady] = useState(false);

  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState("");

  const [bounds, setBounds] = useState({
    north: 90,
    south: -90,
    east: 180,
    west: -180
  });

  const [gridCells, setGridCells] = useState([]);
  const [viewMode, setViewMode] = useState("RAW");
  const [fitTrigger, setFitTrigger] = useState(0);


  // Fetch data from the backend
  useEffect(() => {
    if (!dataReady) return;

    const controller = new AbortController();

    setFires([]);
    setGridCells([]);
    setError("");

    if (viewMode === "GRID" && !selectedDate) {
      setLoading(false);
      return;
    }

    setLoading(true);

    const params = new URLSearchParams({
      ...bounds,
      date: selectedDate
    });

    let endpoint;

    if (viewMode === "RAW") {
      params.set("source", selectedSource);
      endpoint = "/api/fires";
    } else {
      endpoint = "/api/harmonized";
    }

    const timer = setTimeout(async () => {
      try {

        const response = await fetch(
          `${endpoint}?${params}`,
          { signal: controller.signal }
        );

        if (!response.ok) {
          throw new Error(`Server error: ${response.status}`);
        }

        const data = await response.json();

        if (controller.signal.aborted) return;

        if (viewMode === "RAW") {
          setFires(data);
        } else {
          setGridCells(data);
        }

      } catch (error) {

        if (!controller.signal.aborted) {
          setError("Unable to retrieve satellite data.");
          console.error(error);
        }

      } finally {

        if (!controller.signal.aborted) {
          setLoading(false);
        }

      }
    }, 300);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };

  }, [
    selectedDate,
    bounds,
    selectedSource,
    viewMode,
    retryCount,
    dataReady
  ]);


  useEffect(() => {

    fetch("/api/sync", {
      method: "POST"
    })
      .then(response => {
        if (!response.ok) {
          throw new Error("NASA synchronization unavailable");
        }
      })
      .catch(error => console.error(error))
      .finally(() => setDataReady(true));

  }, []);
  
  
  useEffect(() => {
    const controller = new AbortController();

    setHistoryLoading(true);
    setHistoryError("");
    if (!dataReady) return;

    fetch("/api/history", {
      signal: controller.signal
    })
      .then(response => {
        if (!response.ok) {
          throw new Error("Failed to load history");
        }

        return response.json();
      })
      .then(data => {
        if (!controller.signal.aborted) {
          setHistory(data);
        }
      })
      .catch(error => {
        if (!controller.signal.aborted) {
          setHistoryError("Unable to load historical data.");
          console.error(error);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setHistoryLoading(false);
        }
      });

    return () => controller.abort();

  }, [retryCount, dataReady]);


  // Determine grid cell colors
  function getGridColor(cell) {
    if (cell.shared) {
      return "#a855f7";
    }

    if (cell.modisCount > 0) {
      return "#ef4444";
    }

    return "#3b82f6";
  }


  // Statistics calculations
  const modisCount = viewMode === "RAW"
    ? fires.filter(fire => fire.source === "MODIS").length
    : gridCells.reduce((sum, cell) => sum + cell.modisCount, 0);

  const viirsCount = viewMode === "RAW"
    ? fires.filter(fire => fire.source === "VIIRS").length
    : gridCells.reduce((sum, cell) => sum + cell.viirsCount, 0);

  const totalDetections = modisCount + viirsCount;

  const sharedCells = gridCells.filter(
    cell => cell.shared
  ).length;

  const modisOnlyCells = gridCells.filter(
    cell => cell.modisCount > 0 && cell.viirsCount === 0
  ).length;

  const viirsOnlyCells = gridCells.filter(
    cell => cell.viirsCount > 0 && cell.modisCount === 0
  ).length;

  const overlapPercentage = gridCells.length > 0
    ? ((sharedCells / gridCells.length) * 100).toFixed(1)
    : "0.0";


  // Chart data
  const satelliteData = [
    { name: "MODIS", detections: modisCount },
    { name: "VIIRS", detections: viirsCount }
  ];

  const gridData = [
    { name: "MODIS Only", value: modisOnlyCells },
    { name: "VIIRS Only", value: viirsOnlyCells },
    { name: "Both Sensors", value: sharedCells }
  ];

  const colors = ["#ef4444", "#3b82f6", "#a855f7"];


  // User interface
  
  return (
    <div className="dashboard">

      {/* Dashboard header */}
      <header className="dashboard-header">

        <p className="eyebrow">
          NASA FIRMS DATA EXPLORER
        </p>

        <h1>FireWatch</h1>

        <p>
          MODIS & VIIRS Wildfire Intelligence Dashboard
        </p>

      </header>


      {/* Dashboard filters */}
      <div className="panel filters">

        <div className="filter-group">

          <label>Observation Date</label>

          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
          />

        </div>

        <div className="filter-group">

          <label>Satellite Source</label>

          <select
            value={selectedSource}
            onChange={(e) => setSelectedSource(e.target.value)}
            disabled={viewMode === "GRID"}
          >
            <option value="ALL">All Satellites</option>
            <option value="MODIS">MODIS</option>
            <option value="VIIRS">VIIRS</option>
          </select>

        </div>

        <div className="filter-group">

          <label>Visualization Mode</label>

          <select
            value={viewMode}
            onChange={(e) => setViewMode(e.target.value)}
          >
            <option value="RAW">Raw Detections</option>
            <option value="GRID">Harmonized Grid</option>
          </select>

        </div>

        <button
          className="clear-button"
          onClick={() => {
            setSelectedDate("");
            setSelectedSource("ALL");
          }}
        >
          Clear Filters
        </button>

      </div>

      
      {/* Application status */}
      <div className="status-area">

        {loading && (
          <div className="status-message loading-message">
            <span className="spinner" />
            Loading satellite detections...
          </div>
        )}

        {error && (
          <div className="status-message error-message">
            {error}

            <button
              className="retry-button"
              onClick={() => setRetryCount(count => count + 1)}
            >
              Retry
            </button>
          </div>
        )}

        {!loading && !error && (
          <>
            {viewMode === "GRID" && !selectedDate ? (
              <p>Please select a date to view harmonized data.</p>
            ) : totalDetections === 0 ? (
              <p>
                No detections found. Try another date or map area.
              </p>
            ) : (
              <p>
                {viewMode === "RAW"
                  ? `Fire detections displayed: ${fires.length}`
                  : `Grid cells displayed: ${gridCells.length}`}
              </p>
            )}
          </>
        )}

      </div>



      {/* Statistics cards */}
      <div className="stats-grid">

        <div className="stat-card">
          <h3>Total Detections</h3>
          <h2>{totalDetections}</h2>
        </div>

        <div className="stat-card">
          <h3>MODIS Detections</h3>
          <h2 style={{ color: "var(--modis)" }}>{modisCount}</h2>
        </div>

        <div className="stat-card">
          <h3>VIIRS Detections</h3>
          <h2 style={{ color: "var(--viirs)" }}>{viirsCount}</h2>
        </div>

        {viewMode === "GRID" && (
          <>
            <div className="stat-card">
              <h3>Shared Grid Cells</h3>
              <h2 style={{ color: "var(--shared)" }}>{sharedCells}</h2>
            </div>

            <div className="stat-card">
              <h3>Spatial Overlap</h3>
              <h2>{overlapPercentage}%</h2>
            </div>
          </>
        )}
      </div>

      {viewMode === "RAW" && (
        <small>
          Statistics are based on displayed detections
          (maximum 500).
        </small>
      )}

      <div className="panel map-panel">

        <div className="map-header">

          <h2>Geospatial Fire Detections</h2>

          <button
            className="fit-button"
            disabled={loading || totalDetections === 0}
            onClick={() => setFitTrigger(count => count + 1)}
          >
            Fit to Detections
          </button>

        </div>


      {/* Interactive map */}
      <MapContainer
        center={[15, -90]}
        zoom={5}
        preferCanvas={true}
        style={{ height: "65vh", width: "100%" }}
      >
        <MapEvents setBounds={setBounds} />
        <FitToDetections
          detections={viewMode === "RAW" ? fires : gridCells}
          loading={loading}
          fitTrigger={fitTrigger}
        />

        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />


        {/* Raw detection markers */}
        {viewMode === "RAW" && fires.map((fire, index) => (
          <CircleMarker
            key={fire.id || index}
            center={[fire.latitude, fire.longitude]}
            radius={3}
            pathOptions={{
              color: fire.source === "MODIS" ? "#ef4444" : "#3b82f6"
            }}
          >
            <Popup>
              Source: {fire.source}
              <br />
              Detected: {fire.date}
            </Popup>
          </CircleMarker>
        ))}


        {/* Harmonized grid rectangles */}
        {viewMode === "GRID" && gridCells.map((cell) => (
          <Rectangle
            key={`${cell.latitude},${cell.longitude}`}
            bounds={[
              [cell.latitude - 0.005, cell.longitude - 0.005],
              [cell.latitude + 0.005, cell.longitude + 0.005]
            ]}
            pathOptions={{
              color: getGridColor(cell),
              fillOpacity: 0.6,
              weight: 1
            }}
          >
            <Popup>
              MODIS: {cell.modisCount}
              <br />
              VIIRS: {cell.viirsCount}
              <br />
              Total detections: {cell.totalCount}
              <br />
              Shared: {cell.shared ? "Yes" : "No"}
            </Popup>
          </Rectangle>
        ))}

        </MapContainer>

        <div className="map-legend">

          <div className="legend-item">
            <span className="legend-dot modis-dot" />
            MODIS detections
          </div>

          <div className="legend-item">
            <span className="legend-dot viirs-dot" />
            VIIRS detections
          </div>

          {viewMode === "GRID" && (
            <div className="legend-item">
              <span className="legend-dot shared-dot" />
              Shared grid cells
            </div>
          )}

        </div>

      </div>


      {/* Data visualization charts */}
      <div className="charts-grid">

        {/* Satellite comparison bar chart */}
        <div className="panel chart-panel">
          <h2>Satellite Detection Comparison</h2>

          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={satelliteData}>
              <XAxis 
                dataKey="name"
                stroke="#94a3b8"
              />
              <YAxis stroke="#94a3b8" />
              <Tooltip
                contentStyle={{
                  backgroundColor: "#172235",
                  border: "1px solid #334155",
                  borderRadius: "8px",
                  color: "#f8fafc"
                }}
                labelStyle={{ color: "#f8fafc" }}
                itemStyle={{ color: "#f8fafc" }}
              />

              <Bar dataKey="detections">
                {satelliteData.map((entry, index) => (
                  <Cell
                    key={entry.name}
                    fill={colors[index]}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>


        {/* Grid distribution pie chart */}
        {viewMode === "GRID" && (
          <div className="panel chart-panel">
            <h2>Grid Cell Distribution</h2>

            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={gridData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius={100}
                  label
                >
                  {gridData.map((entry, index) => (
                    <Cell
                      key={entry.name}
                      fill={colors[index]}
                    />
                  ))}
                </Pie>

                <Tooltip
                  contentStyle={{
                    backgroundColor: "#172235",
                    border: "1px solid #334155",
                    borderRadius: "8px",
                    color: "#f8fafc"
                  }}
                  labelStyle={{ color: "#f8fafc" }}
                  itemStyle={{ color: "#f8fafc" }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}

      </div>

      {historyLoading && (
        <div className="status-message loading-message">
          Loading historical data...
        </div>
      )}

      {historyError && (
        <div className="status-message error-message">
          {historyError}

          <button
            className="retry-button"
            onClick={() => setRetryCount(count => count + 1)}
          >
            Retry
          </button>
        </div>
      )}

      {!historyLoading && !historyError && history.length === 0 && (
        <p className="muted-text">
          No historical observations available.
        </p>
      )}

      <HistoryCalendar
        history={history}
        selectedDate={selectedDate}
        setSelectedDate={setSelectedDate}
      />


      <div className="panel chart-panel">

        <h2>Historical Detection Trends</h2>

        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={history}>

            <XAxis dataKey="date" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip
              contentStyle={{
                backgroundColor: "#172235",
                border: "1px solid #334155",
                borderRadius: "8px",
                color: "#f8fafc"
              }}
              labelStyle={{ color: "#f8fafc" }}
              itemStyle={{ color: "#f8fafc" }}
            />
            <Legend />

            <Line
              type="monotone"
              dataKey="modisCount"
              name="MODIS"
              stroke="#ef4444"
              strokeWidth={2}
            />

            <Line
              type="monotone"
              dataKey="viirsCount"
              name="VIIRS"
              stroke="#3b82f6"
              strokeWidth={2}
            />

          </LineChart>
        </ResponsiveContainer>

      </div>


      <section className="panel">

        <h2>About Our Methodology</h2>

        <p className="muted-text">
          FireWatch integrates MODIS and VIIRS active fire
          detections from NASA FIRMS into a unified
          geographic visualization.
        </p>

        <p className="muted-text">
          Detections are assigned to a common 0.01-degree
          spatial grid to support comparison between
          satellite instruments.
        </p>

        <p className="muted-text">
          Shared grid cells indicate that both instruments
          recorded detections within the same geographic
          cell on the selected date.
        </p>

        <p className="muted-text">
          Spatial overlap represents the percentage of
          occupied grid cells containing detections from
          both instruments.
        </p>

        <p className="muted-text">
          Individual detections do not necessarily represent
          unique wildfires. Spatial overlap does not confirm
          that both instruments observed the same fire.
        </p>

      </section>

      <footer className="dashboard-footer">
        FireWatch | Powered by NASA FIRMS Data
      </footer>

    </div>
  );
}

export default App;
