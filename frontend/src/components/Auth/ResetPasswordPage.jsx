import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { HiOutlineKey, HiOutlineShieldCheck } from 'react-icons/hi2';
import { resetPassword } from '../../api/authApi';
import './Login.css';

export default function ResetPasswordPage() {
  const [params] = useSearchParams(); const navigate = useNavigate();
  const [password,setPassword]=useState(''); const [confirm,setConfirm]=useState(''); const [loading,setLoading]=useState(false);
  const submit=async(event)=>{event.preventDefault();if(password!==confirm){toast.error('Passwords do not match');return;}setLoading(true);try{await resetPassword(params.get('token'),password);toast.success('Password reset. You can sign in now.');navigate('/login');}catch(error){toast.error(error.response?.data?.message||'Reset link is invalid or expired');}finally{setLoading(false);}};
  return <div className="login-page"><div className="login-bg-effects"><div className="login-orb login-orb-1"/><div className="login-orb login-orb-2"/></div><div className="login-card"><div className="login-logo"><div className="login-logo-icon"><HiOutlineKey/></div><h1>Choose a new password</h1><p>Use at least 10 characters.</p></div><form onSubmit={submit} className="login-form"><div className="input-group"><label>New password</label><input className="input" type="password" minLength={10} required value={password} onChange={e=>setPassword(e.target.value)}/></div><div className="input-group"><label>Confirm password</label><input className="input" type="password" minLength={10} required value={confirm} onChange={e=>setConfirm(e.target.value)}/></div><button className="btn btn-primary login-btn" disabled={loading||!params.get('token')}><HiOutlineShieldCheck/>{loading?'Updating…':'Reset Password'}</button></form><div className="login-toggle"><Link to="/login">Back to sign in</Link></div></div></div>;
}
