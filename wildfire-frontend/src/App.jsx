
import { useEffect, useState } from "react";
import { MapContainer, TileLayer, CircleMarker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";

function MapEvents({setBounds}) {
  const map = useMapEvents({
    moveend: () => {
      const bounds = map.getBounds();

      setBounds({
        north: bounds.getNorth(),
        south: bounds.getSouth(),
        east: bounds.getEast(),
        west: bounds.getWest()
      });
    }
  });
  return null;
}

function App() {
  const [fires, setFires] = useState([]);
  const [selectedDate, setSelectedDate] = useState("ALL");
  const [bounds, setBounds] = useState({
    north: 90,
    south: -90,
    east: 180,
    west: -180
  });

  useEffect(() => {
    const controller = new AbortController();

    const params = new URLSearchParams({
      ...bounds,
      date: selectedDate,
      source: selectedSource
    });

    fetch (`/api/fires?${params}`, {signal: controller.signal})
      .then(response => {
        if (!response.ok) throw new Error("Failed to load fires");
        return response.json();
      })
      .then(data => setFires(data))
      .catch(error => {
        if (error.name !== "AbortError"){
          console.error(error);
        }
      });

      return () => controller.abort();
  }, [selectedDate, bounds, selectedSource]);


  return (
    <div>
      <h1>NASA Wildfire Dashboard</h1>
      <label>Select Date: </label>
      
      <input
        type = "date"
        value = {selectedDate}
        onChange = {(e) => setSelectedDate(e.target.value)}
      />

      <label> Satellite Source: </label>
      <select value={selectedSource} onChange={(e) => setSelectedSource(e.target.value)}
      >
      <option value="ALL">All Satellites</option>
      <option value="MODIS">MODIS</option>
      <option value="VIIRS">VIIRS</option>
      </select>

      <button onClick = {() => {setSelectedDate("")
                               setSelectedSource("ALL");
                               }}>
        Clear Filter
      </button>
      <p>Fire detections displayed: {fires.length}</p>

      <MapContainer
        center={[45, -75]}
        zoom={4}
        style={{ height: "80vh", width: "100%" }}
      >
        <MapEvents setBounds = {setBounds} />
        
      
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {fires.map((fire, index) => (
          <CircleMarker
            key={index}
            center={[fire.latitude, fire.longitude]}
            radius={3}
            pathOptions={{ color: fire.source === "MODIS" ? "red" : "blue" }}
          >
            <Popup>
              Source: {fire.source}
              <br />
              Detected: {fire.date}
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  );
}

export default App;
