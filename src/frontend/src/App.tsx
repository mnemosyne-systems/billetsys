import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import AppFooter from './components/layout/AppFooter';
import AuthenticatedHeader from './components/layout/AuthenticatedHeader';
import LoginHeader from './components/layout/LoginHeader';
import useJson from './hooks/useJson';
import AppRoutes from './AppRoutes';
import type { Session } from './types/app';

function App() {
  const sessionState = useJson<Session>('/api/app/session');
  const session = sessionState.data;
  const location = useLocation();
  const isLoginRoute = location.pathname === '/login' && !session?.authenticated;
  const brandName = session?.installationCompanyName || 'billetsys';

  useEffect(() => {
    document.title = `${brandName}: billetsys`;
  }, [brandName]);

  return (
    <div className={isLoginRoute ? 'login-shell' : 'app-shell'}>
      {isLoginRoute ? <LoginHeader brandName={brandName} /> : <AuthenticatedHeader session={session} />}
      <main className={isLoginRoute ? 'login-main' : 'app-main'}>
        <AppRoutes sessionState={sessionState} />
      </main>
      <AppFooter className={isLoginRoute ? 'login-footer' : 'app-footer'} />
    </div>
  );
}

export default App;
