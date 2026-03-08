// lib/api.js
const baseURL = import.meta.env.MODE === "development" 
  ? "http://localhost:3000/api/v1" 
  : "/api/v1";

export const fetchInstance = async (endpoint, options = {}) => {
  const response = await fetch(`${baseURL}${endpoint}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    ...options
  });
  
  return response;
};