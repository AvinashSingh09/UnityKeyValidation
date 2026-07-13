import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './components/Auth/LoginPage';
import Sidebar from './components/Layout/Sidebar';
import ProductsPage from './components/Products/ProductsPage';
import ProductDetailPage from './components/Products/ProductDetailPage';
import KeysPage from './components/Keys/KeysPage';
import KeyDetailPage from './components/Keys/KeyDetailPage';
import LogsPage from './components/Logs/LogsPage';
import './App.css';

const AnalyticsPage = lazy(() => import('./components/Analytics/AnalyticsPage'));
const DashboardPage = lazy(() => import('./components/Dashboard/DashboardPage'));

const pageLoader = <div className="loader"><div className="spinner" /></div>;

function ProtectedRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="loader"><div className="spinner" /></div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="app-layout">
      <Sidebar />
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

function PublicRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="loader"><div className="spinner" /></div>;
  }

  if (user) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: {
              background: 'var(--bg-card)',
              color: 'var(--text-primary)',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius-md)',
              fontSize: '14px',
            },
          }}
        />
        <Routes>
          <Route element={<PublicRoute />}>
            <Route path="/login" element={<LoginPage />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Suspense fallback={pageLoader}><DashboardPage /></Suspense>} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/products/:id" element={<ProductDetailPage />} />
            <Route path="/keys" element={<KeysPage />} />
            <Route path="/keys/:id" element={<KeyDetailPage />} />
            <Route path="/analytics" element={<Suspense fallback={pageLoader}><AnalyticsPage /></Suspense>} />
            <Route path="/logs" element={<LogsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
