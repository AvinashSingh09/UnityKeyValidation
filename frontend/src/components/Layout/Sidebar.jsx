import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  HiOutlineKey,
  HiOutlineSquares2X2,
  HiOutlineCube,
  HiOutlineDocumentText,
  HiOutlineChartBarSquare,
  HiOutlineArrowRightOnRectangle,
} from 'react-icons/hi2';
import './Sidebar.css';

export default function Sidebar() {
  const { user, logoutUser } = useAuth();

  const navItems = [
    { to: '/', icon: <HiOutlineSquares2X2 />, label: 'Dashboard' },
    { to: '/products', icon: <HiOutlineCube />, label: 'Products' },
    { to: '/keys', icon: <HiOutlineKey />, label: 'License Keys' },
    { to: '/analytics', icon: <HiOutlineChartBarSquare />, label: 'Analytics' },
    { to: '/logs', icon: <HiOutlineDocumentText />, label: 'Validation Logs' },
  ];

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-logo">
          <HiOutlineKey />
        </div>
        <div>
          <h2>KeyVault</h2>
          <span>License Manager</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'sidebar-link-active' : ''}`
            }
          >
            <span className="sidebar-link-icon">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">
          <div className="sidebar-avatar">
            {user?.fullName?.charAt(0)?.toUpperCase() || '?'}
          </div>
          <div className="sidebar-user-info">
            <span className="sidebar-user-name">{user?.fullName}</span>
            <span className="sidebar-user-role">{user?.role?.replace('_', ' ')}</span>
          </div>
        </div>
        <button onClick={logoutUser} className="btn btn-ghost btn-icon sidebar-logout" title="Logout">
          <HiOutlineArrowRightOnRectangle />
        </button>
      </div>
    </aside>
  );
}
