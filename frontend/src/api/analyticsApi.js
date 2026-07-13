import axiosClient from './axiosClient';

export const getDashboard = () =>
  axiosClient.get('/analytics/dashboard');

export const getLogs = (params = {}) =>
  axiosClient.get('/analytics/logs', { params });

export const getGeography = () =>
  axiosClient.get('/analytics/geography');
