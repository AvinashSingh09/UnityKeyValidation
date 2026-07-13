import { useState, useEffect } from 'react';
import { getLogs } from '../../api/analyticsApi';
import { formatDateTime } from '../../utils/dateUtils';
import { HiOutlineDocumentText } from 'react-icons/hi2';
import './Logs.css';

export default function LogsPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchLogs();
  }, [page]);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const { data } = await getLogs({ page, size: 30 });
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
    } catch (err) {
      console.error('Failed to load logs', err);
    } finally {
      setLoading(false);
    }
  };

  const resultBadge = (result) => {
    return (
      <span className={`badge ${result === 'SUCCESS' ? 'badge-active' : 'badge-revoked'}`}>
        {result}
      </span>
    );
  };

  return (
    <div className="logs-page">
      <div className="page-header">
        <div>
          <h1>Validation Logs</h1>
          <p>Audit trail of all key validation attempts</p>
        </div>
      </div>

      {loading ? (
        <div className="loader"><div className="spinner" /></div>
      ) : logs.length === 0 ? (
        <div className="card empty-state">
          <HiOutlineDocumentText />
          <h3>No validation logs yet</h3>
          <p>Logs will appear here when Unity clients validate keys</p>
        </div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Action</th>
                  <th>Result</th>
                  <th>Reason</th>
                  <th>Product Code</th>
                  <th>Hardware ID</th>
                  <th>IP Address</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.timestamp)}</td>
                    <td><span className="log-action">{log.action}</span></td>
                    <td>{resultBadge(log.result)}</td>
                    <td><span className="text-sm text-secondary">{log.reason}</span></td>
                    <td><span className="mono text-sm">{log.productCode || '—'}</span></td>
                    <td><span className="mono text-sm truncate log-hwid">{log.hardwareId || '—'}</span></td>
                    <td><span className="text-sm">{log.ipAddress || '—'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Prev</button>
            <span>Page {page + 1} of {totalPages || 1}</span>
            <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next</button>
          </div>
        </>
      )}
    </div>
  );
}
