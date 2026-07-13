import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  HiOutlineArrowLeft, HiOutlineChartBarSquare, HiOutlineCheckCircle,
  HiOutlineClipboard, HiOutlineComputerDesktop, HiOutlineCube,
  HiOutlineExclamationTriangle, HiOutlineKey, HiOutlineMapPin,
} from 'react-icons/hi2';
import { getProductOverview } from '../../api/productApi';
import { formatDate, formatDateTime, timeRemaining } from '../../utils/dateUtils';
import './ProductDetailPage.css';

const keyBadge = { ACTIVE: 'badge-active', EXPIRED: 'badge-expired', REVOKED: 'badge-revoked', SUSPENDED: 'badge-suspended' };

function OverviewMetric({ icon, label, value, note, tone = '' }) {
  return <article className="product-overview-metric card"><span className={tone}>{icon}</span><div><small>{label}</small><strong>{value}</strong><p>{note}</p></div></article>;
}

export default function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await getProductOverview(id);
        setOverview(data);
      } catch (error) {
        toast.error(error.response?.data?.message || 'Failed to load product details');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  const statusSegments = useMemo(() => {
    if (!overview?.totalKeys) return [];
    return [['Active', overview.activeKeys, 'active'], ['Suspended', overview.suspendedKeys, 'suspended'], ['Expired', overview.expiredKeys, 'expired'], ['Revoked', overview.revokedKeys, 'revoked']].filter((item) => item[1] > 0);
  }, [overview]);

  const copyCode = async () => {
    await navigator.clipboard.writeText(overview.product.productCode);
    toast.success('Product code copied');
  };

  if (loading) return <div className="loader"><div className="spinner" /></div>;
  if (!overview) return <div className="card empty-state"><h3>Product not found</h3><Link to="/products">Return to products</Link></div>;

  const product = overview.product;
  return <div className="product-detail-page">
    <Link to="/products" className="product-detail-back"><HiOutlineArrowLeft /> Products</Link>

    <header className="product-detail-hero card">
      <div className="product-detail-identity"><span className="product-detail-icon"><HiOutlineCube /></span><div>
        <div className="product-detail-title"><h1>{product.name}</h1><span className={`badge ${product.active ? 'badge-active' : 'badge-revoked'}`}>{product.active ? 'Active' : 'Inactive'}</span></div>
        <div className="product-detail-code"><code>{product.productCode}</code><button className="btn btn-ghost btn-icon btn-sm" onClick={copyCode} title="Copy product code"><HiOutlineClipboard /></button></div>
        <p>{product.description || 'No product description has been added.'}</p>
      </div></div>
      <div className="product-detail-actions"><Link className="btn btn-secondary" to={`/keys?productId=${product.id}`}><HiOutlineKey /> View all licenses</Link></div>
    </header>

    <section className="product-overview-metrics">
      <OverviewMetric icon={<HiOutlineKey />} label="License keys" value={overview.totalKeys} note={`${overview.activeKeys} currently active`} />
      <OverviewMetric icon={<HiOutlineComputerDesktop />} label="Activated devices" value={overview.activeDevices} note={`${overview.locatedDevices} with Geo-IP location`} tone="metric-info" />
      <OverviewMetric icon={<HiOutlineCheckCircle />} label="Validation success · 30d" value={`${overview.successRate30d.toFixed(1)}%`} note={`${overview.validations30d} attempts · ${overview.failedValidations30d} failed`} tone="metric-success" />
      <OverviewMetric icon={<HiOutlineExclamationTriangle />} label="Expiring · 30d" value={overview.expiringIn30Days} note={overview.expiringIn30Days ? 'Licenses requiring attention' : 'No upcoming expirations'} tone={overview.expiringIn30Days ? 'metric-warning' : 'metric-success'} />
    </section>

    <div className="product-detail-grid">
      <section className="product-detail-panel card product-license-panel">
        <div className="product-panel-heading"><div><h2>License inventory</h2><p>Recently created licenses for this product.</p></div><Link to={`/keys?productId=${product.id}`}>View all</Link></div>
        {overview.recentKeys.length === 0 ? <div className="product-detail-empty"><HiOutlineKey /><strong>No licenses yet</strong><span>Generate a key to begin tracking usage.</span></div> : <div className="table-wrapper"><table className="table"><thead><tr><th>License</th><th>Customer</th><th>Status</th><th>Seats</th><th>Expires</th></tr></thead><tbody>{overview.recentKeys.map((license) => <tr key={license.id} className="clickable-product-row" onClick={() => navigate(`/keys/${license.id}`)}><td><code>{license.key}</code></td><td>{license.customerName || 'Unassigned'}</td><td><span className={`badge ${keyBadge[license.status] || ''}`}>{license.status}</span></td><td><strong>{license.currentActivations}</strong> / {license.maxActivations}</td><td><span>{formatDate(license.validUntil)}</span><small>{timeRemaining(license.validUntil)}</small></td></tr>)}</tbody></table></div>}
      </section>

      <aside className="product-detail-panel card product-info-panel">
        <div className="product-panel-heading"><div><h2>Project information</h2><p>Configuration used by the Unity client.</p></div></div>
        <dl><div><dt>Product code</dt><dd><code>{product.productCode}</code></dd></div><div><dt>Version</dt><dd>{product.version ? `v${product.version}` : 'Not specified'}</dd></div><div><dt>Status</dt><dd>{product.active ? 'Active' : 'Inactive'}</dd></div><div><dt>Created</dt><dd>{formatDate(product.createdAt)}</dd></div><div><dt>Created by</dt><dd>{product.createdBy || 'Unknown'}</dd></div><div><dt>Last activity</dt><dd>{formatDateTime(overview.lastActivityAt)}</dd></div></dl>
        <div className="license-status-block"><span>License status mix</span>{overview.totalKeys ? <><div className="license-status-bar">{statusSegments.map(([label, count, className]) => <i key={label} className={className} style={{ width: `${(count / overview.totalKeys) * 100}%` }} title={`${label}: ${count}`} />)}</div><div className="license-status-legend">{statusSegments.map(([label, count, className]) => <span key={label}><i className={className} />{label} <b>{count}</b></span>)}</div></> : <p>No licenses to summarize.</p>}</div>
      </aside>
    </div>

    <section className="product-detail-panel card product-activity-panel">
      <div className="product-panel-heading"><div><h2>Recent validation activity</h2><p>Latest client requests using product code {product.productCode}.</p></div><HiOutlineChartBarSquare /></div>
      {overview.recentActivity.length === 0 ? <div className="product-detail-empty compact"><strong>No validation activity yet</strong></div> : <div className="table-wrapper"><table className="table"><thead><tr><th>Timestamp</th><th>Action</th><th>Result</th><th>Reason</th><th>Location / IP</th><th>Hardware ID</th></tr></thead><tbody>{overview.recentActivity.map((activity) => <tr key={activity.id}><td>{formatDateTime(activity.timestamp)}</td><td>{activity.action}</td><td><span className={`badge ${activity.result === 'SUCCESS' ? 'badge-active' : 'badge-revoked'}`}>{activity.result}</span></td><td>{activity.reason?.replaceAll('_', ' ')}</td><td><div className="product-activity-location"><span><HiOutlineMapPin /> {[activity.location?.city, activity.location?.country].filter(Boolean).join(', ') || 'Location unavailable'}</span><code>{activity.location?.publicIpAddress || activity.ipAddress || '—'}</code></div></td><td><code className="product-hardware-id">{activity.hardwareId || '—'}</code></td></tr>)}</tbody></table></div>}
    </section>
  </div>;
}
