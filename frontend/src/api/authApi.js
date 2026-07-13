import axiosClient from './axiosClient';

export const login = (email, password) =>
  axiosClient.post('/auth/login', { email, password });

export const register = (fullName, email, password) =>
  axiosClient.post('/auth/register', { fullName, email, password });

export const refreshToken = (refreshToken) =>
  axiosClient.post('/auth/refresh', { refreshToken });

export const getSetupStatus = () => axiosClient.get('/auth/setup-status');
export const forgotPassword = (email) => axiosClient.post('/auth/forgot-password', { email });
export const resetPassword = (token, newPassword) => axiosClient.post('/auth/reset-password', { token, newPassword });
export const logout = (refreshToken) => axiosClient.post('/auth/logout', { refreshToken });
