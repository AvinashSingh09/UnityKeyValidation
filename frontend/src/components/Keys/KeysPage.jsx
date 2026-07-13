import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getKeys, createKey, batchCreateKeys, revokeKey, suspendKey, reactivateKey, deleteKey } from '../../api/keyApi';
import { getProducts } from '../../api/productApi';
import { useAuth } from '../../context/AuthContext';
import { formatDate, formatDateTime, timeRemaining } from '../../utils/dateUtils';
import toast from 'react-hot-toast';
import {
  HiOutlinePlus,
  HiOutlineKey,
  HiOutlineClipboard,
  HiOutlineXMark,
  HiOutlinePause,
  HiOutlinePlay,
  HiOutlineTrash,
  HiOutlineFunnel,
  HiOutlineSquare3Stack3D,
  HiOutlineEye,
  HiOutlinePencilSquare,
  HiOutlineMapPin,
} from 'react-icons/hi2';
import './Keys.css';

export default function KeysPage() {
  const [keys, setKeys] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchParams] = useSearchParams();
  const [filterProduct, setFilterProduct] = useState(() => searchParams.get('productId') || '');
  const [filterStatus, setFilterStatus] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showBatchModal, setShowBatchModal] = useState(false);
  const navigate = useNavigate();
  const { isAdmin, isSuperAdmin } = useAuth();

  const [createForm, setCreateForm] = useState({
    productId: '', customerName: '', customerEmail: '',
    type: 'TIME_LIMITED', maxActivations: 1,
    validFrom: '', validUntil: '', notes: '',
  });

  const [batchForm, setBatchForm] = useState({
    productId: '', count: 5, customerName: '',
    type: 'TIME_LIMITED', maxActivations: 1,
    validFrom: '', validUntil: '', notes: '',
  });

  useEffect(() => {
    fetchProducts();
  }, []);

  useEffect(() => {
    fetchKeys();
  }, [page, filterProduct, filterStatus]);

  const fetchProducts = async () => {
    try {
      const { data } = await getProducts();
      setProducts(data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchKeys = async () => {
    setLoading(true);
    try {
      const params = { page, size: 15 };
      if (filterProduct) params.productId = filterProduct;
      if (filterStatus) params.status = filterStatus;
      const { data } = await getKeys(params);
      setKeys(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      toast.error('Failed to load keys');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...createForm,
        validFrom: createForm.validFrom ? new Date(createForm.validFrom).toISOString() : null,
        validUntil: createForm.validUntil ? new Date(createForm.validUntil).toISOString() : null,
      };
      const { data } = await createKey(payload);
      toast.success('Key generated!');
      copyToClipboard(data.key);
      setShowCreateModal(false);
      fetchKeys();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create key');
    }
  };

  const handleBatchCreate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...batchForm,
        validFrom: batchForm.validFrom ? new Date(batchForm.validFrom).toISOString() : null,
        validUntil: batchForm.validUntil ? new Date(batchForm.validUntil).toISOString() : null,
      };
      const { data } = await batchCreateKeys(payload);
      toast.success(`${data.length} keys generated!`);
      const keysText = data.map(k => k.key).join('\n');
      navigator.clipboard.writeText(keysText);
      toast.success('All keys copied to clipboard');
      setShowBatchModal(false);
      fetchKeys();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Batch generation failed');
    }
  };

  const handleStatusAction = async (id, action) => {
    try {
      if (action === 'revoke') {
        await revokeKey(id);
        toast.success('Key revoked');
      } else if (action === 'suspend') {
        await suspendKey(id);
        toast.success('Key suspended');
      } else if (action === 'reactivate') {
        await reactivateKey(id);
        toast.success('Key reactivated');
      } else if (action === 'delete') {
        if (!window.confirm('Permanently delete this key?')) return;
        await deleteKey(id);
        toast.success('Key deleted');
      }
      fetchKeys();
    } catch (err) {
      toast.error('Action failed');
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    toast.success('Key copied to clipboard');
  };

  const statusBadge = (status) => {
    const map = {
      ACTIVE: 'badge-active',
      EXPIRED: 'badge-expired',
      REVOKED: 'badge-revoked',
      SUSPENDED: 'badge-suspended',
    };
    return <span className={`badge ${map[status] || ''}`}>{status}</span>;
  };

  const latestActivation = (key) => {
    if (!key.activations?.length) return null;
    return [...key.activations].sort((a, b) =>
      new Date(b.lastValidatedAt || b.activatedAt) - new Date(a.lastValidatedAt || a.activatedAt)
    )[0];
  };

  const locationLabel = (activation) => {
    if (!activation?.location) return 'Location unavailable';
    return [activation.location.city, activation.location.region, activation.location.country]
      .filter(Boolean).join(', ') || 'Location unavailable';
  };

  const displayIp = (activation) =>
    activation?.location?.publicIpAddress || activation?.ipAddress || 'IP unavailable';

  return (
    <div className="keys-page">
      <div className="page-header">
        <div>
          <h1>License Keys</h1>
          <p>Generate and manage license keys</p>
        </div>
        {isAdmin() && (
          <div className="flex gap-sm">
            <button onClick={() => setShowBatchModal(true)} className="btn btn-secondary">
              <HiOutlineSquare3Stack3D /> Batch Generate
            </button>
            <button onClick={() => setShowCreateModal(true)} className="btn btn-primary">
              <HiOutlinePlus /> Generate Key
            </button>
          </div>
        )}
      </div>

      {/* Filters */}
      <div className="keys-filters card">
        <HiOutlineFunnel />
        <select className="select" value={filterProduct} onChange={(e) => { setFilterProduct(e.target.value); setPage(0); }}>
          <option value="">All Products</option>
          {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
        <select className="select" value={filterStatus} onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}>
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="EXPIRED">Expired</option>
          <option value="REVOKED">Revoked</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>

      {/* Table */}
      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : keys.length === 0 ? (
        <div className="card empty-state">
          <HiOutlineKey />
          <h3>No license keys found</h3>
          <p>Generate your first key to get started</p>
        </div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Product</th>
                  <th>Customer</th>
                  <th>Status</th>
                  <th>Type</th>
                  <th>Seats</th>
                  <th>Last Active Device</th>
                  <th>Expires</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {keys.map((key) => {
                  const lastSeen = latestActivation(key);
                  return (
                  <tr key={key.id}>
                    <td>
                      <div className="key-cell">
                        <span className="mono key-text">{key.key}</span>
                        <button onClick={() => copyToClipboard(key.key)} className="btn btn-ghost btn-icon btn-sm" title="Copy">
                          <HiOutlineClipboard />
                        </button>
                      </div>
                    </td>
                    <td>{key.productName}</td>
                    <td>{key.customerName || '—'}</td>
                    <td>{statusBadge(key.status)}</td>
                    <td><span className="text-sm text-secondary">{key.type}</span></td>
                    <td>
                      <span className="seats-info">{key.currentActivations}/{key.maxActivations}</span>
                    </td>
                    <td>
                      {lastSeen ? (
                        <div className="last-active-cell">
                          <span className="last-active-location"><HiOutlineMapPin /> {locationLabel(lastSeen)}</span>
                          <span className="mono last-active-ip">{displayIp(lastSeen)}</span>
                          <span className="last-active-time">{formatDateTime(lastSeen.lastValidatedAt || lastSeen.activatedAt)}</span>
                        </div>
                      ) : <span className="text-secondary text-sm">Never activated</span>}
                    </td>
                    <td>
                      <div className="expiry-cell">
                        <span>{formatDate(key.validUntil)}</span>
                        <span className="text-sm text-secondary">{timeRemaining(key.validUntil)}</span>
                      </div>
                    </td>
                    <td>
                      <div className="flex gap-sm">
                        {isAdmin() && <button onClick={() => navigate(`/keys/${key.id}?edit=true`)} className="btn btn-ghost btn-icon btn-sm" title="Edit license details"><HiOutlinePencilSquare /></button>}
                        <button onClick={() => navigate(`/keys/${key.id}`)} className="btn btn-ghost btn-icon btn-sm" title="Open license details">
                          <HiOutlineEye />
                        </button>
                        {isAdmin() && key.status === 'ACTIVE' && (
                          <>
                            <button onClick={() => handleStatusAction(key.id, 'suspend')} className="btn btn-ghost btn-icon btn-sm" title="Suspend">
                              <HiOutlinePause />
                            </button>
                            <button onClick={() => handleStatusAction(key.id, 'revoke')} className="btn btn-ghost btn-icon btn-sm" title="Revoke">
                              <HiOutlineXMark />
                            </button>
                          </>
                        )}
                        {isAdmin() && (key.status === 'SUSPENDED' || key.status === 'EXPIRED') && (
                          <button onClick={() => handleStatusAction(key.id, 'reactivate')} className="btn btn-ghost btn-icon btn-sm" title="Reactivate">
                            <HiOutlinePlay />
                          </button>
                        )}
                        {isSuperAdmin() && (
                          <button onClick={() => handleStatusAction(key.id, 'delete')} className="btn btn-ghost btn-icon btn-sm text-danger" title="Delete">
                            <HiOutlineTrash />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )})}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="pagination">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</button>
            <span>Page {page + 1} of {totalPages || 1}</span>
            <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next</button>
          </div>
        </>
      )}

      {/* Create Key Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Generate License Key</h2>
              <button onClick={() => setShowCreateModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="input-group">
                <label>Product</label>
                <select className="select" value={createForm.productId} onChange={(e) => setCreateForm({...createForm, productId: e.target.value})} required>
                  <option value="">Select product</option>
                  {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              </div>
              <div className="input-group mt-md">
                <label>Customer Name</label>
                <input className="input" value={createForm.customerName} onChange={(e) => setCreateForm({...createForm, customerName: e.target.value})} placeholder="Client name" />
              </div>
              <div className="input-group mt-md">
                <label>Customer Email</label>
                <input className="input" type="email" value={createForm.customerEmail} onChange={(e) => setCreateForm({...createForm, customerEmail: e.target.value})} placeholder="client@company.com" />
              </div>
              <div className="form-row mt-md">
                <div className="input-group">
                  <label>Key Type</label>
                  <select className="select" value={createForm.type} onChange={(e) => setCreateForm({...createForm, type: e.target.value})}>
                    <option value="TIME_LIMITED">Time Limited</option>
                    <option value="PERPETUAL">Perpetual</option>
                    <option value="TRIAL">Trial</option>
                  </select>
                </div>
                <div className="input-group">
                  <label>Max Devices</label>
                  <input className="input" type="number" min="1" value={createForm.maxActivations} onChange={(e) => setCreateForm({...createForm, maxActivations: parseInt(e.target.value) || 1})} />
                </div>
              </div>
              <div className="form-row mt-md">
                <div className="input-group">
                  <label>Valid From</label>
                  <input className="input" type="datetime-local" value={createForm.validFrom} onChange={(e) => setCreateForm({...createForm, validFrom: e.target.value})} />
                </div>
                <div className="input-group">
                  <label>Valid Until</label>
                  <input className="input" type="datetime-local" value={createForm.validUntil} onChange={(e) => setCreateForm({...createForm, validUntil: e.target.value})} />
                </div>
              </div>
              <div className="input-group mt-md">
                <label>Notes</label>
                <input className="input" value={createForm.notes} onChange={(e) => setCreateForm({...createForm, notes: e.target.value})} placeholder="Internal notes" />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary"><HiOutlineKey /> Generate</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Batch Generate Modal */}
      {showBatchModal && (
        <div className="modal-overlay" onClick={() => setShowBatchModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Batch Generate Keys</h2>
              <button onClick={() => setShowBatchModal(false)} className="btn btn-ghost btn-icon">✕</button>
            </div>
            <form onSubmit={handleBatchCreate}>
              <div className="form-row">
                <div className="input-group">
                  <label>Product</label>
                  <select className="select" value={batchForm.productId} onChange={(e) => setBatchForm({...batchForm, productId: e.target.value})} required>
                    <option value="">Select product</option>
                    {products.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                  </select>
                </div>
                <div className="input-group">
                  <label>Number of Keys</label>
                  <input className="input" type="number" min="1" max="100" value={batchForm.count} onChange={(e) => setBatchForm({...batchForm, count: parseInt(e.target.value) || 1})} />
                </div>
              </div>
              <div className="form-row mt-md">
                <div className="input-group">
                  <label>Key Type</label>
                  <select className="select" value={batchForm.type} onChange={(e) => setBatchForm({...batchForm, type: e.target.value})}>
                    <option value="TIME_LIMITED">Time Limited</option>
                    <option value="PERPETUAL">Perpetual</option>
                    <option value="TRIAL">Trial</option>
                  </select>
                </div>
                <div className="input-group">
                  <label>Max Devices</label>
                  <input className="input" type="number" min="1" value={batchForm.maxActivations} onChange={(e) => setBatchForm({...batchForm, maxActivations: parseInt(e.target.value) || 1})} />
                </div>
              </div>
              <div className="form-row mt-md">
                <div className="input-group">
                  <label>Valid From</label>
                  <input className="input" type="datetime-local" value={batchForm.validFrom} onChange={(e) => setBatchForm({...batchForm, validFrom: e.target.value})} />
                </div>
                <div className="input-group">
                  <label>Valid Until</label>
                  <input className="input" type="datetime-local" value={batchForm.validUntil} onChange={(e) => setBatchForm({...batchForm, validUntil: e.target.value})} />
                </div>
              </div>
              <div className="input-group mt-md">
                <label>Customer Name</label>
                <input className="input" value={batchForm.customerName} onChange={(e) => setBatchForm({...batchForm, customerName: e.target.value})} placeholder="Optional" />
              </div>
              <div className="modal-footer">
                <button type="button" onClick={() => setShowBatchModal(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary"><HiOutlineSquare3Stack3D /> Generate {batchForm.count} Keys</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
