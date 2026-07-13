import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  HiOutlineArrowLeft,
  HiOutlineClipboard,
  HiOutlineComputerDesktop,
  HiOutlineMapPin,
  HiOutlinePencilSquare,
  HiOutlineShieldCheck,
  HiOutlineTrash,
  HiOutlineUser,
} from 'react-icons/hi2';
import { getKey, removeDevice, updateDevice, updateKey } from '../../api/keyApi';
import { getLogs } from '../../api/analyticsApi';
import { useAuth } from '../../context/AuthContext';
import { formatDate, formatDateTime, timeRemaining } from '../../utils/dateUtils';
import './KeyDetailPage.css';

const statusClass = {
  ACTIVE: 'badge-active',
  EXPIRED: 'badge-expired',
  REVOKED: 'badge-revoked',
  SUSPENDED: 'badge-suspended',
};

const toDateTimeInput = (value) => value ? new Date(value).toISOString().slice(0, 16) : '';
const licenseFormFrom = (license) => ({
  customerName: license.customerName || '', customerEmail: license.customerEmail || '',
  type: license.type || 'TIME_LIMITED', maxActivations: license.maxActivations || 1,
  validFrom: toDateTimeInput(license.validFrom), validUntil: toDateTimeInput(license.validUntil),
  notes: license.notes || '',
});

