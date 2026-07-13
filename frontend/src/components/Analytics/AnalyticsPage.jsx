import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  HiOutlineArrowPath,
  HiOutlineBolt,
  HiOutlineChartBarSquare,
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineComputerDesktop,
  HiOutlineExclamationTriangle,
} from 'react-icons/hi2';
import { getInsights } from '../../api/analyticsApi';
import { formatDateTime } from '../../utils/dateUtils';
import './AnalyticsPage.css';

const shortDate = (value) => new Date(`${value}T00:00:00Z`).toLocaleDateString('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' });
const labelReason = (value) => value?.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());

function MetricCard({ icon, label, value, note, tone = 'primary' }) {
  return <article className="analytics-metric card">
    <span className={`analytics-metric-icon tone-${tone}`}>{icon}</span>
    <div><span>{label}</span><strong>{value}</strong><small>{note}</small></div>
  </article>;
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return <div className="analytics-tooltip"><strong>{label}</strong>{payload.map((item) => <span key={item.dataKey}><i style={{ background: item.color }} />{item.name}<b>{item.value}</b></span>)}</div>;
}

export default function AnalyticsPage() {
  const [days, setDays] = useState(14);
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadInsights = async (selectedDays = days) => {
    setLoading(true);
    try {
      const { data } = await getInsights(selectedDays);
      setInsights(data);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to load analytics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadInsights(days); }, [days]);

  const trend = useMemo(() => (insights?.dailyMetrics || []).map((metric) => ({ ...metric, label: shortDate(metric.date) })), [insights]);
  const failureReasons = useMemo(() => (insights?.failureReasons || []).map((reason) => ({ ...reason, label: labelReason(reason.reason) })), [insights]);

  return (
    <div className="analytics-page">
      <div className="page-header analytics-header">
        <div><h1>Analytics</h1><p>Monitor usage, validation health, and license risk.</p></div>
        <div className="analytics-controls">
          <select className="select" value={days} onChange={(event) => setDays(Number(event.target.value))} aria-label="Analytics period">
            <option value={7}>Last 7 days</option><option value={14}>Last 14 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option>
          </select>
          <button className="btn btn-secondary btn-icon" onClick={() => loadInsights()} disabled={loading} title="Refresh analytics"><HiOutlineArrowPath /></button>
        </div>
      </div>

      {loading && !insights ? <div className="loader"><div className="spinner" /></div> : !insights ? (
        <div className="card empty-state"><HiOutlineChartBarSquare /><h3>Analytics unavailable</h3><p>Try refreshing the page.</p></div>
      ) : <>
        <section className="analytics-metrics">
          <MetricCard icon={<HiOutlineCheckCircle />} label="Validation success" value={`${insights.successRate.toFixed(1)}%`} note={`${insights.successfulValidations.toLocaleString()} of ${insights.totalValidations.toLocaleString()} attempts · ${insights.periodDays} days`} tone="success" />
          <MetricCard icon={<HiOutlineBolt />} label="Validations" value={insights.totalValidations.toLocaleString()} note={`${insights.failedValidations.toLocaleString()} failed in selected period`} />
          <MetricCard icon={<HiOutlineComputerDesktop />} label="Active devices · 24h" value={insights.activeDevices24h.toLocaleString()} note={`${insights.activeDevices7d.toLocaleString()} seen in 7 days`} tone="info" />
          <MetricCard icon={<HiOutlineExclamationTriangle />} label="Expiring · 30d" value={insights.expiringIn30Days.toLocaleString()} note={`${insights.expiringIn7Days.toLocaleString()} expire within 7 days`} tone={insights.expiringIn7Days ? 'warning' : 'success'} />
        </section>

        <section className="analytics-grid">
          <article className="analytics-panel card analytics-trend-panel">
            <div className="analytics-panel-heading"><div><h2>Validation trend</h2><p>Daily successful and failed requests, with new device activations.</p></div><span className="analytics-live-dot">UTC days</span></div>
            <div className="analytics-chart analytics-chart-large">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trend} margin={{ top: 10, right: 8, left: -24, bottom: 0 }}>
                  <defs><linearGradient id="successFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#10b981" stopOpacity={0.24}/><stop offset="95%" stopColor="#10b981" stopOpacity={0}/></linearGradient><linearGradient id="failureFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#ef4444" stopOpacity={0.18}/><stop offset="95%" stopColor="#ef4444" stopOpacity={0}/></linearGradient></defs>
                  <CartesianGrid stroke="var(--border-light)" vertical={false} />
                  <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: 'var(--text-tertiary)', fontSize: 10 }} minTickGap={22} />
                  <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: 'var(--text-tertiary)', fontSize: 10 }} />
                  <Tooltip content={<ChartTooltip />} />
                  <Area type="monotone" dataKey="successes" name="Successful" stroke="#10b981" strokeWidth={2} fill="url(#successFill)" />
                  <Area type="monotone" dataKey="failures" name="Failed" stroke="#ef4444" strokeWidth={2} fill="url(#failureFill)" />
                  <Area type="monotone" dataKey="activations" name="New activations" stroke="#6366f1" strokeWidth={2} fill="transparent" strokeDasharray="4 4" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
            <div className="analytics-legend"><span><i className="legend-success" />Successful</span><span><i className="legend-failure" />Failed</span><span><i className="legend-activation" />New activations</span></div>
          </article>

          <article className="analytics-panel card">
            <div className="analytics-panel-heading"><div><h2>Failure drivers</h2><p>Why validation attempts were rejected.</p></div></div>
            {failureReasons.length ? <div className="analytics-chart analytics-chart-small"><ResponsiveContainer width="100%" height="100%"><BarChart data={failureReasons} layout="vertical" margin={{ top: 4, right: 12, left: 16, bottom: 0 }}><CartesianGrid stroke="var(--border-light)" horizontal={false}/><XAxis type="number" allowDecimals={false} axisLine={false} tickLine={false} tick={{ fill: 'var(--text-tertiary)', fontSize: 10 }}/><YAxis type="category" dataKey="label" width={118} axisLine={false} tickLine={false} tick={{ fill: 'var(--text-secondary)', fontSize: 10 }}/><Tooltip cursor={{ fill: 'var(--bg-tertiary)' }} content={<ChartTooltip />} /><Bar dataKey="count" name="Failures" fill="#ef4444" radius={[0, 5, 5, 0]} barSize={14}/></BarChart></ResponsiveContainer></div> : <div className="analytics-zero"><HiOutlineCheckCircle /><strong>No failed validations</strong><span>Nothing to investigate in this period.</span></div>}
          </article>
        </section>

        <section className="analytics-panel card analytics-products">
          <div className="analytics-panel-heading"><div><h2>Product usage</h2><p>License footprint and validation volume by product for the selected period.</p></div></div>
          {(insights.productMetrics || []).length ? <div className="table-wrapper"><table className="table"><thead><tr><th>Product</th><th>Active keys</th><th>Active seats</th><th>Validations</th><th>Success rate</th></tr></thead><tbody>{insights.productMetrics.map((product) => <tr key={product.productId}><td><div className="analytics-product-name"><strong>{product.productName}</strong><code>{product.productCode}</code></div></td><td><strong>{product.activeKeys}</strong><span> / {product.totalKeys}</span></td><td>{product.activeDevices}</td><td>{product.validations.toLocaleString()}</td><td><div className="rate-cell"><strong>{product.successRate.toFixed(1)}%</strong><span><i style={{ width: `${product.successRate}%` }} /></span></div></td></tr>)}</tbody></table></div> : <div className="analytics-zero"><strong>No products found</strong></div>}
        </section>

        <footer className="analytics-footnote"><HiOutlineClock /><span>Updated {formatDateTime(insights.generatedAt)}. Active devices count license seats whose last successful activity falls within each rolling window. Validation metrics use server-recorded attempts; days are grouped in UTC.</span></footer>
      </>}
    </div>
  );
}
