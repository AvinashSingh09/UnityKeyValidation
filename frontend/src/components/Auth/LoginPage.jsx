import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { forgotPassword, getSetupStatus, login, register } from '../../api/authApi';
import toast from 'react-hot-toast';
import { HiOutlineShieldCheck } from 'react-icons/hi2';
import './Login.css';

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const [isForgot, setIsForgot] = useState(false);
  const [setupRequired, setSetupRequired] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => { getSetupStatus().then(({ data }) => setSetupRequired(data.setupRequired)).catch(() => {}); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isForgot) {
        await forgotPassword(email);
        toast.success('If that account exists, a reset link has been sent.');
        setIsForgot(false);
        return;
      }
      let response;
      if (isRegister) {
        response = await register(fullName, email, password);
      } else {
        response = await login(email, password);
      }
      loginUser(response.data);
      toast.success(`Welcome${response.data.fullName ? ', ' + response.data.fullName : ''}!`);
      navigate('/');
    } catch (error) {
      const msg = error.response?.data?.message || 'Authentication failed';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-bg-effects">
        <div className="login-orb login-orb-1" />
        <div className="login-orb login-orb-2" />
        <div className="login-orb login-orb-3" />
      </div>

      <div className="login-card">
        <div className="login-logo">
          <img
            className="login-brand-logo"
            src="/360 logo.png"
            alt="360 Bright Media"
          />
          <p>Unity License Management Platform</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {isRegister && !isForgot && (
            <div className="input-group">
              <label htmlFor="fullName">Full Name</label>
              <input
                id="fullName"
                type="text"
                className="input"
                placeholder="Enter your name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </div>
          )}

          <div className="input-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              className="input"
              placeholder="admin@company.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          {!isForgot && <div className="input-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={isRegister ? 10 : 8}
            />
          </div>}

          {!isRegister && !isForgot && <button type="button" className="login-forgot" onClick={() => setIsForgot(true)}>Forgot password?</button>}

          <button type="submit" className="btn btn-primary login-btn" disabled={loading}>
            {loading ? (
              <span className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} />
            ) : (
              <>
                <HiOutlineShieldCheck />
                {isForgot ? 'Send Reset Link' : isRegister ? 'Create Account' : 'Sign In'}
              </>
            )}
          </button>
        </form>

        <div className="login-toggle">
          {isForgot ? <><span>Remembered your password?</span><button onClick={() => setIsForgot(false)} className="btn btn-ghost btn-sm">Sign In</button></>
            : isRegister ? <><span>Already have an account?</span><button onClick={() => setIsRegister(false)} className="btn btn-ghost btn-sm">Sign In</button></>
            : setupRequired ? <><span>First-time setup?</span><button onClick={() => setIsRegister(true)} className="btn btn-ghost btn-sm">Create Owner Account</button></>
            : <span>Accounts are created by your KeyVault administrator.</span>}
        </div>
      </div>
    </div>
  );
}
