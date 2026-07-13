import axiosClient from './axiosClient';

export const login = (email, password) =>
  axiosClient.post('/auth/login', { email, password });

export const register = (fullName, email, password, role) =>
  axiosClient.post('/auth/register', { fullName, email, password, role });

export const refreshToken = (refreshToken) =>
  axiosClient.post('/auth/refresh', { refreshToken });
