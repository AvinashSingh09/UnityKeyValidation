import axiosClient from './axiosClient';
export const getSessions = () => axiosClient.get('/security/sessions');
export const revokeSession = (id) => axiosClient.delete(`/security/sessions/${id}`);
export const changePassword = (data) => axiosClient.post('/security/change-password', data);
export const getUsers = () => axiosClient.get('/security/users');
export const createUser = (data) => axiosClient.post('/security/users', data);
export const updateUser = (id, data) => axiosClient.put(`/security/users/${id}`, data);
export const getAuditLogs = (params = {}) => axiosClient.get('/security/audit', { params });
