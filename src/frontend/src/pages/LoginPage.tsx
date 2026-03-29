import { Navigate, useLocation } from 'react-router-dom';
import type { SessionPageProps } from '../types/app';

export default function LoginPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const location = useLocation();
  const error = new URLSearchParams(location.search).get('error');

  if (session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  return (
    <section className="login-card">
      <h1>Login</h1>
      <form className="auth-form" method="post" action="/login">
        <label>
          Username
          <input name="username" autoComplete="username" required />
        </label>
        <label>
          Password
          <input type="password" name="password" autoComplete="current-password" required />
        </label>
        {error && <p className="error-text">{error}</p>}
        <div className="login-actions">
          <button type="submit" className="primary-button">
            Login
          </button>
        </div>
      </form>
    </section>
  );
}
