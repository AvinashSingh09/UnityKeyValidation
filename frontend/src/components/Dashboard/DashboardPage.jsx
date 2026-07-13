import { useState, useEffect } from 'react';
import { getDashboard, getGeography } from '../../api/analyticsApi';
import { getProducts, createProduct } from '../../api/productApi';
import { getKeys, createKey } from '../../api/keyApi';
import { useAuth } from '../../context/AuthContext';
import { formatDate } from '../../utils/dateUtils';
import toast from 'react-hot-toast';
import {
  ResponsiveContainer,
  ScatterChart,
  Scatter,
  CartesianGrid,
  XAxis,
  YAxis,
  ZAxis,
  Tooltip,
} from 'recharts';
import {
  HiOutlineKey,
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineXCircle,
  HiOutlineCube,
  HiOutlineShieldCheck,
  HiOutlineExclamationTriangle,
  HiOutlinePauseCircle,
  HiOutlineBookOpen,
  HiOutlineCommandLine,
  HiOutlineClipboard,
  HiOutlineMagnifyingGlass,
  HiOutlinePlus,
  HiOutlineArrowTopRightOnSquare,
  HiOutlineArrowPath,
  HiOutlineMapPin,
  HiOutlineGlobeAlt,
} from 'react-icons/hi2';
import './DashboardPage.css';

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [products, setProducts] = useState([]);
  const [keys, setKeys] = useState([]);
  const [geography, setGeography] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Search filters
  const [productSearch, setProductSearch] = useState('');
  const [keySearch, setKeySearch] = useState('');

  // Modals / Overlays
  const [activeGuideTab, setActiveGuideTab] = useState('unity');
  const [showGuideModal, setShowGuideModal] = useState(false);
  const [copiedText, setCopiedText] = useState('');

  // Quick modals for inline creation
  const [showProductModal, setShowProductModal] = useState(false);
  const [newProductForm, setNewProductForm] = useState({ name: '', productCode: '', description: '', version: '1.0.0' });
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [newKeyForm, setNewKeyForm] = useState({ productId: '', customerName: '', customerEmail: '', type: 'TIME_LIMITED', maxActivations: 1, validUntil: '' });

  const { user, isAdmin } = useAuth();

  useEffect(() => {
    loadAllData();
  }, []);

  const loadAllData = async () => {
    setLoading(true);
    try {
      await Promise.all([fetchStats(), fetchRecentProducts(), fetchRecentKeys(), fetchGeography()]);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await Promise.all([fetchStats(), fetchRecentProducts(), fetchRecentKeys(), fetchGeography()]);
      toast.success('Dashboard data refreshed');
    } catch (err) {
      toast.error('Failed to refresh data');
    } finally {
      setRefreshing(false);
    }
  };

  const fetchStats = async () => {
    const { data } = await getDashboard();
    setStats(data);
  };

  const fetchRecentProducts = async () => {
    const { data } = await getProducts();
    setProducts(data || []);
  };

  const fetchRecentKeys = async () => {
    const { data } = await getKeys({ page: 0, size: 5 });
    setKeys(data.content || []);
  };

  const fetchGeography = async () => {
    const { data } = await getGeography();
    setGeography(data);
  };

  const GeoTooltip = ({ active, payload }) => {
    if (!active || !payload?.length) return null;
    const point = payload[0].payload;
    const place = [point.city, point.region, point.country].filter(Boolean).join(', ');
    return (
      <div className="geo-tooltip">
        <strong>{place || 'Unknown location'}</strong>
        <span>{point.productName} · {point.customerName || 'Unassigned key'}</span>
        <span className="mono">{point.ipAddress}</span>
        <span>{formatDate(point.lastSeenAt)}</span>
      </div>
    );
  };

  const handleCreateProduct = async (e) => {
    e.preventDefault();
    try {
      await createProduct(newProductForm);
      toast.success('Product created successfully');
      setShowProductModal(false);
      setNewProductForm({ name: '', productCode: '', description: '', version: '1.0.0' });
      loadAllData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create product');
    }
  };

  const handleCreateKey = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...newKeyForm,
        validUntil: newKeyForm.validUntil ? new Date(newKeyForm.validUntil).toISOString() : null,
      };
      const { data } = await createKey(payload);
      toast.success('License key created successfully');
      navigator.clipboard.writeText(data.key);
      toast.success('Copied key to clipboard');
      setShowKeyModal(false);
      setNewKeyForm({ productId: '', customerName: '', customerEmail: '', type: 'TIME_LIMITED', maxActivations: 1, validUntil: '' });
      loadAllData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to generate key');
    }
  };

  const copyToClipboard = (text, type = 'Key') => {
    navigator.clipboard.writeText(text);
    setCopiedText(text);
    toast.success(`${type} copied to clipboard`);
    setTimeout(() => setCopiedText(''), 2000);
  };

  const getGreeting = () => {
    const hrs = new Date().getHours();
    if (hrs < 12) return 'Good morning';
    if (hrs < 17) return 'Good afternoon';
    return 'Good evening';
  };

  const codeSnippets = {
    unity: `using UnityEngine;
using KeyVault.SDK;

public class GameLicensing : MonoBehaviour
{
    void Start()
    {
        // 1. Configure the KeyValidator
        KeyValidator.Instance.config.serverUrl = "http://localhost:8000/api";
        KeyValidator.Instance.config.productCode = "STEELVR-001";
        KeyValidator.Instance.config.heartbeatIntervalSeconds = 300f; // 5 min checks

        // 2. Register for license updates
        KeyValidator.Instance.OnLicenseStateChanged += (isLicensed, reason) => {
            if (isLicensed) {
                Debug.Log("License verified. Launching application...");
            } else {
                Debug.LogError($"Access denied: {reason}. Disabling features.");
                // Disable gameplay or exit application
            }
        };
    }
}`,
    web: `// Validate license key directly via REST call
const response = await fetch('http://localhost:8000/api/validate/check', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    key: "A1B2C-D3E4F-G5H6I-J7K8L",
    productCode: "STEELVR-001",
    hardwareId: "unique-device-fingerprint-hash"
  })
});

const data = await response.json();
console.log("Is key valid?", data.valid);`,
    fingerprint: `// Generate hardware ID on custom platforms
public static string GenerateFingerprint()
{
    string cpu = SystemInfo.processorType;
    string gpu = SystemInfo.graphicsDeviceName;
    string os = SystemInfo.operatingSystem;
    string uuid = SystemInfo.deviceUniqueIdentifier;
    
    return ComputeSHA256(cpu + gpu + os + uuid);
}`
  };

  if (loading) {
    return <div className="loader"><div className="spinner" /></div>;
  }

  // Filtered products and keys
  const filteredProducts = products.filter(p =>
    p.name.toLowerCase().includes(productSearch.toLowerCase()) ||
    p.productCode.toLowerCase().includes(productSearch.toLowerCase())
  );

  const filteredKeys = keys.filter(k =>
    k.key.toLowerCase().includes(keySearch.toLowerCase()) ||
    (k.customerName && k.customerName.toLowerCase().includes(keySearch.toLowerCase()))
  );

  // Stats definition for high fidelity cards
  const statCards = [
    { label: 'Total Keys', value: stats?.totalKeys || 0, icon: <HiOutlineKey />, color: 'rgba(99, 102, 241, 1)', shadow: 'rgba(99, 102, 241, 0.25)' },
    { label: 'Active Keys', value: stats?.activeKeys || 0, icon: <HiOutlineCheckCircle />, color: 'rgba(16, 185, 129, 1)', shadow: 'rgba(16, 185, 129, 0.25)' },
    { label: 'Expired Keys', value: stats?.expiredKeys || 0, icon: <HiOutlineClock />, color: 'rgba(245, 158, 11, 1)', shadow: 'rgba(245, 158, 11, 0.25)' },
    { label: 'Validations Today', value: stats?.validationsToday || 0, icon: <HiOutlineShieldCheck />, color: 'rgba(6, 182, 212, 1)', shadow: 'rgba(6, 182, 212, 0.25)' }
  ];



  return (
    <div className="dashboard-v2">
      {/* Top Welcome Bar */}
      <div className="welcome-banner">
        <div className="welcome-info">
          <span className="badge badge-primary welcome-pill">KeyVault Pro v1.0</span>
          <h2>{getGreeting()}, {user?.fullName || 'Avinash'}!</h2>
          <p>Here is your license platform's activity overview. Connected to database cluster.</p>
        </div>
        <div className="welcome-actions">
          <button onClick={handleRefresh} disabled={refreshing} className="btn btn-secondary btn-icon" title="Refresh metrics">
            <HiOutlineArrowPath className={refreshing ? 'spin' : ''} />
          </button>
          <button onClick={() => setShowGuideModal(true)} className="btn btn-secondary">
            <HiOutlineBookOpen /> Integration Guide
          </button>
          {isAdmin() && (
            <button onClick={() => setShowKeyModal(true)} className="btn btn-primary">
              <HiOutlinePlus /> Quick Generate
            </button>
          )}
        </div>
      </div>

      {/* Main Metrics Card Grid */}
      <div className="stats-v2-grid">
        {statCards.map((card) => (
          <div key={card.label} className="metric-card card" style={{ '--card-glow': card.shadow }}>
            <div className="metric-card-glow-bg" />
            <div className="metric-content">
              <span className="metric-label">{card.label}</span>
              <h3 className="metric-value">{card.value.toLocaleString()}</h3>
            </div>
            <div className="metric-icon" style={{ background: `${card.color}15`, color: card.color }}>
              {card.icon}
            </div>
          </div>
        ))}
      </div>

      {/* Split Side-by-Side: Products & Customers/Keys (like Cryptolens Dashboard) */}
      <div className="split-layout">
        {/* Column 1: Products */}
        <div className="card split-panel">
          <div className="panel-header">
            <div className="panel-title-area">
              <HiOutlineCube className="panel-icon text-accent" />
              <div>
                <h3>Products & Builds</h3>
                <p>Register Unity builds and products</p>
              </div>
            </div>
            {isAdmin() && (
              <button onClick={() => setShowProductModal(true)} className="btn btn-secondary btn-sm">
                <HiOutlinePlus /> Create Product
              </button>
            )}
          </div>

          <div className="panel-search">
            <HiOutlineMagnifyingGlass className="search-icon" />
            <input
              type="text"
              placeholder="Search products..."
              className="input"
              value={productSearch}
              onChange={(e) => setProductSearch(e.target.value)}
            />
          </div>

          <div className="panel-list">
            {filteredProducts.length === 0 ? (
              <div className="empty-panel">
                <p>No products found matching filter</p>
              </div>
            ) : (
              filteredProducts.map((p) => (
                <div key={p.id} className="panel-list-item hover-lift">
                  <div className="item-meta">
                    <span className="item-title">{p.name}</span>
                    <span className="item-subtitle mono">{p.productCode}</span>
                  </div>
                  <div className="item-right">
                    <span className="version-pill">v{p.version || '1.0.0'}</span>
                    <span className={`badge ${p.active ? 'badge-active' : 'badge-revoked'}`}>
                      {p.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Column 2: License Keys (Customers) */}
        <div className="card split-panel">
          <div className="panel-header">
            <div className="panel-title-area">
              <HiOutlineKey className="panel-icon text-primary" />
              <div>
                <h3>Recent Licenses & Seats</h3>
                <p>Track activated machines and licenses</p>
              </div>
            </div>
            {isAdmin() && (
              <button onClick={() => setShowKeyModal(true)} className="btn btn-secondary btn-sm">
                <HiOutlinePlus /> Generate Key
              </button>
            )}
          </div>

          <div className="panel-search">
            <HiOutlineMagnifyingGlass className="search-icon" />
            <input
              type="text"
              placeholder="Search keys, clients..."
              className="input"
              value={keySearch}
              onChange={(e) => setKeySearch(e.target.value)}
            />
          </div>

          <div className="panel-list">
            {filteredKeys.length === 0 ? (
              <div className="empty-panel">
                <p>No licenses found matching filter</p>
              </div>
            ) : (
              filteredKeys.map((k) => (
                <div key={k.id} className="panel-list-item hover-lift">
                  <div className="item-meta">
                    <div className="flex items-center gap-sm">
                      <span className="key-code-pill mono">{k.key}</span>
                      <button
                        onClick={() => copyToClipboard(k.key, 'Key')}
                        className="copy-btn-inline"
                        title="Copy key"
                      >
                        <HiOutlineClipboard />
                      </button>
                    </div>
                    <span className="item-subtitle">{k.customerName || 'No Client Info'}</span>
                  </div>
                  <div className="item-right flex-col items-end">
                    <span className={`badge ${k.status === 'ACTIVE' ? 'badge-active' : 'badge-expired'}`}>
                      {k.status}
                    </span>
                    <span className="seat-fraction mt-xs">{k.currentActivations}/{k.maxActivations} Seats</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>


      {/* Geographic activity */}
      <section className="geo-section card">
        <div className="geo-header">
          <div className="panel-title-area">
            <HiOutlineGlobeAlt className="panel-icon text-accent" />
            <div>
              <h3>Player Geography</h3>
              <p>Last known public IP location for active devices</p>
            </div>
          </div>
          <div className="geo-totals">
            <span><strong>{geography?.locatedDevices || 0}</strong> located devices</span>
            <span><strong>{geography?.countries || 0}</strong> countries</span>
          </div>
        </div>

        {!geography?.locations?.length ? (
          <div className="geo-empty">
            <HiOutlineMapPin />
            <div>
              <strong>No public locations recorded yet</strong>
              <p>Locations appear after a Unity client activates or validates from a public IP.</p>
            </div>
          </div>
        ) : (
          <div className="geo-grid">
            <div className="geo-chart" aria-label="World coordinate plot of active devices">
              <ResponsiveContainer width="100%" height={330}>
                <ScatterChart margin={{ top: 18, right: 18, bottom: 8, left: 0 }}>
                  <defs>
                    <radialGradient id="geoDot" cx="50%" cy="50%" r="50%">
                      <stop offset="0%" stopColor="#a5b4fc" stopOpacity={1} />
                      <stop offset="100%" stopColor="#6366f1" stopOpacity={0.55} />
                    </radialGradient>
                  </defs>
                  <CartesianGrid stroke="rgba(148, 163, 184, 0.12)" strokeDasharray="3 6" />
                  <XAxis type="number" dataKey="longitude" domain={[-180, 180]} ticks={[-180, -120, -60, 0, 60, 120, 180]} tickFormatter={(v) => `${Math.abs(v)}°${v < 0 ? 'W' : v > 0 ? 'E' : ''}`} tick={{ fill: '#778096', fontSize: 10 }} axisLine={false} tickLine={false} />
                  <YAxis type="number" dataKey="latitude" domain={[-90, 90]} ticks={[-90, -60, -30, 0, 30, 60, 90]} tickFormatter={(v) => `${Math.abs(v)}°${v < 0 ? 'S' : v > 0 ? 'N' : ''}`} tick={{ fill: '#778096', fontSize: 10 }} axisLine={false} tickLine={false} width={38} />
                  <ZAxis range={[90, 90]} />
                  <Tooltip content={<GeoTooltip />} cursor={{ strokeDasharray: '3 3' }} />
                  <Scatter data={geography.locations} fill="url(#geoDot)" />
                </ScatterChart>
              </ResponsiveContainer>
            </div>

            <div className="geo-side-list">
              <h4>Top countries</h4>
              {(geography.countrySummary || []).slice(0, 6).map((country) => (
                <div className="country-row" key={country.countryCode}>
                  <span className="country-code">{country.countryCode}</span>
                  <span className="country-name">{country.country}</span>
                  <strong>{country.devices}</strong>
                </div>
              ))}
              <h4 className="recent-title">Recently active</h4>
              {(geography.locations || []).slice(0, 4).map((point) => (
                <div className="recent-location" key={`${point.keyId}-${point.hardwareId}`}>
                  <HiOutlineMapPin />
                  <div>
                    <strong>{[point.city, point.country].filter(Boolean).join(', ')}</strong>
                    <span>{point.productName} · <span className="mono">{point.ipAddress}</span></span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
        <p className="geo-accuracy-note">Geo-IP locations are approximate and may reflect a VPN, carrier, or ISP gateway.</p>
      </section>



      {/* Quick Integration / Tools Panel */}
      <div className="integrations-section mt-lg">
        <div className="section-header">
          <h3>Integrations & SDK Options</h3>
          <p>Drop-in guides for verifying license keys in Unity builds and platforms</p>
        </div>
        <div className="integrations-grid">
          <div className="card integration-card" onClick={() => { setActiveGuideTab('unity'); setShowGuideModal(true); }}>
            <HiOutlineCommandLine className="card-icon text-primary" />
            <h4>Unity C# Integration</h4>
            <p>Deploy KeyValidator.cs to check keys, fetch client status, and update machine registry.</p>
            <span className="learn-more-btn">Read setup code <HiOutlineArrowTopRightOnSquare /></span>
          </div>

          <div className="card integration-card" onClick={() => { setActiveGuideTab('web'); setShowGuideModal(true); }}>
            <HiOutlineCommandLine className="card-icon text-accent" />
            <h4>Web REST API Calls</h4>
            <p>Direct HTTP POST requests to activate, validate or request seat release on nodes.</p>
            <span className="learn-more-btn">View JSON structure <HiOutlineArrowTopRightOnSquare /></span>
          </div>

          <div className="card integration-card" onClick={() => { setActiveGuideTab('fingerprint'); setShowGuideModal(true); }}>
            <HiOutlineShieldCheck className="card-icon text-success" />
            <h4>Machine Fingerprint</h4>
            <p>Integrate CPU, OS, and GPU descriptors into SHA256 fingerprints to secure keys.</p>
            <span className="learn-more-btn">Check class methods <HiOutlineArrowTopRightOnSquare /></span>
          </div>
        </div>
      </div>

      {/* Integration Guide Modal */}
      {showGuideModal && (
        <div className="modal-overlay" onClick={() => setShowGuideModal(false)}>
          <div className="modal modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2><HiOutlineBookOpen /> Integration Guide</h2>
              <button onClick={() => setShowGuideModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            
            <div className="guide-tabs">
              <button onClick={() => setActiveGuideTab('unity')} className={`tab-btn ${activeGuideTab === 'unity' ? 'active' : ''}`}>Unity Component</button>
              <button onClick={() => setActiveGuideTab('web')} className={`tab-btn ${activeGuideTab === 'web' ? 'active' : ''}`}>Web API Endpoints</button>
              <button onClick={() => setActiveGuideTab('fingerprint')} className={`tab-btn ${activeGuideTab === 'fingerprint' ? 'active' : ''}`}>Hardware Fingerprinting</button>
            </div>

            <div className="guide-content mt-md">
              <div className="code-box-header">
                <span className="mono">Code Snippet</span>
                <button className="btn btn-ghost btn-sm" onClick={() => copyToClipboard(codeSnippets[activeGuideTab], 'Code')}>
                  <HiOutlineClipboard /> Copy
                </button>
              </div>
              <pre className="code-display">
                <code>{codeSnippets[activeGuideTab]}</code>
              </pre>
            </div>
            
            <div className="modal-footer">
              <button onClick={() => setShowGuideModal(false)} className="btn btn-primary">Done</button>
            </div>
          </div>
        </div>
      )}

      {/* Add Product Modal */}
      {showProductModal && (
        <div className="modal-overlay" onClick={() => setShowProductModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add Product</h2>
              <button onClick={() => setShowProductModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            <form onSubmit={handleCreateProduct}>
              <div className="input-group">
                <label>Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Virtual Classroom Simulator"
                  className="input"
                  value={newProductForm.name}
                  onChange={(e) => setNewProductForm({...newProductForm, name: e.target.value})}
                />
              </div>
              <div className="input-group mt-md">
                <label>Product Code</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. VCLASS-VR-01"
                  className="input mono"
                  value={newProductForm.productCode}
                  onChange={(e) => setNewProductForm({...newProductForm, productCode: e.target.value.toUpperCase()})}
                />
              </div>
              <div className="input-group mt-md">
                <label>Version</label>
                <input
                  type="text"
                  placeholder="1.0.0"
                  className="input"
                  value={newProductForm.version}
                  onChange={(e) => setNewProductForm({...newProductForm, version: e.target.value})}
                />
              </div>
              <div className="input-group mt-md">
                <label>Description</label>
                <input
                  type="text"
                  placeholder="Enter details..."
                  className="input"
                  value={newProductForm.description}
                  onChange={(e) => setNewProductForm({...newProductForm, description: e.target.value})}
                />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowProductModal(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary">Save Product</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Generate Key Modal */}
      {showKeyModal && (
        <div className="modal-overlay" onClick={() => setShowKeyModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Generate License Key</h2>
              <button onClick={() => setShowKeyModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            <form onSubmit={handleCreateKey}>
              <div className="input-group">
                <label>Target Product</label>
                <select
                  required
                  className="select"
                  value={newKeyForm.productId}
                  onChange={(e) => setNewKeyForm({...newKeyForm, productId: e.target.value})}
                >
                  <option value="">Select product...</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              </div>
              <div className="input-group mt-md">
                <label>Customer Name</label>
                <input
                  type="text"
                  placeholder="Customer organization / name"
                  className="input"
                  value={newKeyForm.customerName}
                  onChange={(e) => setNewKeyForm({...newKeyForm, customerName: e.target.value})}
                />
              </div>
              <div className="input-group mt-md">
                <label>Customer Email</label>
                <input
                  type="email"
                  placeholder="name@customer.com"
                  className="input"
                  value={newKeyForm.customerEmail}
                  onChange={(e) => setNewKeyForm({...newKeyForm, customerEmail: e.target.value})}
                />
              </div>
              <div className="form-row mt-md">
                <div className="input-group">
                  <label>Type</label>
                  <select
                    className="select"
                    value={newKeyForm.type}
                    onChange={(e) => setNewKeyForm({...newKeyForm, type: e.target.value})}
                  >
                    <option value="TIME_LIMITED">Time Limited</option>
                    <option value="PERPETUAL">Perpetual</option>
                    <option value="TRIAL">Trial</option>
                  </select>
                </div>
                <div className="input-group">
                  <label>Max Seats</label>
                  <input
                    type="number"
                    min="1"
                    className="input"
                    value={newKeyForm.maxActivations}
                    onChange={(e) => setNewKeyForm({...newKeyForm, maxActivations: parseInt(e.target.value) || 1})}
                  />
                </div>
              </div>
              <div className="input-group mt-md">
                <label>Expiry Date</label>
                <input
                  type="datetime-local"
                  className="input"
                  value={newKeyForm.validUntil}
                  onChange={(e) => setNewKeyForm({...newKeyForm, validUntil: e.target.value})}
                />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowKeyModal(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary">Generate & Copy</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