export default function KeyDetailPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const editRequested = searchParams.get('edit') === 'true';
  const { isAdmin } = useAuth();
  const [license, setLicense] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingDevice, setEditingDevice] = useState(null);
  const [deviceForm, setDeviceForm] = useState({ machineName: '', trusted: false });
  const [saving, setSaving] = useState(false);
  const [editingLicense, setEditingLicense] = useState(false);
  const [licenseForm, setLicenseForm] = useState(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [keyResponse, logResponse] = await Promise.all([
          getKey(id),
          getLogs({ keyId: id, page: 0, size: 20 }),
        ]);
        setLicense(keyResponse.data);
        if (editRequested) {
          setLicenseForm(licenseFormFrom(keyResponse.data));
          setEditingLicense(true);
        }
        setLogs(logResponse.data.content || []);
      } catch (error) {
        toast.error(error.response?.data?.message || 'Failed to load license details');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id, editRequested]);

  const devices = useMemo(() => {
    return [...(license?.activations || [])].sort((a, b) =>
      new Date(b.lastValidatedAt || b.activatedAt) - new Date(a.lastValidatedAt || a.activatedAt));
  }, [license]);

  const openDeviceEditor = (device) => {
    setEditingDevice(device);
    setDeviceForm({ machineName: device.machineName || '', trusted: Boolean(device.trusted) });
  };

  const openLicenseEditor = () => {
    setLicenseForm(licenseFormFrom(license));
    setEditingLicense(true);
  };

  const saveLicense = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...licenseForm,
        maxActivations: Number(licenseForm.maxActivations),
        validFrom: new Date(licenseForm.validFrom).toISOString(),
        validUntil: licenseForm.type === 'PERPETUAL' ? null : new Date(licenseForm.validUntil).toISOString(),
      };
      const { data } = await updateKey(id, payload);
      setLicense(data);
      setEditingLicense(false);
      toast.success('License details updated');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update license');
    } finally {
      setSaving(false);
    }
  };

  const saveDevice = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const { data } = await updateDevice(id, editingDevice.hardwareId, deviceForm);
      setLicense(data);
      setEditingDevice(null);
      toast.success('Device updated');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update device');
    } finally {
      setSaving(false);
    }
  };

  const deactivateDevice = async (device) => {
    if (!window.confirm(`Deactivate ${device.machineName || 'this device'} and free its seat?`)) return;
    try {
      const { data } = await removeDevice(id, device.hardwareId);
      setLicense(data);
      toast.success('Device deactivated and seat released');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to deactivate device');
    }
  };

  const copyKey = async () => {
    await navigator.clipboard.writeText(license.key);
    toast.success('Key copied to clipboard');
  };

  const locationLabel = (device) => {
    const location = device.location;
    if (!location) return 'Location unavailable';
    return [location.city, location.region, location.country].filter(Boolean).join(', ') || 'Location unavailable';
  };

  if (loading) return <div className="loader"><div className="spinner" /></div>;
  if (!license) {
    return <div className="card empty-state"><h3>License not found</h3><Link to="/keys">Return to license keys</Link></div>;
  }

  return (
    <div className="key-detail-page">
      <Link to="/keys" className="detail-back"><HiOutlineArrowLeft /> License keys</Link>

      <header className="detail-hero card">
        <div className="detail-hero-main">
          <div className="detail-key-icon"><HiOutlineShieldCheck /></div>
          <div>
            <div className="detail-title-row">
              <h1>{license.productName}</h1>
              <span className={`badge ${statusClass[license.status] || ''}`}>{license.status}</span>
            </div>
            <div className="detail-license-key">
              <code>{license.key}</code>
              <button className="btn btn-ghost btn-icon btn-sm" onClick={copyKey} title="Copy license key"><HiOutlineClipboard /></button>
            </div>
          </div>
        </div>
        <div className="detail-customer">
          <HiOutlineUser />
          <div><span>Assigned to</span><strong>{license.customerName || 'Unassigned'}</strong><small>{license.customerEmail || 'No email provided'}</small></div>
        </div>
      </header>

      <section className="detail-stats">
        <article className="card"><span>Active seats</span><strong>{license.currentActivations} / {license.maxActivations}</strong><small>{Math.max(0, license.maxActivations - license.currentActivations)} seats available</small></article>
        <article className="card"><span>License type</span><strong>{license.type?.replaceAll('_', ' ')}</strong><small>Valid from {formatDate(license.validFrom)}</small></article>
        <article className="card"><span>Expires</span><strong>{formatDate(license.validUntil)}</strong><small>{timeRemaining(license.validUntil)}</small></article>
        <article className="card"><span>Last activity</span><strong>{devices[0] ? formatDateTime(devices[0].lastValidatedAt || devices[0].activatedAt) : 'Never'}</strong><small>{devices[0]?.machineName || 'No activated devices'}</small></article>
      </section>

      <div className="detail-grid">
        <section className="detail-section card">
          <div className="detail-section-heading">
            <div><h2>Devices</h2><p>Manage seats currently attached to this license.</p></div>
            <span className="detail-count">{devices.length}</span>
          </div>

          {devices.length === 0 ? (
            <div className="detail-empty"><HiOutlineComputerDesktop /><strong>No activated devices</strong><span>The first successful activation will appear here.</span></div>
          ) : (
            <div className="device-list">
              {devices.map((device) => (
                <article className="device-card" key={device.hardwareId}>
                  <div className="device-card-top">
                    <div className="device-identity">
                      <span className="device-icon"><HiOutlineComputerDesktop /></span>
                      <div><h3>{device.machineName || 'Unnamed device'}</h3><p><HiOutlineMapPin /> {locationLabel(device)}</p></div>
                    </div>
                    <div className="device-actions">
                      {device.trusted && <span className="trusted-badge"><HiOutlineShieldCheck /> Trusted</span>}
                      {isAdmin() && <button className="btn btn-ghost btn-icon btn-sm" onClick={() => openDeviceEditor(device)} title="Edit device"><HiOutlinePencilSquare /></button>}
                      {isAdmin() && <button className="btn btn-ghost btn-icon btn-sm text-danger" onClick={() => deactivateDevice(device)} title="Deactivate device"><HiOutlineTrash /></button>}
                    </div>
                  </div>
                  <div className="device-facts">
                    <span><small>Public IP</small><code>{device.location?.publicIpAddress || device.ipAddress || 'Unavailable'}</code></span>
                    <span><small>Last active</small><strong>{formatDateTime(device.lastValidatedAt || device.activatedAt)}</strong></span>
                    <span><small>Activated</small><strong>{formatDateTime(device.activatedAt)}</strong></span>
                    <span><small>ISP</small><strong>{device.location?.isp || 'Unavailable'}</strong></span>
                  </div>
                  <div className="device-hardware"><small>Hardware ID</small><code>{device.hardwareId}</code></div>
                </article>
              ))}
            </div>
          )}
          <p className="detail-note">Geo-IP locations are approximate and may represent a VPN, carrier, or ISP gateway.</p>
        </section>

        <aside className="detail-section card detail-metadata">
          <div className="detail-section-heading"><div><h2>License details</h2><p>Configuration and ownership.</p></div>{isAdmin() && <button className="btn btn-secondary btn-sm" onClick={openLicenseEditor}><HiOutlinePencilSquare /> Edit</button>}</div>
          <dl>
            <div><dt>Product</dt><dd>{license.productName}</dd></div>
            <div><dt>Status</dt><dd>{license.status}</dd></div>
            <div><dt>Maximum seats</dt><dd>{license.maxActivations}</dd></div>
            <div><dt>Valid from</dt><dd>{formatDate(license.validFrom)}</dd></div>
            <div><dt>Valid until</dt><dd>{formatDate(license.validUntil)}</dd></div>
            <div><dt>Created</dt><dd>{formatDate(license.createdAt)}</dd></div>
          </dl>
          {license.notes && <div className="detail-notes"><span>Notes</span><p>{license.notes}</p></div>}
        </aside>
      </div>

      <section className="detail-section card activity-section">
        <div className="detail-section-heading"><div><h2>Recent activity</h2><p>The latest validation attempts for this license.</p></div></div>
        {logs.length === 0 ? <div className="detail-empty compact"><strong>No activity recorded</strong></div> : (
          <div className="table-wrapper">
            <table className="table">
              <thead><tr><th>Timestamp</th><th>Action</th><th>Result</th><th>Reason</th><th>IP address</th><th>Hardware ID</th></tr></thead>
              <tbody>{logs.map((log) => <tr key={log.id}>
                <td>{formatDateTime(log.timestamp)}</td><td>{log.action}</td>
                <td><span className={`badge ${log.result === 'SUCCESS' ? 'badge-active' : 'badge-revoked'}`}>{log.result}</span></td>
                <td>{log.reason?.replaceAll('_', ' ')}</td><td><code>{log.ipAddress || '—'}</code></td><td><code className="detail-log-hardware">{log.hardwareId || '—'}</code></td>
              </tr>)}</tbody>
            </table>
          </div>
        )}
      </section>

      {editingLicense && licenseForm && (
        <div className="modal-overlay" onClick={() => setEditingLicense(false)}>
          <div className="modal modal-large license-edit-modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header"><div><h2>Edit license</h2><p className="mono">{license.key}</p></div><button className="btn btn-ghost btn-icon" onClick={() => setEditingLicense(false)}>×</button></div>
            <form onSubmit={saveLicense}>
              <div className="form-row"><div className="input-group"><label>Customer name</label><input className="input" maxLength={160} value={licenseForm.customerName} onChange={(event) => setLicenseForm({ ...licenseForm, customerName: event.target.value })} /></div><div className="input-group"><label>Customer email</label><input className="input" type="email" value={licenseForm.customerEmail} onChange={(event) => setLicenseForm({ ...licenseForm, customerEmail: event.target.value })} /></div></div>
              <div className="form-row mt-md"><div className="input-group"><label>License type</label><select className="select" value={licenseForm.type} onChange={(event) => setLicenseForm({ ...licenseForm, type: event.target.value })}><option value="TIME_LIMITED">Time limited</option><option value="TRIAL">Trial</option><option value="PERPETUAL">Perpetual</option></select></div><div className="input-group"><label>Maximum devices</label><input className="input" type="number" min={license.currentActivations || 1} required value={licenseForm.maxActivations} onChange={(event) => setLicenseForm({ ...licenseForm, maxActivations: event.target.value })} /><small className="form-help">Currently using {license.currentActivations} device seat(s).</small></div></div>
              <div className="form-row mt-md"><div className="input-group"><label>Valid from</label><input className="input" type="datetime-local" required value={licenseForm.validFrom} onChange={(event) => setLicenseForm({ ...licenseForm, validFrom: event.target.value })} /></div><div className="input-group"><label>Expires</label><input className="input" type="datetime-local" required={licenseForm.type !== 'PERPETUAL'} disabled={licenseForm.type === 'PERPETUAL'} value={licenseForm.type === 'PERPETUAL' ? '' : licenseForm.validUntil} onChange={(event) => setLicenseForm({ ...licenseForm, validUntil: event.target.value })} />{licenseForm.type === 'PERPETUAL' && <small className="form-help">Perpetual licenses do not expire.</small>}</div></div>
              <div className="input-group mt-md"><label>Internal notes</label><textarea className="input license-notes-input" maxLength={1000} rows={4} value={licenseForm.notes} onChange={(event) => setLicenseForm({ ...licenseForm, notes: event.target.value })} placeholder="Optional notes about this license" /></div>
              <div className="modal-footer"><button type="button" className="btn btn-secondary" onClick={() => setEditingLicense(false)}>Cancel</button><button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button></div>
            </form>
          </div>
        </div>
      )}

      {editingDevice && (
        <div className="modal-overlay" onClick={() => setEditingDevice(null)}>
          <div className="modal device-edit-modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header"><div><h2>Edit device</h2><p>{editingDevice.hardwareId}</p></div><button className="btn btn-ghost btn-icon" onClick={() => setEditingDevice(null)}>×</button></div>
            <form onSubmit={saveDevice}>
              <div className="input-group"><label>Device name</label><input className="input" maxLength="120" value={deviceForm.machineName} onChange={(event) => setDeviceForm({ ...deviceForm, machineName: event.target.value })} placeholder="e.g. Studio PC" /></div>
              <label className="trusted-toggle"><input type="checkbox" checked={deviceForm.trusted} onChange={(event) => setDeviceForm({ ...deviceForm, trusted: event.target.checked })} /><span><strong>Mark as trusted</strong><small>Use this label for devices you recognize and approve.</small></span></label>
              <div className="modal-footer"><button type="button" className="btn btn-secondary" onClick={() => setEditingDevice(null)}>Cancel</button><button className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save device'}</button></div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
