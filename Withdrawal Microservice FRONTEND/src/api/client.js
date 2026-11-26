import axios from "axios";

const TOKEN_KEY = "bank_token";
const NAME_KEY = "bank_name";

// sessionStorage: cleared when the tab closes. For production consider httpOnly cookies + CSRF protection.
export const session = {
  token: () => sessionStorage.getItem(TOKEN_KEY),
  name: () => sessionStorage.getItem(NAME_KEY) || "",
  save: (token, name) => {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(NAME_KEY, name);
  },
  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(NAME_KEY);
  },
};

const api = axios.create({ baseURL: "/api/v1", timeout: 20000 });

api.interceptors.request.use((config) => {
  const token = session.token();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith("/auth/");
    if (err.response?.status === 401 && !isAuthCall) {
      session.clear();
      window.dispatchEvent(new Event("auth:expired"));
    }
    return Promise.reject(err);
  }
);

export default api;
