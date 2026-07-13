import axiosClient from './axiosClient';

export const getKeys = (params = {}) =>
  axiosClient.get('/keys', { params });

export const getKey = (id) =>
  axiosClient.get(`/keys/${id}`);

export const createKey = (data) =>
  axiosClient.post('/keys', data);

export const batchCreateKeys = (data) =>
  axiosClient.post('/keys/batch', data);

export const updateKey = (id, data) =>
  axiosClient.put(`/keys/${id}`, data);

export const revokeKey = (id) =>
  axiosClient.put(`/keys/${id}/revoke`);

export const suspendKey = (id) =>
  axiosClient.put(`/keys/${id}/suspend`);

export const reactivateKey = (id) =>
  axiosClient.put(`/keys/${id}/reactivate`);

export const deleteKey = (id) =>
  axiosClient.delete(`/keys/${id}`);

export const updateDevice = (keyId, hardwareId, data) =>
  axiosClient.put(`/keys/${keyId}/devices/${encodeURIComponent(hardwareId)}`, data);

export const removeDevice = (keyId, hardwareId) =>
  axiosClient.delete(`/keys/${keyId}/devices/${encodeURIComponent(hardwareId)}`);
