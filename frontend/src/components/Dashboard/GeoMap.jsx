import { useEffect } from 'react';
import {
  CircleMarker,
  MapContainer,
  Popup,
  TileLayer,
  useMap,
} from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function MapViewport({ locations }) {
  const map = useMap();

  useEffect(() => {
    const positions = locations.map((point) => [point.latitude, point.longitude]);
    const resizeTimer = window.setTimeout(() => map.invalidateSize(), 0);

    if (positions.length === 1) {
      map.setView(positions[0], 5, { animate: false });
    } else if (positions.length > 1) {
      map.fitBounds(positions, { padding: [44, 44], maxZoom: 5, animate: false });
    }

    return () => window.clearTimeout(resizeTimer);
  }, [locations, map]);

  return null;
}

export default function GeoMap({ locations }) {
  return (
    <MapContainer
      center={[20, 0]}
      zoom={2}
      minZoom={2}
      maxZoom={12}
      scrollWheelZoom
      className="geo-leaflet-map"
      aria-label="Interactive map of active player devices"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapViewport locations={locations} />

      {locations.map((point) => (
        <CircleMarker
          key={`${point.keyId}-${point.hardwareId}`}
          center={[point.latitude, point.longitude]}
          radius={9}
          pathOptions={{
            color: '#ffffff',
            weight: 3,
            fillColor: '#4f46e5',
            fillOpacity: 0.92,
          }}
        >
          <Popup className="geo-map-popup">
            <div className="geo-popup-content">
              <strong>{[point.city, point.country].filter(Boolean).join(', ')}</strong>
              <span>{point.productName}</span>
              <span>{point.customerName || 'Unassigned license'}</span>
              <code>{point.ipAddress}</code>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
