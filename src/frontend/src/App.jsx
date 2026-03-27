import { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';

function App() {
  const sessionState = useJson('/api/app/session');
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
        <Routes>
          <Route path="/" element={<HomePage sessionState={sessionState} />} />
          <Route path="/login" element={<LoginPage sessionState={sessionState} />} />
          <Route
            path="/error"
            element={
              <StatusPage
                sessionState={sessionState}
                title="Something went wrong"
                message="An unexpected error interrupted the request before the React shell could finish loading the page."
              />
            }
          />
          <Route
            path="/not-found"
            element={
              <StatusPage
                sessionState={sessionState}
                title="Page not found"
                message="The page you requested does not exist or is no longer available in the React shell."
              />
            }
          />
          <Route path="/profile" element={<ProfilePage sessionState={sessionState} />} />
          <Route path="/profile/password" element={<PasswordPage sessionState={sessionState} />} />
          <Route path="/reports" element={<ReportsPage sessionState={sessionState} />} />
          <Route path="/owner" element={<OwnerPage sessionState={sessionState} />} />
          <Route path="/owner/edit" element={<OwnerEditPage sessionState={sessionState} />} />
          <Route path="/users" element={<DirectoryUsersPage sessionState={sessionState} apiBase="/api/admin/users" basePath="/users" titleFallback="Users" />} />
          <Route path="/users/new" element={<DirectoryUserFormPage sessionState={sessionState} bootstrapBase="/api/admin/users/bootstrap" navigateFallback="/users" />} />
          <Route path="/users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/admin/users" backFallback="/users" />} />
          <Route path="/users/:id/edit" element={<DirectoryUserFormPage sessionState={sessionState} bootstrapBase="/api/admin/users/bootstrap" navigateFallback="/users" />} />
          <Route path="/tickets" element={<TicketWorkbenchPage sessionState={sessionState} />} />
          <Route path="/tickets/new" element={<TicketWorkbenchFormPage sessionState={sessionState} />} />
          <Route path="/tickets/:id/edit" element={<TicketWorkbenchFormPage sessionState={sessionState} />} />
          <Route path="/messages" element={<MessagesPage sessionState={sessionState} />} />
          <Route path="/messages/new" element={<MessageFormPage sessionState={sessionState} />} />
          <Route path="/messages/:id/edit" element={<MessageFormPage sessionState={sessionState} />} />
          <Route path="/attachments/:id" element={<AttachmentPage sessionState={sessionState} />} />
          <Route path="/companies" element={<CompaniesPage sessionState={sessionState} />} />
          <Route path="/companies/new" element={<CompanyFormPage sessionState={sessionState} mode="create" />} />
          <Route path="/companies/:id" element={<CompanyDetailPage sessionState={sessionState} />} />
          <Route path="/companies/:id/edit" element={<CompanyFormPage sessionState={sessionState} mode="edit" />} />
          <Route path="/support/tickets" element={<SupportTicketsPage sessionState={sessionState} view="assigned" />} />
          <Route path="/support/tickets/open" element={<SupportTicketsPage sessionState={sessionState} view="open" />} />
          <Route path="/support/tickets/closed" element={<SupportTicketsPage sessionState={sessionState} view="closed" />} />
          <Route path="/support/tickets/new" element={<SupportTicketCreatePage sessionState={sessionState} />} />
          <Route path="/support/tickets/:id" element={<SupportTicketDetailPage sessionState={sessionState} />} />
          <Route path="/support/users" element={<DirectoryUsersPage sessionState={sessionState} apiBase="/api/support/users" basePath="/support/users" titleFallback="Users" />} />
          <Route path="/support/users/new" element={<DirectoryUserFormPage sessionState={sessionState} bootstrapBase="/api/support/users/bootstrap" navigateFallback="/support/users" />} />
          <Route path="/support/support-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/support/support-users" backFallback="/support/users" />} />
          <Route path="/support/tam-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/support/tam-users" backFallback="/support/users" />} />
          <Route path="/support/superuser-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/support/superuser-users" backFallback="/support/users" />} />
          <Route path="/support/user-profiles/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/support/user-profiles" backFallback="/support/users" />} />
          <Route path="/support/companies/:id" element={<DirectoryCompanyDetailPage sessionState={sessionState} apiBase="/api/support/companies" backFallback="/support/users" />} />
          <Route path="/tam/users" element={<DirectoryUsersPage sessionState={sessionState} apiBase="/api/tam/users" basePath="/tam/users" titleFallback="Users" />} />
          <Route path="/tam/users/new" element={<DirectoryUserFormPage sessionState={sessionState} bootstrapBase="/api/tam/users/bootstrap" navigateFallback="/tam/users" />} />
          <Route path="/user/support-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/user/support-users" backFallback="/user/tickets" />} />
          <Route path="/user/tam-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/user/tam-users" backFallback="/user/tickets" />} />
          <Route path="/user/superuser-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/user/superuser-users" backFallback="/user/tickets" />} />
          <Route path="/user/user-profiles/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/user/user-profiles" backFallback="/user/tickets" />} />
          <Route path="/user/companies/:id" element={<DirectoryCompanyDetailPage sessionState={sessionState} apiBase="/api/user/companies" backFallback="/user/tickets" />} />
          <Route path="/superuser/users" element={<DirectoryUsersPage sessionState={sessionState} apiBase="/api/superuser/users" basePath="/superuser/users" titleFallback="Users" />} />
          <Route path="/superuser/users/new" element={<DirectoryUserFormPage sessionState={sessionState} bootstrapBase="/api/superuser/users/bootstrap" navigateFallback="/superuser/users" />} />
          <Route path="/superuser/support-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/superuser/support-users" backFallback="/superuser/users" />} />
          <Route path="/superuser/superuser-users/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/superuser/superuser-users" backFallback="/superuser/users" />} />
          <Route path="/superuser/user-profiles/:id" element={<DirectoryUserDetailPage sessionState={sessionState} apiBase="/api/superuser/user-profiles" backFallback="/superuser/users" />} />
          <Route path="/superuser/companies/:id" element={<DirectoryCompanyDetailPage sessionState={sessionState} apiBase="/api/superuser/companies" backFallback="/superuser/users" />} />
          <Route path="/user/tickets" element={<SupportTicketsPage sessionState={sessionState} view="assigned" apiBase="/api/user/tickets" basePath="/user/tickets" createFallbackPath="/user/tickets/new" />} />
          <Route path="/user/tickets/open" element={<SupportTicketsPage sessionState={sessionState} view="open" apiBase="/api/user/tickets" basePath="/user/tickets" createFallbackPath="/user/tickets/new" />} />
          <Route path="/user/tickets/closed" element={<SupportTicketsPage sessionState={sessionState} view="closed" apiBase="/api/user/tickets" basePath="/user/tickets" createFallbackPath="/user/tickets/new" />} />
          <Route path="/user/tickets/new" element={<SupportTicketCreatePage sessionState={sessionState} apiBase="/api/user/tickets/bootstrap" backPath="/user/tickets" submitFallbackPath="/user/tickets" title="New ticket" navigateTo="/user/tickets" compactCreateActions hideEntitlementLevel />} />
          <Route path="/user/tickets/:id" element={<SupportTicketDetailPage sessionState={sessionState} apiBase="/api/user/tickets" backPath="/user/tickets" titleFallback="Ticket" secondaryUsersLabel="TAM" />} />
          <Route path="/superuser/tickets" element={<SupportTicketsPage sessionState={sessionState} view="assigned" apiBase="/api/superuser/tickets" basePath="/superuser/tickets" createFallbackPath="/superuser/tickets/new" />} />
          <Route path="/superuser/tickets/open" element={<SupportTicketsPage sessionState={sessionState} view="open" apiBase="/api/superuser/tickets" basePath="/superuser/tickets" createFallbackPath="/superuser/tickets/new" />} />
          <Route path="/superuser/tickets/closed" element={<SupportTicketsPage sessionState={sessionState} view="closed" apiBase="/api/superuser/tickets" basePath="/superuser/tickets" createFallbackPath="/superuser/tickets/new" />} />
          <Route path="/superuser/tickets/new" element={<SupportTicketCreatePage sessionState={sessionState} apiBase="/api/superuser/tickets/bootstrap" backPath="/superuser/tickets" submitFallbackPath="/superuser/tickets" navigateTo="/superuser/tickets" />} />
          <Route path="/superuser/tickets/:id" element={<SupportTicketDetailPage sessionState={sessionState} apiBase="/api/superuser/tickets" backPath="/superuser/tickets" titleFallback="Superuser ticket" secondaryUsersLabel="Superusers" />} />
          <Route path="/articles" element={<ArticlesPage sessionState={sessionState} />} />
          <Route path="/articles/new" element={<ArticleFormPage sessionState={sessionState} mode="create" />} />
          <Route path="/articles/:id" element={<ArticleDetailPage sessionState={sessionState} />} />
          <Route path="/articles/:id/edit" element={<ArticleFormPage sessionState={sessionState} mode="edit" />} />
          <Route path="/categories" element={<CategoriesPage sessionState={sessionState} />} />
          <Route path="/categories/new" element={<CategoryFormPage sessionState={sessionState} mode="create" />} />
          <Route path="/categories/:id" element={<CategoryDetailPage sessionState={sessionState} />} />
          <Route path="/categories/:id/edit" element={<CategoryFormPage sessionState={sessionState} mode="edit" />} />
          <Route path="/entitlements" element={<EntitlementsPage sessionState={sessionState} />} />
          <Route path="/entitlements/new" element={<EntitlementFormPage sessionState={sessionState} mode="create" />} />
          <Route path="/entitlements/:id" element={<EntitlementDetailPage sessionState={sessionState} />} />
          <Route path="/entitlements/:id/edit" element={<EntitlementFormPage sessionState={sessionState} mode="edit" />} />
          <Route path="/levels" element={<LevelsPage sessionState={sessionState} />} />
          <Route path="/levels/new" element={<LevelFormPage sessionState={sessionState} mode="create" />} />
          <Route path="/levels/:id" element={<LevelDetailPage sessionState={sessionState} />} />
          <Route path="/levels/:id/edit" element={<LevelFormPage sessionState={sessionState} mode="edit" />} />
          <Route
            path="*"
            element={<StatusPage sessionState={sessionState} title="Page not found" message="That route is not available in the React shell." />}
          />
        </Routes>
      </main>
      <AppFooter className={isLoginRoute ? 'login-footer' : 'app-footer'} />
    </div>
  );
}

function AuthenticatedHeader({ session }) {
  const [now, setNow] = useState(() => new Date());
  const location = useLocation();
  const ticketMenuRef = useRef(null);
  const profileMenuRef = useRef(null);
  const role = session?.role;
  const showTicketMenu = role === 'support' || role === 'user';
  const ticketMenuBasePath = role === 'support' ? '/support/tickets' : '/user/tickets';
  const ticketCountsState = useJson(ticketCountsApiPath(role));
  const ticketAlarmState = useText(showRoleTicketAlarm(role) ? '/tickets/alarm/status' : '');

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  const navigation = headerNavigation(session);
  const brandHref = normalizeClientPath(session?.homePath) || '/';
  const userName = session?.displayName || session?.username || 'Guest';
  const assignedCount = ticketCountsState.data?.assignedCount ?? 0;
  const openCount = ticketCountsState.data?.openCount ?? 0;
  const ticketLabel = ticketLabelForRole(role, assignedCount, openCount);
  const showTicketAlarm = String(ticketAlarmState.data || '').trim().toLowerCase() === 'true';
  const isTicketRoute = isRoleTicketRoute(role, location.pathname);
  const rssHref = rssPath(role);
  const closeDetailsMenu = event => {
    const menu = event.currentTarget.closest('details');
    if (menu) {
      menu.open = false;
    }
  };
  useEffect(() => {
    if (ticketMenuRef.current) {
      ticketMenuRef.current.open = false;
    }
    if (profileMenuRef.current) {
      profileMenuRef.current.open = false;
    }
  }, [location.pathname, location.search]);

  useEffect(() => {
    const handlePointerDown = event => {
      [ticketMenuRef.current, profileMenuRef.current].forEach(menu => {
        if (!menu || !menu.open || menu.contains(event.target)) {
          return;
        }
        menu.open = false;
      });
    };
    document.addEventListener('pointerdown', handlePointerDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
    };
  }, []);

  return (
    <header className="shell-header">
      <div className="header-left">
        <SmartLink className="shell-brand" href={brandHref}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M7 8h10M7 12h10M7 16h6" />
            <path d="M6 8l1 1 2-2" />
          </svg>
          {session?.installationCompanyName || 'billetsys'}
        </SmartLink>
        {navigation.length > 0 && (
          <nav className="shell-nav" aria-label="Primary">
            {navigation.map(link => {
              if (showTicketMenu && link.label === 'Tickets') {
                return (
                  <details key={link.href} className="shell-nav-menu" ref={ticketMenuRef}>
                    <summary className={`shell-nav-link shell-nav-summary${isTicketRoute ? ' active' : ''}`}>
                      {ticketLabel}
                      <span
                        className={`ticket-alarm${showTicketAlarm ? ' is-visible' : ''}`}
                        aria-hidden={!showTicketAlarm}
                        title="SLA alarm"
                      >
                        🚨
                      </span>
                    </summary>
                    <div className="shell-nav-dropdown">
                      <SmartLink className="shell-nav-dropdown-link" href={ticketMenuBasePath} onClick={closeDetailsMenu}>
                        Active tickets
                      </SmartLink>
                      <SmartLink className="shell-nav-dropdown-link" href={`${ticketMenuBasePath}/open`} onClick={closeDetailsMenu}>
                        Open tickets
                      </SmartLink>
                      <SmartLink className="shell-nav-dropdown-link" href={`${ticketMenuBasePath}/closed`} onClick={closeDetailsMenu}>
                        Closed tickets
                      </SmartLink>
                    </div>
                  </details>
                );
              }

              if (link.label === 'Tickets') {
                return (
                  <SmartLink key={link.href} className={`shell-nav-link${isTicketRoute ? ' active' : ''}`} href={link.href}>
                    {ticketLabel}
                    <span
                      className={`ticket-alarm${showTicketAlarm ? ' is-visible' : ''}`}
                      aria-hidden={!showTicketAlarm}
                      title="SLA alarm"
                    >
                      🚨
                    </span>
                  </SmartLink>
                );
              }

              return (
                <SmartLink key={link.href} className="shell-nav-link" href={link.href}>
                  {link.label}
                </SmartLink>
              );
            })}
          </nav>
        )}
      </div>
      <div className="header-actions">
        {session?.authenticated && (
          <>
            {rssHref && (
              <a className="icon-link" href={rssHref} title="RSS feed" aria-label="RSS feed">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="6" cy="18" r="1.8" fill="currentColor" stroke="none" />
                  <path d="M4 11a9 9 0 0 1 9 9" />
                  <path d="M4 5a15 15 0 0 1 15 15" />
                </svg>
              </a>
            )}
            <details className="profile-menu" ref={profileMenuRef}>
              <summary className="user-summary" aria-label={userName}>
                {session?.logoBase64 ? (
                  <img className="user-logo" src={session.logoBase64} alt="User logo" />
                ) : (
                  <span className="user-avatar" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M5 19c1.8-3.1 4.4-4.7 7-4.7s5.2 1.6 7 4.7" />
                    </svg>
                  </span>
                )}
              </summary>
              <div className="profile-dropdown">
                <SmartLink className="profile-link" href="/profile" onClick={closeDetailsMenu}>
                  Profile
                </SmartLink>
                <SmartLink className="profile-link" href="/profile/password" onClick={closeDetailsMenu}>
                  Password
                </SmartLink>
                <a className="profile-link" href="/logout" onClick={closeDetailsMenu}>
                  Sign out
                </a>
              </div>
            </details>
          </>
        )}
        <span className="header-clock">
          {now.toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit'
          })}
        </span>
      </div>
    </header>
  );
}

function LoginHeader({ brandName }) {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  return (
    <header className="login-header">
      <a className="login-brand" href="/">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <path d="M7 8h10M7 12h10M7 16h6" />
          <path d="M6 8l1 1 2-2" />
        </svg>
        {brandName}
      </a>
      <span className="login-header-clock">
        {now.toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit'
        })}
      </span>
    </header>
  );
}

function AppFooter({ className }) {
  return (
    <footer className={className}>
      Copyright © {new Date().getFullYear()} Powered by{' '}
      <a href="https://github.com/mnemosyne-systems/billetsys" target="_blank" rel="noreferrer">
        billetsys
      </a>
    </footer>
  );
}

function MarkdownContent({ children }) {
  return <ReactMarkdown rehypePlugins={[rehypeHighlight]}>{children || ''}</ReactMarkdown>;
}

const MARKDOWN_CODE_BLOCK_LANGUAGES = [
  ['', 'Text'],
  ['bash', 'Bash'],
  ['c', 'C'],
  ['cpp', 'C++'],
  ['go', 'Go'],
  ['html', 'HTML'],
  ['java', 'Java'],
  ['javascript', 'JS'],
  ['json', 'JSON'],
  ['python', 'Py'],
  ['rust', 'Rust'],
  ['sql', 'SQL'],
  ['xml', 'XML']
];

function MarkdownEditor({ value, onChange, inputRef, rows = 10, name, required = false }) {
  const fallbackInputRef = useRef(null);
  const textareaRef = inputRef || fallbackInputRef;

  const applyAction = (action, option) => {
    if (!textareaRef.current) {
      return;
    }
    const textarea = textareaRef.current;
    const selectionStart = textarea.selectionStart ?? value.length;
    const selectionEnd = textarea.selectionEnd ?? value.length;
    const selectedText = value.slice(selectionStart, selectionEnd);
    const replacement = markdownActionText(action, selectedText, option);
    if (replacement == null) {
      return;
    }
    const nextValue = `${value.slice(0, selectionStart)}${replacement}${value.slice(selectionEnd)}`;
    onChange(nextValue);
    requestAnimationFrame(() => {
      textarea.focus();
      const cursor = selectionStart + replacement.length;
      textarea.setSelectionRange(cursor, cursor);
    });
  };

  return (
    <div className="markdown-editor">
      <div className="markdown-toolbar">
        <button type="button" onClick={() => applyAction('bold')} aria-label="Bold">
          <strong>B</strong>
        </button>
        <button type="button" onClick={() => applyAction('italic')} aria-label="Italic">
          <em>I</em>
        </button>
        <button type="button" onClick={() => applyAction('heading')} aria-label="Heading">
          H
        </button>
        <button type="button" onClick={() => applyAction('list')} aria-label="List">
          •
        </button>
        <button type="button" onClick={() => applyAction('quote')} aria-label="Quote">
          "
        </button>
        <button type="button" onClick={() => applyAction('code')} aria-label="Code">
          {'</>'}
        </button>
        <select
          className="markdown-toolbar-select"
          defaultValue="__label"
          onChange={event => {
            const selectedLanguage = event.target.value;
            if (selectedLanguage === '__label') {
              return;
            }
            applyAction('code-block', selectedLanguage);
            event.target.value = '__label';
          }}
          aria-label="Code block language"
        >
          <option value="__label" hidden>
            Code
          </option>
          {MARKDOWN_CODE_BLOCK_LANGUAGES.map(([code, label]) => (
            <option key={code || 'plaintext'} value={code}>
              {label}
            </option>
          ))}
        </select>
        <button type="button" onClick={() => applyAction('link')} aria-label="Link">
          Link
        </button>
        <button type="button" onClick={() => applyAction('media')} aria-label="Media">
          Img
        </button>
      </div>
      <div className="markdown-panels">
        <div className="markdown-panel">
          <div className="markdown-panel-header">Write</div>
          <textarea ref={textareaRef} name={name} rows={rows} value={value} onChange={event => onChange(event.target.value)} required={required} />
        </div>
        <div className="markdown-panel">
          <div className="markdown-panel-header">Preview</div>
          <div className="markdown-preview markdown-output">
            <MarkdownContent>{value || ''}</MarkdownContent>
          </div>
        </div>
      </div>
    </div>
  );
}

function profileInitial(fullName, username, email) {
  const firstName = (fullName || '')
    .trim()
    .split(/\s+/)
    .find(Boolean);
  const source = firstName || username || email || '?';
  return source.charAt(0).toUpperCase();
}

function ProfileLogoPreview({ logoBase64, fullName, username, email }) {
  const initial = profileInitial(fullName, username, email);

  return (
    <div className="profile-logo-preview">
      {logoBase64 ? (
        <img className="profile-logo-image" src={logoBase64} alt="Profile logo" />
      ) : (
        <span className="profile-logo-fallback" aria-label="Profile initial">
          {initial}
        </span>
      )}
    </div>
  );
}

function HomePage({ sessionState }) {
  const session = sessionState.data;
  const adminLinks = orderedNavigation(session?.navigation, [
    'Owner',
    'Companies',
    'Users',
    'Entitlements',
    'Levels',
    'Categories',
    'Articles',
    'Reports'
  ]);

  if (sessionState.loading) {
    return (
      <section className="panel">
        <p>Loading session...</p>
      </section>
    );
  }

  if (sessionState.error) {
    return (
      <section className="panel">
        <p className="error-text">{sessionState.error}</p>
      </section>
    );
  }

  if (!session?.authenticated) {
    return <Navigate replace to="/login" />;
  }

  const homePath = normalizeClientPath(session.homePath) || '/';
  if (homePath !== '/') {
    return <Navigate replace to={homePath} />;
  }

  return (
    <section className="dashboard-panel">
      <div className="dashboard-card-grid">
        {adminLinks.map(link => (
          <SmartLink key={link.href} className="dashboard-card" href={link.href}>
            {link.label}
          </SmartLink>
        ))}
      </div>
    </section>
  );
}

function LoginPage({ sessionState }) {
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

function ProfilePage({ sessionState }) {
  const location = useLocation();
  const profileState = useJson('/api/profile');
  const profile = profileState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '', saved: false });
  const routeError = new URLSearchParams(location.search).get('error') || '';
  const logoInputRef = useRef(null);

  useEffect(() => {
    if (profile) {
      setFormState({
        name: profile.username || '',
        email: profile.email || '',
        fullName: profile.fullName || '',
        social: profile.social || '',
        phoneNumber: profile.phoneNumber || '',
        phoneExtension: profile.phoneExtension || '',
        countryId: profile.countryId ? String(profile.countryId) : '',
        timezoneId: profile.timezoneId ? String(profile.timezoneId) : '',
        companyId: profile.currentCompanyId ? String(profile.currentCompanyId) : '',
        logoBase64: profile.logoBase64 || ''
      });
    }
  }, [profile]);

  const availableTimezones =
    profile?.timezones?.filter(timezone => !formState?.countryId || String(timezone.countryId) === formState.countryId) ||
    [];

  const updateField = (field, value) => {
    setFormState(current => ({ ...current, [field]: value }));
    setSaveState(current => ({ ...current, saved: false }));
  };

  const openLogoPicker = () => {
    logoInputRef.current?.click();
  };

  const uploadLogo = event => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      setSaveState({ saving: false, error: 'Logo must be an image file.', saved: false });
      return;
    }
    const reader = new FileReader();
    reader.onload = loadEvent => {
      const result = loadEvent.target?.result;
      if (typeof result !== 'string') {
        setSaveState({ saving: false, error: 'Unable to read logo file.', saved: false });
        return;
      }
      updateField('logoBase64', result);
    };
    reader.onerror = () => {
      setSaveState({ saving: false, error: 'Unable to read logo file.', saved: false });
    };
    reader.readAsDataURL(file);
  };

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '', saved: false });
    try {
      const response = await fetch('/api/profile', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...formState,
          name: formState.name,
          countryId: formState.countryId ? Number(formState.countryId) : null,
          timezoneId: formState.timezoneId ? Number(formState.timezoneId) : null,
          companyId: formState.companyId ? Number(formState.companyId) : null
        })
      });
      if (!response.ok) {
        throw new Error((await response.text()) || 'Unable to save profile.');
      }
      const updated = await response.json();
      setFormState(current => ({
        ...current,
        name: updated.username || '',
        email: updated.email || '',
        fullName: updated.fullName || '',
        social: updated.social || '',
        phoneNumber: updated.phoneNumber || '',
        phoneExtension: updated.phoneExtension || '',
        countryId: updated.countryId ? String(updated.countryId) : '',
        timezoneId: updated.timezoneId ? String(updated.timezoneId) : '',
        companyId: updated.currentCompanyId ? String(updated.currentCompanyId) : '',
        logoBase64: updated.logoBase64 || ''
      }));
      setSaveState({ saving: false, error: '', saved: true });
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save profile.', saved: false });
    }
  };

  return (
    <section className="panel">
      <DataState state={profileState} emptyMessage="Profile unavailable." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && profile && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <form className="owner-form" onSubmit={submit}>
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Username
                    <input value={formState.name} onChange={event => updateField('name', event.target.value)} required />
                  </label>
                  <label>
                    Full name
                    <input value={formState.fullName} onChange={event => updateField('fullName', event.target.value)} />
                  </label>
                  <label>
                    Email
                    <input type="email" value={formState.email} onChange={event => updateField('email', event.target.value)} />
                  </label>
                  <label>
                    Social
                    <input value={formState.social} onChange={event => updateField('social', event.target.value)} />
                  </label>
                  <label>
                    Phone number
                    <input value={formState.phoneNumber} onChange={event => updateField('phoneNumber', event.target.value)} />
                  </label>
                  <label>
                    Phone extension
                    <input value={formState.phoneExtension} onChange={event => updateField('phoneExtension', event.target.value)} />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={event => {
                        const nextCountryId = event.target.value;
                        const timezoneStillValid = profile.timezones.some(
                          timezone => String(timezone.id) === formState.timezoneId && String(timezone.countryId) === nextCountryId
                        );
                        setFormState(current => ({
                          ...current,
                          countryId: nextCountryId,
                          timezoneId: timezoneStillValid ? current.timezoneId : ''
                        }));
                        setSaveState(current => ({ ...current, saved: false }));
                      }}
                    >
                      <option value="">Select a country</option>
                      {profile.countries.map(country => (
                        <option key={country.id} value={country.id}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select value={formState.timezoneId} onChange={event => updateField('timezoneId', event.target.value)}>
                      <option value="">Select a time zone</option>
                      {availableTimezones.map(timezone => (
                        <option key={timezone.id} value={timezone.id}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  {profile.canSelectCompany ? (
                    <label>
                      Company
                      <select value={formState.companyId} onChange={event => updateField('companyId', event.target.value)}>
                        <option value="">Select a company</option>
                        {profile.companies.map(company => (
                          <option key={company.id} value={company.id}>
                            {company.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  ) : (
                    <label className="ticket-detail-spacer" aria-hidden="true">
                      <input value="-" readOnly />
                    </label>
                  )}
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Logo</div>
                    <div className="owner-detail-panel-body profile-logo-panel">
                      <div className="profile-logo-panel-content">
                        <ProfileLogoPreview
                          logoBase64={formState.logoBase64}
                          fullName={formState.fullName}
                          username={formState.name}
                          email={formState.email}
                        />
                        <input ref={logoInputRef} type="file" accept="image/*" className="hidden-file-input" onChange={uploadLogo} />
                        <button type="button" className="primary-button profile-logo-upload-button" onClick={openLogoPicker}>
                          Upload
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="detail-card-spacer" aria-hidden="true" />
                </div>

                {(saveState.error || (!saveState.saved && routeError)) && <p className="error-text">{saveState.error || routeError}</p>}
                {saveState.saved && <p className="success-text">Profile saved.</p>}

                <div className="button-row button-row-end">
                  <button type="submit" className="primary-button" disabled={saveState.saving}>
                    {saveState.saving ? 'Saving...' : 'Save'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}

function PasswordPage({ sessionState }) {
  const location = useLocation();
  const [formState, setFormState] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [saveState, setSaveState] = useState({ saving: false, error: '', saved: false });
  const routeError = new URLSearchParams(location.search).get('error') || '';

  const submit = async event => {
    event.preventDefault();
    setSaveState({ saving: true, error: '', saved: false });
    try {
      const response = await fetch('/api/profile/password', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formState)
      });
      if (!response.ok) {
        throw new Error((await response.text()) || 'Unable to update password.');
      }
      setFormState({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setSaveState({ saving: false, error: '', saved: true });
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to update password.', saved: false });
    }
  };

  return (
    <section className="panel auth-panel">
      <DataState
        state={{ loading: false, unauthorized: !sessionState.data?.authenticated, forbidden: false, error: '', empty: false }}
        emptyMessage=""
        signInHref="/login"
      >
        <form className="auth-form" onSubmit={submit}>
          <label>
            Old password
            <input
              type="password"
              value={formState.oldPassword}
              onChange={event => setFormState(current => ({ ...current, oldPassword: event.target.value }))}
              required
            />
          </label>
          <label>
            New password
            <input
              type="password"
              value={formState.newPassword}
              onChange={event => setFormState(current => ({ ...current, newPassword: event.target.value }))}
              required
            />
          </label>
          <label>
            Confirm password
            <input
              type="password"
              value={formState.confirmPassword}
              onChange={event => setFormState(current => ({ ...current, confirmPassword: event.target.value }))}
              required
            />
          </label>
          {(saveState.error || (!saveState.saved && routeError)) && <p className="error-text">{saveState.error || routeError}</p>}
          {saveState.saved && <p className="success-text">Password updated.</p>}
          <div className="button-row">
            <button type="submit" className="primary-button" disabled={saveState.saving}>
              {saveState.saving ? 'Saving...' : 'Update'}
            </button>
          </div>
        </form>
      </DataState>
    </section>
  );
}

function ReportsPage({ sessionState }) {
  const session = sessionState.data;
  const role = session?.role;
  const supportsReports = ['admin', 'tam', 'superuser'].includes(role || '');
  const [filters, setFilters] = useState({ companyId: '', period: 'all' });
  const chartScriptState = useExternalScript('/webjars/chart.js/4.5.1/dist/chart.umd.js');
  const chartInstancesRef = useRef({});
  const reportUrl = supportsReports
    ? `/api/reports${toQueryString({ companyId: filters.companyId || undefined, period: filters.period || undefined })}`
    : null;
  const reportsState = useJson(reportUrl);
  const reports = reportsState.data;

  useEffect(() => {
    if (reports && !filters.companyId && reports.selectedCompanyId) {
      setFilters(current => ({ ...current, companyId: String(reports.selectedCompanyId) }));
    }
  }, [reports, filters.companyId]);

  const onChartReady = (name, instance) => {
    if (instance) {
      chartInstancesRef.current[name] = instance;
      return;
    }
    delete chartInstancesRef.current[name];
  };

  const exportReport = () => {
    if (!reports?.exportPath) {
      return;
    }
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `${reports.exportPath}${toQueryString({ companyId: filters.companyId || undefined, period: filters.period || undefined })}`;

    const imageFields = {
      statusChart: chartInstancesRef.current.statusChart,
      categoryChart: chartInstancesRef.current.categoryChart,
      companyChart: chartInstancesRef.current.companyChart,
      timeChart: chartInstancesRef.current.timeChart,
      responseTimeChart: chartInstancesRef.current.responseTimeChart,
      resolutionTimeChart: chartInstancesRef.current.resolutionTimeChart,
      histogramChart: chartInstancesRef.current.histogramChart
    };

    Object.entries(imageFields).forEach(([name, chart]) => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = chart ? chart.toBase64Image() : '';
      form.appendChild(input);
    });

    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
  };

  if (!supportsReports) {
    return (
      <section className="panel">
        <h2>Reports</h2>
        <p className="muted-text">Reports are available for admin, TAM, and superuser roles.</p>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Reports</h2>
        </div>
      </div>

      <DataState state={reportsState} emptyMessage="No report data available." signInHref={sessionState.data?.homePath || '/login'}>
        {reports && (
          <div className="report-layout">
            {reports.showCompanyFilter ? (
              <div className="filter-row">
                <label>
                  Company
                  <select value={filters.companyId} onChange={event => setFilters(current => ({ ...current, companyId: event.target.value }))}>
                    <option value="">All companies</option>
                    {(reports.companies || []).map(company => (
                      <option key={company.id} value={company.id}>
                        {company.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            ) : (
              <p className="report-summary">
                <strong>Company:</strong> {reports.companyName}
              </p>
            )}
            <p className="report-summary">
              Total tickets: <strong>{reports.totalTickets}</strong>
            </p>

            <div className="report-grid">
              <ReportChartCard
                chartKey="statusChart"
                title="Tickets by Status"
                type="pie"
                items={reports.status}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                colorMap={REPORT_STATUS_COLORS}
                onChartReady={onChartReady}
              />
              <ReportChartCard
                chartKey="categoryChart"
                title="Tickets by Category"
                type="bar"
                items={reports.category}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                integerScale
                onChartReady={onChartReady}
              />
              {reports.showCompanyChart && (
                <ReportChartCard
                  chartKey="companyChart"
                  title="Tickets by Company"
                  type="bar"
                  items={reports.company}
                  scriptReady={chartScriptState.loaded}
                  scriptError={chartScriptState.error}
                  integerScale
                  onChartReady={onChartReady}
                />
              )}
              <ReportChartCard
                chartKey="timeChart"
                title="Ticket Volume Over Time"
                type="line"
                items={reports.timeline}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                integerScale
                fill
                onChartReady={onChartReady}
              >
                <label className="report-inline-filter">
                  Period
                  <select value={filters.period} onChange={event => setFilters(current => ({ ...current, period: event.target.value }))}>
                    <option value="all">All</option>
                    <option value="year">Year</option>
                    <option value="month">Month</option>
                  </select>
                </label>
              </ReportChartCard>
              <ReportChartCard
                chartKey="responseTimeChart"
                title="Avg. First Response Time (hours)"
                type="bar"
                items={reports.firstResponse}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                onChartReady={onChartReady}
              />
              <section className="detail-card report-chart-card">
                <div className="report-title-row">
                  <h3>Resolution Time</h3>
                </div>
                <ReportChartCanvas
                  chartKey="histogramChart"
                  type="bar"
                  items={reports.histogram.map(bucket => ({ label: bucket.label, value: bucket.count }))}
                  scriptReady={chartScriptState.loaded}
                  scriptError={chartScriptState.error}
                  integerScale
                  onChartReady={onChartReady}
                />
                <table className="report-histogram-table">
                  <thead>
                    <tr>
                      <th>Duration</th>
                      <th>Count</th>
                      <th>Tickets</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(reports.histogram || []).map(bucket => (
                      <tr key={bucket.label}>
                        <td>{bucket.label}</td>
                        <td>{bucket.count}</td>
                        <td>
                          {bucket.tickets.length === 0 ? (
                            '—'
                          ) : (
                            bucket.tickets.map((ticket, index) => (
                              <span key={ticket.id}>
                                <a href={`/tickets/${ticket.id}`}>{ticket.name}</a>
                                {index < bucket.tickets.length - 1 ? ', ' : ''}
                              </span>
                            ))
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
              <ReportChartCard
                chartKey="resolutionTimeChart"
                title="Avg. Resolution Time (hours)"
                type="bar"
                items={reports.resolutionTime}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                onChartReady={onChartReady}
              />
            </div>

            <div className="button-row">
              <button type="button" className="action-button export-btn" onClick={exportReport} disabled={!chartScriptState.loaded}>
                Export
              </button>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}

function StatusPage({ sessionState, title, message }) {
  const homeHref = sessionState.data?.homePath || '/login';

  return (
    <section className="panel auth-panel">
      <div className="section-header">
        <div>
          <h2>{title}</h2>
          <p className="section-copy">{message}</p>
        </div>
      </div>
      <div className="button-row">
        <SmartLink className="primary-button" href={homeHref}>
          Return to app
        </SmartLink>
      </div>
    </section>
  );
}

function ArticlesPage({ sessionState }) {
  const articlesState = useJson('/api/articles');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Articles</h2>
        </div>
        <div className="button-row">
          {articlesState.data?.canCreate && (
            <SmartLink className="primary-button" href={articlesState.data.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={articlesState} emptyMessage="No articles are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="ticket-table-wrap">
          <table className="support-ticket-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Tags</th>
              </tr>
            </thead>
            <tbody>
              {(articlesState.data?.items || []).map(article => (
                <tr key={article.id}>
                  <td>
                    <Link className="inline-link" to={`/articles/${article.id}`}>
                      {article.title}
                    </Link>
                  </td>
                  <td>{article.tags || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataState>
    </section>
  );
}

function ArticleDetailPage({ sessionState }) {
  const { id } = useParams();
  const articleState = useJson(id ? `/api/articles/${id}` : null);
  const article = articleState.data;

  return (
    <section className="panel">
      <DataState state={articleState} emptyMessage="Article not found." signInHref={sessionState.data?.homePath || '/login'}>
        {article && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <h1>{article.title || 'Article details'}</h1>
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Title
                    <input value={article.title || '—'} readOnly />
                  </label>
                  <label>
                    Tags
                    <input value={article.tags || '—'} readOnly />
                  </label>
                  <div className="owner-detail-panel form-span-2">
                    <div className="owner-detail-panel-label">Body</div>
                    <div className="owner-detail-panel-body">
                      {article.body ? (
                        <div className="markdown-output">
                          <MarkdownContent>{article.body}</MarkdownContent>
                        </div>
                      ) : (
                        <p className="muted-text">No body.</p>
                      )}
                    </div>
                  </div>
                  <div className="owner-detail-panel form-span-2">
                    <div className="owner-detail-panel-label">Attachments</div>
                    <div className="owner-detail-panel-body">
                      {article.attachments.length === 0 ? (
                        <p className="muted-text">No attachments.</p>
                      ) : (
                        <div className="attachment-table">
                          <div className="attachment-row attachment-header-row">
                            <span>Name</span>
                            <span>Mimetype</span>
                            <span>Size</span>
                          </div>
                          {article.attachments.map(attachment => (
                            <div key={attachment.id} className="attachment-row">
                              <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                                {attachment.name}
                              </a>
                              <span>{attachment.mimeType}</span>
                              <span>{attachment.sizeLabel}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {(article.canDelete || (article.canEdit && article.editPath)) && (
              <div className={`button-row${article.canDelete && article.canEdit && article.editPath ? ' button-row-split' : ' button-row-end'} admin-detail-actions`}>
                {article.canDelete && <DeleteArticleButton articleId={article.id} label="Delete" />}
                {article.canEdit && article.editPath && (
                  <SmartLink className="primary-button" href={article.editPath}>
                    Edit
                  </SmartLink>
                )}
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function ArticleFormPage({ sessionState, mode }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const articleState = useJson(mode === 'edit' && id ? `/api/articles/${id}` : '/api/articles/bootstrap');
  const article = articleState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const [files, setFiles] = useState([]);
  const bodyInputRef = useRef(null);
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!article) {
      return;
    }
    if (isEdit) {
      setFormState({
        title: article.title || '',
        tags: article.tags || '',
        body: article.body || ''
      });
      return;
    }
    setFormState({ title: '', tags: '', body: '' });
  }, [article, isEdit]);

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postMultipart(isEdit ? `/articles/${id}` : '/articles', [
        ['title', formState.title],
        ['tags', formState.tags],
        ['body', formState.body],
        ...files.map(file => ['attachments', file])
      ]);
      navigate(isEdit && id ? `/articles/${id}` : '/articles');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save article.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <DataState state={articleState} emptyMessage="Article unavailable." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && article && article.canEdit && (
          <div className="form-card ticket-detail-card">
            {isEdit && <h1>{formState.title || 'Edit article'}</h1>}
            <form className="owner-form" onSubmit={submit}>
              <div className="owner-form-grid ticket-detail-grid">
                <label>
                  Title
                  <input value={formState.title} onChange={event => setFormState(current => ({ ...current, title: event.target.value }))} required />
                </label>
                <label>
                  Tags
                  <input value={formState.tags} onChange={event => setFormState(current => ({ ...current, tags: event.target.value }))} />
                </label>
                <label className="form-span-2">
                  Body
                  <MarkdownEditor
                    value={formState.body}
                    onChange={value => setFormState(current => ({ ...current, body: value }))}
                    inputRef={bodyInputRef}
                    rows={12}
                    required
                  />
                </label>
              </div>

              <AttachmentPicker files={files} onFilesChange={setFiles} existingAttachments={article.attachments || []} />

              {saveState.error && <p className="error-text">{saveState.error}</p>}

              <div className={`button-row${isEdit ? ' button-row-split' : ' button-row-end'}`}>
                {isEdit && article.id && article.canDelete && <DeleteArticleButton articleId={article.id} label="Delete" />}
                <button type="submit" className="primary-button" disabled={saveState.saving}>
                  {saveState.saving ? 'Saving...' : isEdit ? 'Save' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        )}
      </DataState>
    </section>
  );
}

function OwnerPage({ sessionState }) {
  const ownerState = useJson('/api/owner');
  const owner = ownerState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Owner profile</h2>
        </div>
      </div>

      <DataState state={ownerState} emptyMessage="Owner company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {owner && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={owner.name || '—'} readOnly />
                  </label>
                  <label>
                    Phone
                    <input value={owner.phoneNumber || '—'} readOnly />
                  </label>
                  <label>
                    Country
                    <input value={owner.countryName || '—'} readOnly />
                  </label>
                  <label>
                    Time zone
                    <input value={owner.timezoneName || '—'} readOnly />
                  </label>
                  <label>
                    Address1
                    <input value={owner.address1 || '—'} readOnly />
                  </label>
                  <label>
                    Address2
                    <input value={owner.address2 || '—'} readOnly />
                  </label>
                  <label>
                    City
                    <input value={owner.city || '—'} readOnly />
                  </label>
                  <label>
                    State
                    <input value={owner.state || '—'} readOnly />
                  </label>
                  <label>
                    Zip
                    <input value={owner.zip || '—'} readOnly />
                  </label>
                  <div className="detail-card-spacer" aria-hidden="true" />
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Support</div>
                    <div className="owner-detail-panel-body">
                      <OwnerUserList users={owner.supportUsers} />
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">TAMs</div>
                    <div className="owner-detail-panel-body">
                      <OwnerUserList users={owner.tamUsers} />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="button-row button-row-end admin-detail-actions">
              <SmartLink className="primary-button" href="/owner/edit">
                Edit
              </SmartLink>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}

function OwnerEditPage({ sessionState }) {
  const navigate = useNavigate();
  const ownerState = useJson('/api/owner');
  const owner = ownerState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });

  useEffect(() => {
    if (owner) {
      setFormState({
        name: owner.name || '',
        address1: owner.address1 || '',
        address2: owner.address2 || '',
        city: owner.city || '',
        state: owner.state || '',
        zip: owner.zip || '',
        phoneNumber: owner.phoneNumber || '',
        countryId: owner.countryId ? String(owner.countryId) : '',
        timezoneId: owner.timezoneId ? String(owner.timezoneId) : '',
        supportIds: owner.supportUsers.map(user => user.id),
        tamIds: owner.tamUsers.map(user => user.id)
      });
    }
  }, [owner]);

  const selectedCountryId = formState?.countryId || '';
  const availableTimezones = owner?.timezones?.filter(timezone => selectedCountryId && String(timezone.countryId) === selectedCountryId) || [];

  const updateField = (field, value) => {
    setFormState(current => ({ ...current, [field]: value }));
  };

  const toggleSelectedUser = (field, userId) => {
    setFormState(current => ({
      ...current,
      [field]: current[field].includes(userId)
        ? current[field].filter(existing => existing !== userId)
        : [...current[field], userId]
    }));
  };

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }

    setSaveState({ saving: true, error: '' });
    try {
      const response = await fetch('/api/owner', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...formState,
          countryId: formState.countryId ? Number(formState.countryId) : null,
          timezoneId: formState.timezoneId ? Number(formState.timezoneId) : null
        })
      });

      if (response.status === 401) {
        throw new Error('You need to sign in again.');
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'Unable to save owner details.');
      }

      navigate('/owner');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save owner details.' });
      return;
    }

    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <DataState state={ownerState} emptyMessage="Owner company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && owner && (
          <form className="owner-form owner-detail-form" onSubmit={submit}>
            <div className="form-card ticket-detail-card">
              <div className="owner-form-grid ticket-detail-grid">
                <label>
                  Name
                  <input value={formState.name} onChange={event => updateField('name', event.target.value)} required />
                </label>
                <label>
                  Phone
                  <input value={formState.phoneNumber} onChange={event => updateField('phoneNumber', event.target.value)} />
                </label>
                <label>
                  Country
                  <select
                    value={formState.countryId}
                    onChange={event => {
                      const nextCountryId = event.target.value;
                      const timezoneStillValid = owner.timezones.some(
                        timezone => String(timezone.id) === formState.timezoneId && String(timezone.countryId) === nextCountryId
                      );
                      setFormState(current => ({
                        ...current,
                        countryId: nextCountryId,
                        timezoneId: timezoneStillValid ? current.timezoneId : ''
                      }));
                    }}
                  >
                    <option value="">Select a country</option>
                    {owner.countries.map(country => (
                      <option key={country.id} value={country.id}>
                        {country.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Time zone
                  <select value={formState.timezoneId} onChange={event => updateField('timezoneId', event.target.value)}>
                    <option value="">Select a time zone</option>
                    {availableTimezones.map(timezone => (
                      <option key={timezone.id} value={timezone.id}>
                        {timezone.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Address 1
                  <input value={formState.address1} onChange={event => updateField('address1', event.target.value)} />
                </label>
                <label>
                  Address 2
                  <input value={formState.address2} onChange={event => updateField('address2', event.target.value)} />
                </label>
                <label>
                  City
                  <input value={formState.city} onChange={event => updateField('city', event.target.value)} />
                </label>
                <label>
                  State
                  <input value={formState.state} onChange={event => updateField('state', event.target.value)} />
                </label>
                <label className="form-span-2">
                  Zip
                  <input value={formState.zip} onChange={event => updateField('zip', event.target.value)} />
                </label>
                <OwnerSelector
                  title="Support"
                  users={owner.supportOptions}
                  selectedIds={formState.supportIds}
                  onToggle={userId => toggleSelectedUser('supportIds', userId)}
                />
                <OwnerSelector
                  title="TAMs"
                  users={owner.tamOptions}
                  selectedIds={formState.tamIds}
                  onToggle={userId => toggleSelectedUser('tamIds', userId)}
                />
              </div>
            </div>

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row button-row-end admin-detail-actions">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function CompaniesPage({ sessionState }) {
  const companiesState = useJson('/api/companies');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Companies</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href={companiesState.data?.createPath || '/companies/new'}>
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={companiesState} emptyMessage="No companies are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {companiesState.data?.items.map(company => (
            <article key={company.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <h3>
                    <Link className="inline-link" to={`/companies/${company.id}`}>
                      {company.name}
                    </Link>
                  </h3>
                  <p className="muted-text">
                    {[company.countryName, company.timezoneName].filter(Boolean).join(' • ') || 'No locale configured'}
                  </p>
                  <p className="muted-text">
                    {company.superuserCount} superuser{company.superuserCount === 1 ? '' : 's'} • {company.userCount} users • {company.tamCount} TAMs
                  </p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function CompanyDetailPage({ sessionState }) {
  const { id } = useParams();
  const companyState = useJson(id ? `/api/companies/${id}` : null);
  const company = companyState.data;
  const sortedEntitlements = sortEntitlementAssignments(company?.entitlementAssignments || []);
  const sortedSuperusers = sortUsersByName(company?.selectedSuperusers || []);
  const sortedUsers = sortUsersByName(company?.selectedUsers || []);
  const sortedTams = sortUsersByName(company?.selectedTams || []);

  return (
    <section className="panel">
      <DataState state={companyState} emptyMessage="Company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {company && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={company.name || '—'} readOnly />
                  </label>
                  <label>
                    Phone
                    <input value={company.phoneNumber || '—'} readOnly />
                  </label>
                  <label>
                    Country
                    <input value={company.countryName || '—'} readOnly />
                  </label>
                  <label>
                    Time zone
                    <input value={company.timezoneName || '—'} readOnly />
                  </label>
                  <label>
                    Address1
                    <input value={company.address1 || '—'} readOnly />
                  </label>
                  <label>
                    Address2
                    <input value={company.address2 || '—'} readOnly />
                  </label>
                  <label>
                    City
                    <input value={company.city || '—'} readOnly />
                  </label>
                  <label>
                    State
                    <input value={company.state || '—'} readOnly />
                  </label>
                  <label>
                    Zip
                    <input value={company.zip || '—'} readOnly />
                  </label>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Entitlements</div>
                    <div className="owner-detail-panel-body">
                      {sortedEntitlements.length === 0 ? (
                        <p className="muted-text">—</p>
                      ) : (
                        <ul className="plain-list">
                          {sortedEntitlements.map((entry, index) => (
                            <li key={`${entry.entitlementId}-${entry.levelId}-${index}`}>
                              {entry.entitlementName} • {entry.levelName}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Superuser</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedSuperusers} />
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">User</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedUsers} />
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">TAMs</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedTams} />
                    </div>
                  </div>
                  <div className="detail-card-spacer" aria-hidden="true" />
                </div>
              </div>
            </div>

            {company.id && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={`/companies/${company.id}/edit`}>
                  Edit
                </SmartLink>
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function CompanyFormPage({ sessionState, mode }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const companyState = useJson(mode === 'edit' && id ? `/api/companies/${id}` : '/api/companies/bootstrap');
  const company = companyState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!company) {
      return;
    }
    if (isEdit) {
      setFormState({
        name: company.name || '',
        address1: company.address1 || '',
        address2: company.address2 || '',
        city: company.city || '',
        state: company.state || '',
        zip: company.zip || '',
        phoneNumber: company.phoneNumber || '',
        countryId: company.countryId ? String(company.countryId) : company.defaultCountryId ? String(company.defaultCountryId) : '',
        timezoneId: company.timezoneId ? String(company.timezoneId) : company.defaultTimezoneId ? String(company.defaultTimezoneId) : '',
        selectedUserIds: company.selectedUserIds || [],
        selectedTamIds: company.selectedTamIds || [],
        entitlements:
          company.entitlementAssignments?.map(entry => ({
            entitlementId: entry.entitlementId ? String(entry.entitlementId) : '',
            levelId: entry.levelId ? String(entry.levelId) : '',
            date: entry.date || company.todayDate || '',
            duration: entry.duration ? String(entry.duration) : String(2)
          })) || [],
        primaryContactId: company.primaryContactId ? String(company.primaryContactId) : ''
      });
      return;
    }
    setFormState({
      name: '',
      address1: '',
      address2: '',
      city: '',
      state: '',
      zip: '',
      phoneNumber: '',
      countryId: company.defaultCountryId ? String(company.defaultCountryId) : '',
      timezoneId: company.defaultTimezoneId ? String(company.defaultTimezoneId) : '',
      selectedUserIds: [],
      selectedTamIds: [],
      entitlements: [],
      primaryContactUsername: '',
      primaryContactFullName: '',
      primaryContactEmail: '',
      primaryContactSocial: '',
      primaryContactPhoneNumber: '',
      primaryPhoneNumberExtension: '',
      primaryContactCountry: company.defaultCountryId ? String(company.defaultCountryId) : '',
      primaryContactTimeZone: company.defaultTimezoneId ? String(company.defaultTimezoneId) : '',
      primaryContactPassword: ''
    });
  }, [company, isEdit]);

  const availableTimezones =
    company?.timezones?.filter(timezone => !formState?.countryId || String(timezone.countryId) === formState.countryId) || [];
  const availablePrimaryContactTimezones =
    company?.timezones?.filter(
      timezone => !formState?.primaryContactCountry || String(timezone.countryId) === formState.primaryContactCountry
    ) || [];

  const toggleSelection = (field, idToToggle) => {
    setFormState(current => ({
      ...current,
      [field]: current[field].includes(idToToggle)
        ? current[field].filter(existing => existing !== idToToggle)
        : [...current[field], idToToggle]
    }));
  };

  const updateEntitlement = (index, field, value) => {
    setFormState(current => ({
      ...current,
      entitlements: current.entitlements.map((entry, entryIndex) =>
        entryIndex === index ? { ...entry, [field]: value } : entry
      )
    }));
  };

  const addEntitlement = () => {
    setFormState(current => ({
      ...current,
      entitlements: [
        ...current.entitlements,
        {
          entitlementId: '',
          levelId: '',
          date: company?.todayDate || '',
          duration: String(company?.durations?.[1]?.value || 2)
        }
      ]
    }));
  };

  const removeEntitlement = index => {
    setFormState(current => ({
      ...current,
      entitlements: current.entitlements.filter((_, entryIndex) => entryIndex !== index)
    }));
  };

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      const entries = [
        ['name', formState.name],
        ['address1', formState.address1],
        ['address2', formState.address2],
        ['city', formState.city],
        ['state', formState.state],
        ['zip', formState.zip],
        ['phoneNumber', formState.phoneNumber],
        ['countryId', formState.countryId],
        ['timezoneId', formState.timezoneId],
        ...formState.selectedUserIds.map(userId => ['userIds', String(userId)]),
        ...formState.selectedTamIds.map(userId => ['tamIds', String(userId)]),
        ...formState.entitlements.flatMap(entry => [
          ['entitlementIds', entry.entitlementId],
          ['levelIds', entry.levelId],
          ['entitlementDates', entry.date],
          ['entitlementDurations', entry.duration]
        ])
      ];

      if (isEdit) {
        entries.push(['primaryContact', formState.primaryContactId]);
      } else {
        entries.push(
          ['primaryContactUsername', formState.primaryContactUsername],
          ['primaryContactFullName', formState.primaryContactFullName],
          ['primaryContactEmail', formState.primaryContactEmail],
          ['primaryContactSocial', formState.primaryContactSocial],
          ['primaryContactPhoneNumber', formState.primaryContactPhoneNumber],
          ['primaryPhoneNumberExtension', formState.primaryPhoneNumberExtension],
          ['primaryContactCountry', formState.primaryContactCountry],
          ['primaryContactTimeZone', formState.primaryContactTimeZone],
          ['primaryContactPassword', formState.primaryContactPassword]
        );
      }

      const response = await postForm(isEdit ? `/companies/${id}` : '/companies', entries);
      navigate(await resolvePostRedirectPath(response, '/companies'));
    } catch (error) {
      if (isNetworkRequestError(error)) {
        setSaveState({ saving: false, error: '' });
        submitBrowserForm(isEdit ? `/companies/${id}` : '/companies', entries);
        return;
      }
      setSaveState({ saving: false, error: error.message || 'Unable to save company.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteCompany = async () => {
    if (!id || !window.confirm('Delete this company?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/companies/${id}/delete`, []);
      navigate('/companies');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete company.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      {!isEdit && (
        <div className="section-header">
          <div>
            <SmartLink className="inline-link back-link" href={isEdit && id ? `/companies/${id}` : '/companies'}>
              Back to companies
            </SmartLink>
            <h2>{isEdit ? 'Edit company' : 'New company'}</h2>
          </div>
        </div>
      )}

      <DataState state={companyState} emptyMessage="Company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && company && (
          <form className="owner-form" onSubmit={submit}>
            {isEdit ? (
              <div className="form-card ticket-detail-card">
                <div className="owner-form owner-detail-form">
                  <div className="owner-form-grid ticket-detail-grid">
                    <label>
                      Name
                      <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
                    </label>
                    <label>
                      Phone
                      <input value={formState.phoneNumber} onChange={event => setFormState(current => ({ ...current, phoneNumber: event.target.value }))} />
                    </label>
                    <label>
                      Country
                      <select
                        value={formState.countryId}
                        onChange={event => {
                          const nextCountryId = event.target.value;
                          const timezoneStillValid = (company.timezones || []).some(
                            timezone => String(timezone.id) === formState.timezoneId && String(timezone.countryId) === nextCountryId
                          );
                          setFormState(current => ({
                            ...current,
                            countryId: nextCountryId,
                            timezoneId: timezoneStillValid ? current.timezoneId : ''
                          }));
                        }}
                      >
                        <option value="">Select a country</option>
                        {(company.countries || []).map(country => (
                          <option key={country.id} value={country.id}>
                            {country.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Time zone
                      <select value={formState.timezoneId} onChange={event => setFormState(current => ({ ...current, timezoneId: event.target.value }))}>
                        <option value="">Select a time zone</option>
                        {availableTimezones.map(timezone => (
                          <option key={timezone.id} value={timezone.id}>
                            {timezone.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Address1
                      <input value={formState.address1} onChange={event => setFormState(current => ({ ...current, address1: event.target.value }))} />
                    </label>
                    <label>
                      Address2
                      <input value={formState.address2} onChange={event => setFormState(current => ({ ...current, address2: event.target.value }))} />
                    </label>
                    <label>
                      City
                      <input value={formState.city} onChange={event => setFormState(current => ({ ...current, city: event.target.value }))} />
                    </label>
                    <label>
                      State
                      <input value={formState.state} onChange={event => setFormState(current => ({ ...current, state: event.target.value }))} />
                    </label>
                    <label>
                      Zip
                      <input value={formState.zip} onChange={event => setFormState(current => ({ ...current, zip: event.target.value }))} />
                    </label>
                    <div className="owner-detail-panel">
                      <div className="owner-detail-panel-label">Superuser</div>
                      <div className="owner-detail-panel-body">
                        <SelectableUserSummary users={company.selectedSuperusers} />
                      </div>
                    </div>
                    <SelectableUserPicker
                      title="User"
                      users={company.userOptions || []}
                      selectedIds={formState.selectedUserIds}
                      onToggle={userId => toggleSelection('selectedUserIds', userId)}
                    />
                    <SelectableUserPicker
                      title="TAMs"
                      users={company.tamOptions || []}
                      selectedIds={formState.selectedTamIds}
                      onToggle={userId => toggleSelection('selectedTamIds', userId)}
                    />
                    <div className="detail-card-spacer" aria-hidden="true" />
                  </div>
                  <section className="detail-card">
                    <div className="section-header compact-header">
                      <div>
                        <h3>Entitlements</h3>
                      </div>
                    </div>
                    <div className="version-editor-list">
                      {formState.entitlements.map((entry, index) => (
                        <div key={`${entry.entitlementId || 'new'}-${entry.levelId || 'level'}-${index}`} className="version-editor-card">
                          <div className="owner-form-grid">
                            <label>
                              Entitlement
                              <select value={entry.entitlementId} onChange={event => updateEntitlement(index, 'entitlementId', event.target.value)} required>
                                <option value="">Select entitlement</option>
                                {(company.entitlements || []).map(option => (
                                  <option key={option.id} value={option.id}>
                                    {option.name}
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label>
                              Level
                              <select value={entry.levelId} onChange={event => updateEntitlement(index, 'levelId', event.target.value)} required>
                                <option value="">Select level</option>
                                {(company.levels || []).map(option => (
                                  <option key={option.id} value={option.id}>
                                    {option.name} ({option.level})
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label>
                              Date
                              <input type="date" value={entry.date} onChange={event => updateEntitlement(index, 'date', event.target.value)} required />
                            </label>
                            <label>
                              Duration
                              <select value={entry.duration} onChange={event => updateEntitlement(index, 'duration', event.target.value)} required>
                                {(company.durations || []).map(option => (
                                  <option key={option.value} value={option.value}>
                                    {option.label}
                                  </option>
                              ))}
                            </select>
                          </label>
                        </div>
                        <div className="button-row button-row-end">
                          <button type="button" className="secondary-button danger-button" onClick={() => removeEntitlement(index)}>
                            Remove
                          </button>
                        </div>
                        </div>
                      ))}
                      {formState.entitlements.length === 0 && <p className="muted-text">No entitlements selected yet.</p>}
                    </div>
                    <div className="button-row button-row-end">
                      <button type="button" className="secondary-button danger-button" onClick={addEntitlement}>
                        Add
                      </button>
                    </div>
                  </section>
                </div>
              </div>
            ) : (
              <>
                <div className="owner-form-grid">
                  <label>
                    Name
                    <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
                  </label>
                  <label>
                    Phone number
                    <input value={formState.phoneNumber} onChange={event => setFormState(current => ({ ...current, phoneNumber: event.target.value }))} />
                  </label>
                  <label>
                    Address1
                    <input value={formState.address1} onChange={event => setFormState(current => ({ ...current, address1: event.target.value }))} />
                  </label>
                  <label>
                    Address2
                    <input value={formState.address2} onChange={event => setFormState(current => ({ ...current, address2: event.target.value }))} />
                  </label>
                  <label>
                    City
                    <input value={formState.city} onChange={event => setFormState(current => ({ ...current, city: event.target.value }))} />
                  </label>
                  <label>
                    State
                    <input value={formState.state} onChange={event => setFormState(current => ({ ...current, state: event.target.value }))} />
                  </label>
                  <label>
                    Zip
                    <input value={formState.zip} onChange={event => setFormState(current => ({ ...current, zip: event.target.value }))} />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={event => {
                        const nextCountryId = event.target.value;
                        const timezoneStillValid = (company.timezones || []).some(
                          timezone => String(timezone.id) === formState.timezoneId && String(timezone.countryId) === nextCountryId
                        );
                        setFormState(current => ({
                          ...current,
                          countryId: nextCountryId,
                          timezoneId: timezoneStillValid ? current.timezoneId : ''
                        }));
                      }}
                    >
                      <option value="">Select a country</option>
                      {(company.countries || []).map(country => (
                        <option key={country.id} value={country.id}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select value={formState.timezoneId} onChange={event => setFormState(current => ({ ...current, timezoneId: event.target.value }))}>
                      <option value="">Select a time zone</option>
                      {availableTimezones.map(timezone => (
                        <option key={timezone.id} value={timezone.id}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Username
                    <input value={formState.primaryContactUsername} onChange={event => setFormState(current => ({ ...current, primaryContactUsername: event.target.value }))} required />
                  </label>
                  <label>
                    Full name
                    <input value={formState.primaryContactFullName} onChange={event => setFormState(current => ({ ...current, primaryContactFullName: event.target.value }))} />
                  </label>
                  <label>
                    Email
                    <input type="email" value={formState.primaryContactEmail} onChange={event => setFormState(current => ({ ...current, primaryContactEmail: event.target.value }))} required />
                  </label>
                  <label>
                    Social
                    <input value={formState.primaryContactSocial} onChange={event => setFormState(current => ({ ...current, primaryContactSocial: event.target.value }))} />
                  </label>
                  <label>
                    Phone number
                    <input value={formState.primaryContactPhoneNumber} onChange={event => setFormState(current => ({ ...current, primaryContactPhoneNumber: event.target.value }))} />
                  </label>
                  <label>
                    Phone extension
                    <input value={formState.primaryPhoneNumberExtension} onChange={event => setFormState(current => ({ ...current, primaryPhoneNumberExtension: event.target.value }))} />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.primaryContactCountry}
                      onChange={event => {
                        const nextCountryId = event.target.value;
                        const timezoneStillValid = (company.timezones || []).some(
                          timezone =>
                            String(timezone.id) === formState.primaryContactTimeZone &&
                            String(timezone.countryId) === nextCountryId
                        );
                        setFormState(current => ({
                          ...current,
                          primaryContactCountry: nextCountryId,
                          primaryContactTimeZone: timezoneStillValid ? current.primaryContactTimeZone : ''
                        }));
                      }}
                    >
                      <option value="">Select a country</option>
                      {(company.countries || []).map(country => (
                        <option key={country.id} value={country.id}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select
                      value={formState.primaryContactTimeZone}
                      onChange={event => setFormState(current => ({ ...current, primaryContactTimeZone: event.target.value }))}
                    >
                      <option value="">Select a time zone</option>
                      {availablePrimaryContactTimezones.map(timezone => (
                        <option key={timezone.id} value={timezone.id}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Password
                    <input
                      type="password"
                      value={formState.primaryContactPassword}
                      onChange={event => setFormState(current => ({ ...current, primaryContactPassword: event.target.value }))}
                      required
                    />
                  </label>
                </div>
                <div className="owner-picker-grid">
                  <SelectableUserPicker
                    title="Users"
                    users={company.userOptions || []}
                    selectedIds={formState.selectedUserIds}
                    onToggle={userId => toggleSelection('selectedUserIds', userId)}
                  />
                  <SelectableUserPicker
                    title="TAMs"
                    users={company.tamOptions || []}
                    selectedIds={formState.selectedTamIds}
                    onToggle={userId => toggleSelection('selectedTamIds', userId)}
                  />
                </div>
                <section className="detail-card">
                  <div className="section-header compact-header">
                    <div>
                      <h3>Entitlements</h3>
                    </div>
                  </div>
                  <div className="version-editor-list">
                    {formState.entitlements.map((entry, index) => (
                      <div key={`${entry.entitlementId || 'new'}-${entry.levelId || 'level'}-${index}`} className="version-editor-card">
                        <div className="owner-form-grid">
                          <label>
                            Entitlement
                            <select value={entry.entitlementId} onChange={event => updateEntitlement(index, 'entitlementId', event.target.value)} required>
                              <option value="">Select entitlement</option>
                              {(company.entitlements || []).map(option => (
                                <option key={option.id} value={option.id}>
                                  {option.name}
                                </option>
                              ))}
                            </select>
                          </label>
                          <label>
                            Level
                            <select value={entry.levelId} onChange={event => updateEntitlement(index, 'levelId', event.target.value)} required>
                              <option value="">Select level</option>
                              {(company.levels || []).map(option => (
                                <option key={option.id} value={option.id}>
                                  {option.name} ({option.level})
                                </option>
                              ))}
                            </select>
                          </label>
                          <label>
                            Date
                            <input type="date" value={entry.date} onChange={event => updateEntitlement(index, 'date', event.target.value)} required />
                          </label>
                          <label>
                            Duration
                            <select value={entry.duration} onChange={event => updateEntitlement(index, 'duration', event.target.value)} required>
                              {(company.durations || []).map(option => (
                                <option key={option.value} value={option.value}>
                                  {option.label}
                                </option>
                              ))}
                            </select>
                          </label>
                        </div>
                        <div className="button-row button-row-end">
                          <button type="button" className="secondary-button danger-button" onClick={() => removeEntitlement(index)}>
                            Remove
                          </button>
                        </div>
                      </div>
                    ))}
                    {formState.entitlements.length === 0 && <p className="muted-text">No entitlements selected yet.</p>}
                  </div>
                  <div className="button-row button-row-end">
                    <button type="button" className="secondary-button" onClick={addEntitlement}>
                      Add entitlement
                    </button>
                  </div>
                </section>
              </>
            )}

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${isEdit ? ' button-row-split' : ''}`}>
              {isEdit ? (
                <button type="button" className="secondary-button danger-button" onClick={deleteCompany} disabled={saveState.saving}>
                  Delete
                </button>
              ) : (
                <SmartLink className="secondary-button" href={isEdit && id ? `/companies/${id}` : '/companies'}>
                  Cancel
                </SmartLink>
              )}
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isEdit ? 'Save' : 'Create company'}
              </button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function SupportTicketsPage({ sessionState, view, apiBase = '/api/support/tickets', basePath = '/support/tickets', createFallbackPath = '/support/tickets/new', description = '' }) {
  const query = view && view !== 'assigned' ? toQueryString({ view }) : '';
  const ticketsState = useJson(`${apiBase}${query}`);
  const currentView = ticketsState.data?.view || view || 'assigned';
  const showLevelColumn = apiBase !== '/api/user/tickets';
  const showCreateButton = !(apiBase === '/api/user/tickets' && currentView === 'closed');

  return (
    <section className="panel">
      {showCreateButton && (
        <div className="button-row support-ticket-actions">
          <SmartLink className="primary-button" href={ticketsState.data?.createPath || createFallbackPath}>
            Create
          </SmartLink>
        </div>
      )}

      <DataState state={ticketsState} emptyMessage="No tickets are available in this queue." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="ticket-table-wrap">
          <table className="support-ticket-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Date</th>
                <th>Status</th>
                <th>Category</th>
                <th>Support</th>
                <th>Company</th>
                <th>Entitlement</th>
                {showLevelColumn && <th>Level</th>}
                <th>Affects</th>
                {currentView === 'closed' && <th>Resolved</th>}
              </tr>
            </thead>
            <tbody>
              {(ticketsState.data?.items || []).map(ticket => {
                const useLightText = ticket.slaColor && !isWhiteColorValue(ticket.slaColor);
                return (
                <tr
                  key={ticket.id}
                  className={useLightText ? 'ticket-row-highlight' : undefined}
                  style={ticket.slaColor ? { backgroundColor: ticket.slaColor, color: useLightText ? '#ffffff' : undefined } : undefined}
                >
                  <td>
                    <SmartLink className="inline-link" href={ticket.detailPath}>
                      {ticket.name}
                    </SmartLink>
                    {ticket.messageDirectionArrow && <span className="ticket-direction">{ticket.messageDirectionArrow}</span>}
                  </td>
                  <td>{ticket.messageDateLabel || '-'}</td>
                  <td>{ticket.status || '-'}</td>
                  <td>{ticket.categoryName || '-'}</td>
                  <td>
                    {ticket.supportUser ? (
                      <a className="inline-link" href={ticket.supportUser.detailPath}>
                        {ticket.supportUser.displayName || ticket.supportUser.username}
                      </a>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>
                    {ticket.companyPath ? (
                      <a className="inline-link" href={ticket.companyPath}>
                        {ticket.companyName}
                      </a>
                    ) : (
                      ticket.companyName || '—'
                   )}
                  </td>
                  <td>{ticket.entitlementName || '-'}</td>
                  {showLevelColumn && <td>{ticket.levelName || '-'}</td>}
                  <td>{ticket.affectsVersionName || '-'}</td>
                  {currentView === 'closed' && <td>{ticket.resolvedVersionName || '-'}</td>}
                </tr>
              )})}
            </tbody>
          </table>
        </div>
      </DataState>
    </section>
  );
}

function SupportTicketCreatePage({ sessionState, apiBase = '/api/support/tickets/bootstrap', backPath = '/support/tickets', submitFallbackPath = '/support/tickets', title = 'New support ticket', description = '', navigateTo = '/support/tickets', compactCreateActions = false, hideEntitlementLevel = false }) {
  const navigate = useNavigate();
  const [selectedCompanyId, setSelectedCompanyId] = useState('');
  const [selectedCompanyEntitlementId, setSelectedCompanyEntitlementId] = useState('');
  const bootstrapState = useJson(
    `${apiBase}${toQueryString({
      companyId: selectedCompanyId || undefined,
      companyEntitlementId: selectedCompanyEntitlementId || undefined
    })}`
  );
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] = useState(null);
  const [files, setFiles] = useState([]);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const messageInputRef = useRef(null);
  const showFixedCompany = sessionState.data?.role === 'superuser';
  const compactCreateHeader = apiBase === '/api/superuser/tickets/bootstrap';

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState(current => {
      const initialCompanyId = bootstrap.selectedCompanyId ? String(bootstrap.selectedCompanyId) : '';
      const initialEntitlementId = bootstrap.selectedCompanyEntitlementId ? String(bootstrap.selectedCompanyEntitlementId) : '';
      const initialAffectsVersionId = bootstrap.defaultAffectsVersion?.id ? String(bootstrap.defaultAffectsVersion.id) : '';
      if (!current) {
        setSelectedCompanyId(initialCompanyId);
        setSelectedCompanyEntitlementId(initialEntitlementId);
        return {
          ticketName: bootstrap.ticketName || '',
          companyId: initialCompanyId,
          companyEntitlementId: initialEntitlementId,
          categoryId: bootstrap.defaultCategoryId ? String(bootstrap.defaultCategoryId) : '',
          affectsVersionId: initialAffectsVersionId,
          message: ''
        };
      }
      const validEntitlementIds = (bootstrap.companyEntitlements || []).map(entry => String(entry.id));
      const validVersionIds = (bootstrap.versions || []).map(version => String(version.id));
      const nextEntitlementId = validEntitlementIds.includes(current.companyEntitlementId)
        ? current.companyEntitlementId
        : initialEntitlementId;
      setSelectedCompanyEntitlementId(nextEntitlementId);
      return {
        ...current,
        ticketName: bootstrap.ticketName || current.ticketName,
        companyId: initialCompanyId || current.companyId,
        companyEntitlementId: nextEntitlementId,
        categoryId: current.categoryId || (bootstrap.defaultCategoryId ? String(bootstrap.defaultCategoryId) : ''),
        affectsVersionId: validVersionIds.includes(current.affectsVersionId)
          ? current.affectsVersionId
          : initialAffectsVersionId
      };
    });
  }, [bootstrap]);

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      const response = await postMultipart(bootstrap?.submitPath || submitFallbackPath, [
        ['status', 'Open'],
        ['message', formState.message],
        ['companyId', formState.companyId],
        ['companyEntitlementId', formState.companyEntitlementId],
        ['categoryId', formState.categoryId || null],
        ['affectsVersionId', formState.affectsVersionId || null],
        ...files.map(file => ['attachments', file])
      ], {
        headers: { 'X-Billetsys-Client': 'react' }
      });
      navigate(await resolvePostRedirectPath(response, navigateTo));
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to create ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      {(!compactCreateHeader && title) || description ? (
        <div className="section-header">
          <div>
            {!compactCreateHeader ? (
              <SmartLink className="inline-link back-link" href={backPath}>
                Back to tickets
              </SmartLink>
            ) : null}
            {title ? <h2>{title}</h2> : null}
            {description ? <p className="section-copy">{description}</p> : null}
          </div>
        </div>
      ) : null}

      <DataState state={bootstrapState} emptyMessage="A company is required before creating a support ticket." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && bootstrap && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label>
                Ticket
                <input value={formState.ticketName} readOnly />
              </label>
              <label>
                Company
                {showFixedCompany ? (
                  <input
                    value={(bootstrap.companies || []).find(company => String(company.id) === formState.companyId)?.name || ''}
                    readOnly
                  />
                ) : (
                  <select
                    value={formState.companyId}
                    onChange={event => {
                      const nextCompanyId = event.target.value;
                      setSelectedCompanyId(nextCompanyId);
                      setSelectedCompanyEntitlementId('');
                      setFormState(current => ({
                        ...current,
                        companyId: nextCompanyId,
                        companyEntitlementId: '',
                        affectsVersionId: ''
                      }));
                    }}
                    required
                  >
                    {(bootstrap.companies || []).map(company => (
                      <option key={company.id} value={company.id}>
                        {company.name}
                      </option>
                    ))}
                  </select>
                )}
              </label>
              <label>
                Entitlement
                <select
                  value={formState.companyEntitlementId}
                  onChange={event => {
                    const nextEntitlementId = event.target.value;
                    setSelectedCompanyEntitlementId(nextEntitlementId);
                    setFormState(current => ({ ...current, companyEntitlementId: nextEntitlementId, affectsVersionId: '' }));
                  }}
                  required
                >
                  {(bootstrap.companyEntitlements || []).map(entry => (
                    <option key={entry.id} value={entry.id}>
                      {entry.name}{!hideEntitlementLevel && entry.levelName ? ` • ${entry.levelName}` : ''}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Category
                <select value={formState.categoryId} onChange={event => setFormState(current => ({ ...current, categoryId: event.target.value }))}>
                  {(bootstrap.categories || []).map(category => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-span-2">
                Message
                <MarkdownEditor
                  value={formState.message}
                  onChange={value => setFormState(current => ({ ...current, message: value }))}
                  inputRef={messageInputRef}
                  rows={10}
                  required
                />
              </label>
            </div>

            <section className="detail-grid">
              <div className="detail-card">
                <h3>Affects</h3>
                <select
                  value={formState.affectsVersionId}
                  onChange={event => setFormState(current => ({ ...current, affectsVersionId: event.target.value }))}
                >
                  {(bootstrap.versions || []).length === 0 && <option value="">-</option>}
                  {(bootstrap.versions || []).map(version => (
                    <option key={version.id} value={version.id}>
                      {version.name}{version.date ? ` (${version.date})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <div className="detail-card-spacer" aria-hidden="true" />
            </section>

            <AttachmentPicker files={files} onFilesChange={setFiles} />

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${compactCreateHeader || compactCreateActions ? ' button-row-end' : ''}`}>
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Creating...' : compactCreateHeader || compactCreateActions ? 'Create' : 'Create ticket'}
              </button>
              {!compactCreateHeader && !compactCreateActions ? (
                <SmartLink className="secondary-button" href={backPath}>
                  Cancel
                </SmartLink>
              ) : null}
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function SupportTicketDetailPage({ sessionState, apiBase = '/api/support/tickets', backPath = '/support/tickets', titleFallback = 'Support ticket', secondaryUsersLabel = 'TAM' }) {
  const { id } = useParams();
  const location = useLocation();
  const [refreshNonce, setRefreshNonce] = useState(0);
  const ticketState = useJson(id ? `${apiBase}/${id}${toQueryString({ refresh: refreshNonce })}` : null);
  const ticket = ticketState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const [replyState, setReplyState] = useState({ saving: false, error: '' });
  const [replyBody, setReplyBody] = useState('');
  const [files, setFiles] = useState([]);
  const replyInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const messagesHeadingRef = useRef(null);
  const [scrollToMessages, setScrollToMessages] = useState(false);
  const isClosed = ticket?.displayStatus === 'Closed';
  const canEditStatus = ticket?.editableStatus ?? true;
  const canEditCategory = ticket?.editableCategory ?? true;
  const canEditExternalIssue = ticket?.editableExternalIssue ?? true;
  const canEditAffectsVersion = ticket?.editableAffectsVersion ?? true;
  const canEditResolvedVersion = ticket?.editableResolvedVersion ?? true;
  const showLevelField = apiBase !== '/api/user/tickets' || sessionState.data?.role === 'tam';

  useEffect(() => {
    if (!ticket) {
      return;
    }
    setFormState({
      status: ticket.displayStatus || 'Open',
      categoryId: ticket.categoryId ? String(ticket.categoryId) : '',
      externalIssueLink: ticket.externalIssueLink || '',
      affectsVersionId: ticket.affectsVersionId ? String(ticket.affectsVersionId) : '',
      resolvedVersionId: ticket.resolvedVersionId ? String(ticket.resolvedVersionId) : ''
    });
  }, [ticket]);

  useEffect(() => {
    if (!ticket) {
      return;
    }
    const shouldScrollFromQuery = new URLSearchParams(location.search).has('replyAdded');
    if (!scrollToMessages && !shouldScrollFromQuery) {
      return;
    }
    messagesHeadingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setScrollToMessages(false);
    if (shouldScrollFromQuery) {
      window.history.replaceState({}, '', ticket.actionPath || location.pathname);
    }
  }, [ticket, scrollToMessages, location.search, location.pathname]);

  const saveTicket = async event => {
    event.preventDefault();
    if (!ticket || !formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(ticket.actionPath, [
        ['status', formState.status],
        ['companyId', ticket.companyId],
        ['companyEntitlementId', ticket.companyEntitlementId],
        ['categoryId', formState.categoryId || null],
        ['externalIssueLink', formState.externalIssueLink],
        ['affectsVersionId', formState.affectsVersionId],
        ['resolvedVersionId', formState.resolvedVersionId || null]
      ]);
      setRefreshNonce(current => current + 1);
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const addReplyFiles = event => {
    const nextFiles = Array.from(event.target.files || []);
    if (nextFiles.length === 0) {
      return;
    }
    setFiles(current => [...current, ...nextFiles]);
    event.target.value = '';
  };

  const removeReplyFile = index => {
    setFiles(current => current.filter((_, fileIndex) => fileIndex !== index));
  };

  return (
    <section className="panel support-ticket-detail-page">
      <SmartLink className="inline-link back-link" href={backPath}>
        Back to tickets
      </SmartLink>

      <DataState state={ticketState} emptyMessage="Ticket not found." signInHref={sessionState.data?.homePath || '/login'}>
        {ticket && formState && (
          <>
            <div className="form-card ticket-detail-card">
              <h1>{ticket.name || titleFallback}</h1>
              <form className="owner-form ticket-detail-form" onSubmit={saveTicket}>
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Ticket
                    <input value={ticket.name || ''} readOnly />
                  </label>
                  <label>
                    Company
                    <div className="readonly-link-field">
                      <input value={ticket.companyName || ''} readOnly />
                      {ticket.companyId ? (
                        <a className="readonly-link-field-link" href={`/support/companies/${ticket.companyId}`}>
                          {ticket.companyName || '-'}
                        </a>
                      ) : null}
                    </div>
                  </label>
                  <label>
                    Category
                    {isClosed || !canEditCategory ? (
                      <input value={ticket.categoryName || '-'} readOnly />
                    ) : (
                      <select
                        value={formState.categoryId}
                        onChange={event => setFormState(current => ({ ...current, categoryId: event.target.value }))}
                      >
                        {(ticket.categories || []).map(category => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Entitlement
                    <input value={ticket.entitlementName || '-'} readOnly className={ticket.ticketEntitlementExpired ? 'expired-input' : ''} />
                  </label>
                  <label>
                    Status
                    {isClosed || !canEditStatus ? (
                      <input value={formState.status || '-'} readOnly />
                    ) : (
                      <select
                        value={formState.status}
                        onChange={event => setFormState(current => ({ ...current, status: event.target.value }))}
                      >
                        {(ticket.statusOptions || []).map(option => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  {showLevelField ? (
                    <label>
                      Level
                      <input value={ticket.levelName || '-'} readOnly className={ticket.ticketEntitlementExpired ? 'expired-input' : ''} />
                    </label>
                  ) : (
                    <label className="ticket-detail-spacer" aria-hidden="true">
                      <input value="-" readOnly />
                    </label>
                  )}
                  <label>
                    External issue
                    {isClosed || !canEditExternalIssue ? (
                      formState.externalIssueLink ? (
                        <a href={formState.externalIssueLink} target="_blank" rel="noreferrer">
                          {formState.externalIssueLink}
                        </a>
                      ) : (
                        <input value="-" readOnly />
                      )
                    ) : (
                      <div className="inline-link-field">
                        <input
                          value={formState.externalIssueLink}
                          onChange={event => setFormState(current => ({ ...current, externalIssueLink: event.target.value }))}
                        />
                        {formState.externalIssueLink ? (
                          <a className="inline-link-field-link" href={formState.externalIssueLink} target="_blank" rel="noreferrer">
                            Open
                          </a>
                        ) : null}
                      </div>
                    )}
                  </label>
                  <label className="ticket-detail-spacer" aria-hidden="true">
                    <input value="-" readOnly />
                  </label>
                  <label>
                    Affects
                    {isClosed || !canEditAffectsVersion ? (
                      <input value={versionLabel(ticket.versions, formState.affectsVersionId) || '-'} readOnly />
                    ) : (
                      <select
                        value={formState.affectsVersionId}
                        onChange={event => setFormState(current => ({ ...current, affectsVersionId: event.target.value }))}
                      >
                        {(ticket.versions || []).length === 0 && <option value="">-</option>}
                        {(ticket.versions || []).map(version => (
                          <option key={version.id} value={version.id}>
                            {version.name} ({version.date})
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Resolved
                    {isClosed || !canEditResolvedVersion ? (
                      <input value={versionLabel(ticket.versions, formState.resolvedVersionId) || '-'} readOnly />
                    ) : (
                      <select
                        value={formState.resolvedVersionId}
                        onChange={event => setFormState(current => ({ ...current, resolvedVersionId: event.target.value }))}
                      >
                        <option value="">-</option>
                        {(ticket.versions || []).map(version => (
                          <option key={version.id} value={version.id}>
                            {version.name} ({version.date})
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Support
                    <div className="ticket-user-field">
                      <UserReferenceInlineList users={ticket.supportUsers} />
                    </div>
                  </label>
                  <label>
                    {ticket.secondaryUsersLabel || secondaryUsersLabel}
                    <div className="ticket-user-field">
                      <UserReferenceInlineList users={ticket.secondaryUsers || ticket.tamUsers} />
                    </div>
                  </label>
                </div>

                {saveState.error && <p className="error-text">{saveState.error}</p>}

                {!isClosed && (canEditStatus || canEditCategory || canEditExternalIssue || canEditAffectsVersion || canEditResolvedVersion) && (
                  <div className="form-actions">
                    <button type="submit" className="action-button" disabled={saveState.saving}>
                      {saveState.saving ? 'Saving...' : 'Save ticket'}
                    </button>
                  </div>
                )}
              </form>
            </div>

            <h2 ref={messagesHeadingRef}>Messages</h2>
            {(!ticket.messages || ticket.messages.length === 0) ? (
              <p className="muted-text">No messages yet.</p>
            ) : (
              <table className="message-table">
                {(ticket.messages || []).map(message => (
                  <tbody key={message.id}>
                    <tr className="message-header">
                      <td>{message.dateLabel || '-'}</td>
                      <td className="message-email">
                        {message.author?.detailPath ? (
                          <UserHoverLink user={message.author} className="inline-link">
                            {message.author.displayName || message.author.username}
                          </UserHoverLink>
                        ) : (
                          message.author?.displayName || message.author?.username || '-'
                        )}
                      </td>
                    </tr>
                    <tr>
                      <td colSpan="2">
                        <div className="markdown-output">
                          <MarkdownContent>{message.body || ''}</MarkdownContent>
                        </div>
                      </td>
                    </tr>
                    {(message.attachments || []).length > 0 && (
                      <tr className="message-attachments">
                        <td colSpan="2">
                          {message.attachments.map(attachment => (
                            <div key={attachment.id} className="attachment-footer">
                              <span className="attachment-name">
                                <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                                  {attachment.name}
                                </a>
                              </span>
                              <span className="attachment-meta">
                                {attachment.mimeType} - {attachment.sizeLabel}
                              </span>
                            </div>
                          ))}
                        </td>
                      </tr>
                    )}
                  </tbody>
                ))}
              </table>
            )}

            {!isClosed ? (
              <>
                <h2>Reply</h2>
                <form className="ticket-reply-form" action={ticket.messageActionPath} method="post" encType="multipart/form-data">
                  <MarkdownEditor value={replyBody} onChange={setReplyBody} inputRef={replyInputRef} name="body" rows={6} required />

                  <div className="reply-attachment-container">
                    <span className="attachment-label">Attachments</span>
                    <div className="reply-attachment-list">
                      <table>
                        <thead>
                          <tr>
                            <th>Name</th>
                            <th>Mimetype</th>
                            <th>Size</th>
                            <th></th>
                          </tr>
                        </thead>
                        <tbody>
                          {files.map((file, index) => (
                            <tr key={`${file.name}-${file.size}-${index}`}>
                              <td>{file.name}</td>
                              <td>{file.type || 'application/octet-stream'}</td>
                              <td>{formatFileSize(file.size)}</td>
                              <td>
                                <button type="button" className="secondary-button attachment-remove-button" onClick={() => removeReplyFile(index)}>
                                  Remove
                                </button>
                              </td>
                            </tr>
                          ))}
                          {files.length === 0 && (
                            <tr>
                              <td colSpan="4" className="muted-text">
                                No attachments selected.
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      name="attachments"
                      multiple
                      className="attachment-input"
                      onChange={addReplyFiles}
                    />
                  </div>

                  {replyState.error && <p className="error-text">{replyState.error}</p>}

                  <div className="form-actions attachment-actions">
                    {ticket.exportPath && (
                      <a className="action-button export-btn" href={ticket.exportPath}>
                        Export
                      </a>
                    )}
                    <button type="button" className="action-button" onClick={() => fileInputRef.current?.click()}>
                      Browse
                    </button>
                    <button type="submit" className="action-button" disabled={replyState.saving}>
                      {replyState.saving ? 'Adding...' : 'Add'}
                    </button>
                  </div>
                </form>
              </>
            ) : (
              ticket.exportPath && (
                <div className="form-actions attachment-actions">
                  <a className="action-button export-btn" href={ticket.exportPath}>
                    Export
                  </a>
                </div>
              )
            )}
          </>
        )}
      </DataState>
    </section>
  );
}

function UserReferenceList({ users }) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }
  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          {user.detailPath ? (
            <UserHoverLink user={user} className="inline-link">
              {user.displayName || user.username}
            </UserHoverLink>
          ) : (
            user.displayName || user.username
          )}
          {user.email && <span className="muted-text"> — {user.email}</span>}
        </li>
      ))}
    </ul>
  );
}

function UserReferenceInlineList({ users }) {
  if (!users || users.length === 0) {
    return <input value="-" readOnly />;
  }

  return (
    <div>
      {users.map((user, index) => (
        <span key={user.id}>
          {user.detailPath ? (
            <UserHoverLink user={user} className="inline-link">
              {user.username || user.displayName}
            </UserHoverLink>
          ) : (
            user.username || user.displayName
          )}
          {index < users.length - 1 ? ', ' : ''}
        </span>
      ))}
    </div>
  );
}

function UserHoverLink({ user, className, children }) {
  const [tooltipState, setTooltipState] = useState(null);

  if (!user?.detailPath) {
    return children;
  }

  const updateTooltip = event => {
    const pad = 12;
    const width = 240;
    const height = 140;
    let left = event.clientX + pad;
    let top = event.clientY + pad;
    if (left + width > window.innerWidth - pad) {
      left = event.clientX - width - pad;
    }
    if (top + height > window.innerHeight - pad) {
      top = event.clientY - height - pad;
    }
    setTooltipState({
      left,
      top
    });
  };

  return (
    <>
      <a
        className={className}
        href={user.detailPath}
        onMouseEnter={updateTooltip}
        onMouseMove={updateTooltip}
        onMouseLeave={() => setTooltipState(null)}
        onBlur={() => setTooltipState(null)}
      >
        {children}
      </a>
      {tooltipState && (
        <div className="user-tooltip" style={{ left: tooltipState.left, top: tooltipState.top }}>
          <div className="user-tooltip-inner">
            <div className="user-tooltip-header">
              <div className="user-tooltip-avatar">
                {user.logoBase64 ? (
                  <img src={user.logoBase64} alt="avatar" />
                ) : (
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
                  </svg>
                )}
              </div>
              <div>
                <div className="user-tooltip-name">{user.username || ''}</div>
                <div className="user-tooltip-fullname">{user.fullName || ''}</div>
              </div>
            </div>
            <div className="user-tooltip-divider" />
            <div className="user-tooltip-meta">{user.email ? `📧 ${user.email}` : ''}</div>
            <div className="user-tooltip-meta">{user.countryName ? `🌎 ${user.countryName}` : ''}</div>
            <div className="user-tooltip-meta">{user.timezoneName ? `🕐 ${user.timezoneName}` : ''}</div>
          </div>
        </div>
      )}
    </>
  );
}

function versionLabel(versions, selectedId) {
  if (!selectedId) {
    return '';
  }
  const version = (versions || []).find(option => String(option.id) === String(selectedId));
  return version ? `${version.name} (${version.date})` : '';
}

function markdownActionText(action, selectedText, option = '') {
  const text = selectedText || 'text';
  if (action === 'bold') {
    return `**${text}**`;
  }
  if (action === 'italic') {
    return `*${text}*`;
  }
  if (action === 'heading') {
    return `## ${selectedText || 'Heading'}`;
  }
  if (action === 'list') {
    return selectedText ? selectedText.split('\n').map(line => `- ${line}`).join('\n') : '- Item';
  }
  if (action === 'quote') {
    return selectedText ? selectedText.split('\n').map(line => `> ${line}`).join('\n') : '> Quote';
  }
  if (action === 'code') {
    return `\`${text}\``;
  }
  if (action === 'code-block') {
    return `\`\`\`${option || ''}\n${selectedText || 'code'}\n\`\`\``;
  }
  if (action === 'link') {
    const href = window.prompt('Link URL', 'https://');
    if (!href) {
      return null;
    }
    return `[${selectedText || 'link'}](${href})`;
  }
  if (action === 'media') {
    const href = window.prompt('Media URL', 'https://');
    if (!href) {
      return null;
    }
    return `![${selectedText || 'media'}](${href})`;
  }
  return selectedText;
}

function formatFileSize(size) {
  if (!size) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value >= 10 || unitIndex === 0 ? Math.round(value) : value.toFixed(1)} ${units[unitIndex]}`;
}

function isWhiteColorValue(color) {
  if (!color) {
    return true;
  }
  const normalized = color.replace(/\s+/g, '').toLowerCase();
  return normalized === 'white'
    || normalized === '#fff'
    || normalized === '#ffffff'
    || normalized === 'rgb(255,255,255)'
    || normalized === 'rgba(255,255,255,1)'
    || normalized === 'rgba(255,255,255,1.0)';
}

function CategoriesPage({ sessionState }) {
  const categoriesState = useJson('/api/categories');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Categories</h2>
        </div>
        <div className="button-row">
          {categoriesState.data?.canCreate && (
            <SmartLink className="primary-button" href={categoriesState.data.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={categoriesState} emptyMessage="No categories are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {categoriesState.data?.items.map(category => (
            <article key={category.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>
                      <Link className="inline-link" to={`/categories/${category.id}`}>
                        {category.name}
                      </Link>
                    </h3>
                    {category.isDefault && <span className="status-pill">Default</span>}
                  </div>
                  <p className="tag-copy">{category.descriptionPreview || 'No description'}</p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function CategoryFormPage({ sessionState, mode }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const categoryState = useJson(mode === 'edit' && id ? `/api/categories/${id}` : '/api/categories/bootstrap');
  const category = categoryState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const [files, setFiles] = useState([]);
  const descriptionInputRef = useRef(null);
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!category) {
      return;
    }
    if (isEdit) {
      setFormState({
        name: category.name || '',
        description: category.description || '',
        isDefault: Boolean(category.isDefault)
      });
      return;
    }
    setFormState({ name: '', description: '', isDefault: false });
  }, [category, isEdit]);

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postMultipart(isEdit ? `/categories/${id}` : '/categories', [
        ['name', formState.name],
        ['description', formState.description],
        ['isDefault', String(formState.isDefault)],
        ...files.map(file => ['attachments', file])
      ]);
      navigate(isEdit && id ? `/categories/${id}` : '/categories');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save category.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteCategory = async () => {
    if (!id || !window.confirm('Delete this category?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/categories/${id}/delete`, []);
      navigate('/categories');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete category.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      {!isEdit && (
        <div className="section-header">
          <div>
            <SmartLink className="inline-link back-link" href={isEdit && id ? `/categories/${id}` : '/categories'}>
              Back to categories
            </SmartLink>
            <h2>{isEdit ? 'Edit category' : 'New category'}</h2>
          </div>
        </div>
      )}

      <DataState state={categoryState} emptyMessage="Category unavailable." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label>
                Name
                <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
              </label>
              <label>
                Default
                <select
                  value={String(formState.isDefault)}
                  onChange={event => setFormState(current => ({ ...current, isDefault: event.target.value === 'true' }))}
                >
                  <option value="false">No</option>
                  <option value="true">Yes</option>
                </select>
              </label>
              <label className="form-span-2">
                Description
                <MarkdownEditor
                  value={formState.description}
                  onChange={value => setFormState(current => ({ ...current, description: value }))}
                  inputRef={descriptionInputRef}
                  rows={10}
                />
              </label>
            </div>

            <AttachmentPicker files={files} onFilesChange={setFiles} existingAttachments={category.attachments || []} />

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${isEdit ? ' button-row-split' : ''}`}>
              {isEdit && (
                <button type="button" className="secondary-button danger-button" onClick={deleteCategory} disabled={saveState.saving}>
                  Delete
                </button>
              )}
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isEdit ? 'Save' : 'Create category'}
              </button>
              {!isEdit && (
                <SmartLink className="secondary-button" href={isEdit && id ? `/categories/${id}` : '/categories'}>
                  Cancel
                </SmartLink>
              )}
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function EntitlementsPage({ sessionState }) {
  const entitlementsState = useJson('/api/entitlements');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Entitlements</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href="/entitlements/new">
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={entitlementsState} emptyMessage="No entitlements are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {entitlementsState.data?.items.map(entitlement => (
            <article key={entitlement.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <h3>
                    <Link className="inline-link" to={`/entitlements/${entitlement.id}`}>
                      {entitlement.name}
                    </Link>
                  </h3>
                  <p className="tag-copy">{entitlement.descriptionPreview || 'No description'}</p>
                  <div className="pill-row">
                    {(entitlement.supportLevels || []).map(level => (
                      <span key={level} className="status-pill">
                        {level}
                      </span>
                    ))}
                    {(!entitlement.supportLevels || entitlement.supportLevels.length === 0) && (
                      <span className="muted-text">No support levels</span>
                    )}
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function EntitlementDetailPage({ sessionState }) {
  const { id } = useParams();
  const entitlementState = useJson(id ? `/api/entitlements/${id}` : null);
  const entitlement = entitlementState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{entitlement?.name || 'Entitlement details'}</h2>
        </div>
      </div>

      <DataState state={entitlementState} emptyMessage="Entitlement not found." signInHref={sessionState.data?.homePath || '/login'}>
        {entitlement && (
          <div className="article-detail">
            <div className="admin-detail-layout">
              <section className="detail-section">
                <div className="markdown-card">
                  {entitlement.description ? <MarkdownContent>{entitlement.description}</MarkdownContent> : <p className="muted-text">No description.</p>}
                </div>

                <div className="detail-card">
                  <h3>Versions</h3>
                  <div className="version-list">
                    {(entitlement.versions || []).map(version => (
                      <div key={version.id || `${version.name}-${version.date}`} className="version-row">
                        <strong>{version.name}</strong>
                        <span>{version.date || 'No date'}</span>
                      </div>
                    ))}
                    {(!entitlement.versions || entitlement.versions.length === 0) && <p className="muted-text">No versions.</p>}
                  </div>
                </div>
              </section>

              <section className="detail-section">
                <div className="detail-card">
                  <h3>Support levels</h3>
                  <div className="checkbox-list">
                    {(entitlement.supportLevels || []).map(level => (
                      <div key={level.id} className="checkbox-card">
                        <span>
                          <strong>{level.name}</strong>
                          <small>
                            {level.fromLabel} - {level.toLabel}
                          </small>
                        </span>
                      </div>
                    ))}
                    {(!entitlement.supportLevels || entitlement.supportLevels.length === 0) && (
                      <p className="muted-text">No support levels.</p>
                    )}
                  </div>
                </div>
              </section>
            </div>

            {entitlement.editPath && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={entitlement.editPath}>
                  Edit
                </SmartLink>
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function EntitlementFormPage({ sessionState, mode }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const entitlementState = useJson(mode === 'edit' && id ? `/api/entitlements/${id}` : '/api/entitlements/bootstrap');
  const entitlement = entitlementState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!entitlement) {
      return;
    }
    if (isEdit) {
      setFormState({
        name: entitlement.name || '',
        description: entitlement.description || '',
        selectedLevelIds: entitlement.selectedLevelIds || [],
        versions:
          entitlement.versions?.map(version => ({
            id: version.id ? String(version.id) : '',
            name: version.name || '',
            date: version.date || ''
          })) || []
      });
      return;
    }
    setFormState({
      name: '',
      description: '',
      selectedLevelIds: [],
      versions: [{ id: '', name: '', date: entitlement.todayDate || '' }]
    });
  }, [entitlement, isEdit]);

  const toggleLevel = levelId => {
    setFormState(current => ({
      ...current,
      selectedLevelIds: current.selectedLevelIds.includes(levelId)
        ? current.selectedLevelIds.filter(existing => existing !== levelId)
        : [...current.selectedLevelIds, levelId]
    }));
  };

  const updateVersion = (index, field, value) => {
    setFormState(current => ({
      ...current,
      versions: current.versions.map((version, versionIndex) =>
        versionIndex === index ? { ...version, [field]: value } : version
      )
    }));
  };

  const addVersion = () => {
    setFormState(current => ({
      ...current,
      versions: [...current.versions, { id: '', name: '', date: entitlement?.todayDate || '' }]
    }));
  };

  const removeVersion = index => {
    setFormState(current => ({
      ...current,
      versions: current.versions.filter((_, versionIndex) => versionIndex !== index)
    }));
  };

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(isEdit ? `/entitlements/${id}` : '/entitlements', [
        ['name', formState.name],
        ['description', formState.description],
        ...formState.selectedLevelIds.map(levelId => ['levelIds', String(levelId)]),
        ...formState.versions.flatMap(version => [
          ['versionIds', version.id || ''],
          ['versionNames', version.name],
          ['versionDates', version.date]
        ])
      ]);
      navigate('/entitlements');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save entitlement.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteEntitlement = async () => {
    if (!id || !window.confirm('Delete this entitlement?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/entitlements/${id}/delete`, []);
      navigate('/entitlements');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete entitlement.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href={isEdit && id ? `/entitlements/${id}` : '/entitlements'}>
            Back to entitlements
          </SmartLink>
          <h2>{isEdit ? 'Edit entitlement' : 'New entitlement'}</h2>
        </div>
        <div className="button-row">
          {isEdit && (
            <button type="button" className="secondary-button danger-button" onClick={deleteEntitlement} disabled={saveState.saving}>
              Delete entitlement
            </button>
          )}
        </div>
      </div>

      <DataState state={entitlementState} emptyMessage="Entitlement not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && entitlement && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label>
                Name
                <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
              </label>
              <label className="form-span-2">
                Description
                <textarea
                  rows={6}
                  value={formState.description}
                  onChange={event => setFormState(current => ({ ...current, description: event.target.value }))}
                  required
                />
              </label>
            </div>

            <section className="detail-card">
              <h3>Support levels</h3>
              <div className="checkbox-list">
                {(entitlement.supportLevels || []).map(level => (
                  <label key={level.id} className="checkbox-card">
                    <input
                      type="checkbox"
                      checked={formState.selectedLevelIds.includes(level.id)}
                      onChange={() => toggleLevel(level.id)}
                    />
                    <span>
                      <strong>{level.name}</strong>
                      <small>
                        {level.fromLabel} - {level.toLabel}
                      </small>
                    </span>
                  </label>
                ))}
              </div>
            </section>

            <section className="detail-card">
              <div className="section-header compact-header">
                <div>
                  <h3>Versions</h3>
                  <p className="section-copy">At least one version is required.</p>
                </div>
                <button type="button" className="secondary-button" onClick={addVersion}>
                  Add version
                </button>
              </div>
              <div className="version-editor-list">
                {formState.versions.map((version, index) => (
                  <div key={`${version.id || 'new'}-${index}`} className="version-editor-card">
                    <div className="owner-form-grid">
                      <label>
                        Version
                        <input value={version.name} onChange={event => updateVersion(index, 'name', event.target.value)} required />
                      </label>
                      <label>
                        Date
                        <input type="date" value={version.date} onChange={event => updateVersion(index, 'date', event.target.value)} required />
                      </label>
                    </div>
                    <div className="button-row">
                      <button
                        type="button"
                        className="secondary-button"
                        onClick={() => removeVersion(index)}
                        disabled={formState.versions.length === 1}
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isEdit ? 'Save entitlement' : 'Create entitlement'}
              </button>
              <SmartLink className="secondary-button" href={isEdit && id ? `/entitlements/${id}` : '/entitlements'}>
                Cancel
              </SmartLink>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function LevelsPage({ sessionState }) {
  const levelsState = useJson('/api/levels');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Support levels</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href="/levels/new">
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={levelsState} emptyMessage="No support levels are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {levelsState.data?.items.map(level => (
            <article key={level.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>
                      <Link className="inline-link" to={`/levels/${level.id}`}>
                        {level.name}
                      </Link>
                    </h3>
                    <LevelColorBadge color={level.color} display={level.colorDisplay} />
                  </div>
                  <p className="tag-copy">{level.descriptionPreview || 'No description'}</p>
                  <p className="muted-text">
                    Level {level.level} • {level.fromLabel} - {level.toLabel}
                  </p>
                  <p className="muted-text">
                    {level.countryName || 'No country'} • {level.timezoneName || 'No time zone'}
                  </p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function LevelDetailPage({ sessionState }) {
  const { id } = useParams();
  const levelState = useJson(id ? `/api/levels/${id}` : null);
  const level = levelState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{level?.name || 'Level details'}</h2>
        </div>
      </div>

      <DataState state={levelState} emptyMessage="Level not found." signInHref={sessionState.data?.homePath || '/login'}>
        {level && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={level.name || '—'} readOnly />
                  </label>
                  <label>
                    Business level
                    <input value={level.level ?? '—'} readOnly />
                  </label>
                  <label>
                    Color
                    <LevelColorFieldValue color={level.color} display={level.colorDisplay} />
                  </label>
                  <label>
                    Country
                    <input value={level.countryName || '—'} readOnly />
                  </label>
                  <label>
                    From
                    <input value={level.fromLabel || '—'} readOnly />
                  </label>
                  <label>
                    To
                    <input value={level.toLabel || '—'} readOnly />
                  </label>
                  <label>
                    Time zone
                    <input value={level.timezoneName || '—'} readOnly />
                  </label>
                  <div className="detail-card-spacer" aria-hidden="true" />
                  <label className="form-span-2">
                    Description
                    <textarea value={level.description || '—'} readOnly rows={6} />
                  </label>
                </div>
              </div>
            </div>

            {level.editPath && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={level.editPath}>
                  Edit
                </SmartLink>
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function LevelFormPage({ sessionState, mode }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const levelState = useJson(mode === 'edit' && id ? `/api/levels/${id}` : '/api/levels/bootstrap');
  const level = levelState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!level) {
      return;
    }
    setFormState({
      name: level.name || '',
      description: level.description || '',
      level: String(level.level ?? level.defaultLevel ?? 0),
      color: level.color || level.defaultColor || 'White',
      fromDay: String(level.fromDay ?? level.defaultFromDay ?? 1),
      fromTime: String(level.fromTime ?? level.defaultFromTime ?? 0),
      toDay: String(level.toDay ?? level.defaultToDay ?? 7),
      toTime: String(level.toTime ?? level.defaultToTime ?? 23),
      countryId: level.countryId ? String(level.countryId) : level.defaultCountryId ? String(level.defaultCountryId) : '',
      timezoneId: level.timezoneId ? String(level.timezoneId) : level.defaultTimezoneId ? String(level.defaultTimezoneId) : ''
    });
  }, [level]);

  const availableTimezones =
    level?.timezones?.filter(timezone => !formState?.countryId || String(timezone.countryId) === formState.countryId) || [];

  const submit = async event => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(isEdit ? `/levels/${id}` : '/levels', [
        ['name', formState.name],
        ['description', formState.description],
        ['level', formState.level],
        ['color', formState.color],
        ['fromDay', formState.fromDay],
        ['fromTime', formState.fromTime],
        ['toDay', formState.toDay],
        ['toTime', formState.toTime],
        ['countryId', formState.countryId],
        ['timezoneId', formState.timezoneId]
      ]);
      navigate('/levels');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save level.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteLevel = async () => {
    if (!id || !window.confirm('Delete this level?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/levels/${id}/delete`, []);
      navigate('/levels');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete level.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{isEdit ? 'Edit level' : 'New level'}</h2>
        </div>
      </div>

      <DataState state={levelState} emptyMessage="Level not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && level && (
          <form className="owner-form" onSubmit={submit}>
            <div className={isEdit ? 'form-card ticket-detail-card' : ''}>
              <div className={isEdit ? 'owner-form owner-detail-form' : ''}>
                <div className={`owner-form-grid${isEdit ? ' ticket-detail-grid' : ''}`}>
                  <label>
                    Name
                    <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
                  </label>
                  <label>
                    Level
                    <input
                      type="number"
                      min="0"
                      value={formState.level}
                      onChange={event => setFormState(current => ({ ...current, level: event.target.value }))}
                      required
                    />
                  </label>
                  <label>
                    Color
                    <select value={formState.color} onChange={event => setFormState(current => ({ ...current, color: event.target.value }))}>
                      {(level.colorOptions || []).map(option => (
                        <option key={option.value} value={option.value}>
                          {`${levelColorMarker(option.value)} ${option.display || `(${option.value})`}`}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={event => {
                        const nextCountryId = event.target.value;
                        const timezoneStillValid = availableTimezones.some(timezone => String(timezone.id) === formState.timezoneId);
                        setFormState(current => ({
                          ...current,
                          countryId: nextCountryId,
                          timezoneId: timezoneStillValid ? current.timezoneId : ''
                        }));
                      }}
                    >
                      <option value="">Select a country</option>
                      {(level.countries || []).map(country => (
                        <option key={country.id} value={country.id}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    From day
                    <select value={formState.fromDay} onChange={event => setFormState(current => ({ ...current, fromDay: event.target.value }))}>
                      {(level.dayOptions || []).map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    To day
                    <select value={formState.toDay} onChange={event => setFormState(current => ({ ...current, toDay: event.target.value }))}>
                      {(level.dayOptions || []).map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select value={formState.timezoneId} onChange={event => setFormState(current => ({ ...current, timezoneId: event.target.value }))}>
                      <option value="">Select a time zone</option>
                      {availableTimezones.map(timezone => (
                        <option key={timezone.id} value={timezone.id}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <div className="detail-card-spacer" aria-hidden="true" />
                  <label>
                    From time
                    <select value={formState.fromTime} onChange={event => setFormState(current => ({ ...current, fromTime: event.target.value }))}>
                      {(level.hourOptions || []).map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    To time
                    <select value={formState.toTime} onChange={event => setFormState(current => ({ ...current, toTime: event.target.value }))}>
                      {(level.hourOptions || []).map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="form-span-2">
                    Description
                    <textarea
                      rows={6}
                      value={formState.description}
                      onChange={event => setFormState(current => ({ ...current, description: event.target.value }))}
                      required
                    />
                  </label>
                </div>
              </div>
            </div>

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${isEdit ? ' button-row-split' : ''}`}>
              {isEdit && (
                <button type="button" className="secondary-button danger-button" onClick={deleteLevel} disabled={saveState.saving}>
                  Delete
                </button>
              )}
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isEdit ? 'Save' : 'Create level'}
              </button>
              {!isEdit && (
                <SmartLink className="secondary-button" href={isEdit && id ? `/levels/${id}` : '/levels'}>
                  Cancel
                </SmartLink>
              )}
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function OwnerSelector({ title, users, selectedIds, onToggle }) {
  return (
    <section className="detail-card">
      <h3>{title}</h3>
      <div className="checkbox-list">
        {users.length === 0 ? (
          <p className="muted-text">No users available.</p>
        ) : (
          users.map(user => (
            <label key={user.id} className="checkbox-card">
              <input type="checkbox" checked={selectedIds.includes(user.id)} onChange={() => onToggle(user.id)} />
              <span>
                <strong>{user.displayName || user.username}</strong>
                <small>{user.email}</small>
              </span>
            </label>
          ))
        )}
      </div>
    </section>
  );
}

function SelectableUserPicker({ title, users, selectedIds, onToggle }) {
  return (
    <section className="detail-card">
      <h3>{title}</h3>
      <div className="checkbox-list">
        {users.length === 0 ? (
          <p className="muted-text">No users available.</p>
        ) : (
          users.map(user => (
            <label key={user.id} className="checkbox-card">
              <input type="checkbox" checked={selectedIds.includes(user.id)} onChange={() => onToggle(user.id)} />
              <span>
                <strong>{user.displayName || user.username}</strong>
                <small>{user.email}</small>
              </span>
            </label>
          ))
        )}
      </div>
    </section>
  );
}

function SelectableUserSummary({ users }) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }

  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          {(user.displayName || user.username) + (user.email ? ` (${user.email})` : '')}
        </li>
      ))}
    </ul>
  );
}

function LevelColorBadge({ color, display }) {
  return (
    <span className="status-pill level-color-badge">
      <span className="level-color-swatch" style={{ backgroundColor: color || 'transparent' }} aria-hidden="true" />
      <span>{display || 'No color'}</span>
    </span>
  );
}

function LevelColorFieldValue({ color, display }) {
  return (
    <div className="level-color-field-value">
      <span className="level-color-swatch" style={{ backgroundColor: color || 'transparent' }} aria-hidden="true" />
      <span>{display || '—'}</span>
    </div>
  );
}

function levelColorMarker(color) {
  switch ((color || '').toLowerCase()) {
    case 'black':
      return '⬛';
    case 'silver':
    case 'white':
      return '⬜';
    case 'gray':
      return '◻️';
    case 'maroon':
    case 'red':
      return '🟥';
    case 'purple':
    case 'fuchsia':
      return '🟪';
    case 'green':
    case 'lime':
      return '🟩';
    case 'olive':
    case 'yellow':
      return '🟨';
    case 'navy':
    case 'blue':
    case 'teal':
    case 'aqua':
      return '🟦';
    default:
      return '◻️';
  }
}

function sortUsersByName(users) {
  return [...users].sort((left, right) =>
    (left.displayName || left.username || '').localeCompare(right.displayName || right.username || '', undefined, {
      sensitivity: 'base'
    })
  );
}

function sortEntitlementAssignments(assignments) {
  return [...assignments].sort((left, right) => {
    const entitlementComparison = (left.entitlementName || '').localeCompare(right.entitlementName || '', undefined, {
      sensitivity: 'base'
    });
    if (entitlementComparison !== 0) {
      return entitlementComparison;
    }
    return (left.levelName || '').localeCompare(right.levelName || '', undefined, { sensitivity: 'base' });
  });
}

function OwnerUserList({ users }) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }

  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          <a href={user.profilePath}>{user.displayName || user.username}</a>
        </li>
      ))}
    </ul>
  );
}

function DeleteArticleButton({ articleId, label = 'Delete article' }) {
  const navigate = useNavigate();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState('');

  const remove = async () => {
    if (!window.confirm('Delete this article?')) {
      return;
    }
    setDeleting(true);
    setError('');
    try {
      await postForm(`/articles/${articleId}/delete`, []);
      navigate('/articles');
    } catch (submitError) {
      setDeleting(false);
      setError(submitError.message || 'Unable to delete article.');
      return;
    }
    setDeleting(false);
  };

  return (
    <>
      <button type="button" className="secondary-button danger-button" onClick={remove} disabled={deleting}>
        {deleting ? 'Deleting...' : label}
      </button>
      {error && <p className="error-text">{error}</p>}
    </>
  );
}

function DirectoryUsersPage({ sessionState, apiBase, basePath, titleFallback, description }) {
  const location = useLocation();
  const navigate = useNavigate();
  const companyId = new URLSearchParams(location.search).get('companyId') || '';
  const dataState = useJson(`${apiBase}${toQueryString({ companyId })}`);
  const directory = dataState.data;

  const selectCompany = event => {
    const nextCompanyId = event.target.value;
    navigate(`${location.pathname}${toQueryString({ companyId: nextCompanyId })}`);
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{directory?.title || titleFallback}</h2>
          {directory?.description || description ? <p className="section-copy">{directory?.description || description}</p> : null}
        </div>
        <div className="button-row">
          {directory?.createPath && (
            <SmartLink className="primary-button" href={directory.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={dataState} emptyMessage="No users are available." signInHref={sessionState.data?.homePath || '/login'}>
        <>
          {directory?.showCompanySelector && (
            <section className="detail-card">
              <h3>Company</h3>
              <label>
                Select company
                <select value={directory.selectedCompanyId ? String(directory.selectedCompanyId) : ''} onChange={selectCompany}>
                  {(directory.companies || []).map(company => (
                    <option key={company.id} value={company.id ? String(company.id) : ''}>
                      {company.name}
                    </option>
                  ))}
                </select>
              </label>
            </section>
          )}

          <div className="category-list">
            {(directory?.items || []).map(user => (
              <article key={user.id} className="category-card">
                <div className="category-card-head">
                  <div>
                    <div className="category-title-row">
                      <h3>{user.displayName || user.fullName || user.username || 'User'}</h3>
                      <span className="status-pill">{user.typeLabel || user.type || 'User'}</span>
                    </div>
                    <p className="tag-copy">{user.email || 'No email'}</p>
                    <p className="muted-text">@{user.username || 'unknown'}</p>
                  </div>
                  <div className="button-row">
                    {user.detailPath && (
                      <SmartLink className="inline-link" href={user.detailPath}>
                        Open
                      </SmartLink>
                    )}
                    {user.editPath && (
                      <SmartLink className="inline-link" href={user.editPath}>
                        Edit
                      </SmartLink>
                    )}
                  </div>
                </div>
              </article>
            ))}
          </div>

          {(!directory?.items || directory.items.length === 0) && (
            <p className="muted-text">No users are available for the selected company.</p>
          )}
        </>
      </DataState>
    </section>
  );
}

function DirectoryUserFormPage({ sessionState, bootstrapBase, navigateFallback }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get('companyId') || '';
  const isEdit = Boolean(id);
  const isAdminCreate = !isEdit && bootstrapBase === '/api/admin/users/bootstrap';
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const selectedCountryId = formState?.countryId || '';
  const bootstrapState = useJson(
    `${bootstrapBase}${toQueryString({
      userId: isEdit ? id : undefined,
      companyId: formState?.companyId || requestedCompanyId,
      countryId: selectedCountryId
    })}`
  );
  const bootstrap = bootstrapState.data;

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState(current => {
      if (!current || String(current.id || '') !== String(bootstrap.user?.id || '')) {
        return createDirectoryUserFormState(bootstrap);
      }
      const timezones = bootstrap.timezones || [];
      const hasTimezone = timezones.some(timezone => String(timezone.id) === String(current.timezoneId || ''));
      return {
        ...current,
        companyId: current.companyId || (bootstrap.selectedCompanyId ? String(bootstrap.selectedCompanyId) : ''),
        timezoneId: hasTimezone ? current.timezoneId : timezones[0]?.id ? String(timezones[0].id) : '',
        type: current.type || bootstrap.user?.type || bootstrap.types?.[0]?.value || ''
      };
    });
  }, [bootstrap]);

  const submit = async event => {
    event.preventDefault();
    if (!formState || !bootstrap) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(bootstrap.submitPath, [
        ['name', formState.name],
        ['fullName', formState.fullName],
        ['email', formState.email],
        ['social', formState.social],
        ['phoneNumber', formState.phoneNumber],
        ['phoneExtension', formState.phoneExtension],
        ['countryId', formState.countryId],
        ['timezoneId', formState.timezoneId],
        ['type', formState.type],
        ['companyId', formState.companyId],
        ['password', formState.password]
      ]);
      navigate(resolveClientPath(bootstrap.cancelPath, navigateFallback));
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save user.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteUser = async () => {
    if (!id || !bootstrap?.submitPath?.startsWith('/user/') || !window.confirm('Delete this user?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/user/${id}/delete`, []);
      navigate(resolveClientPath(bootstrap.cancelPath, navigateFallback));
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete user.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      {!isAdminCreate && (
        <div className="section-header">
          <div>
            <SmartLink className="inline-link back-link" href={bootstrap?.cancelPath || navigateFallback}>
              Back
            </SmartLink>
            <h2>{bootstrap?.title || (isEdit ? 'Edit user' : 'New user')}</h2>
          </div>
          <div className="button-row">
            {isEdit && bootstrap?.submitPath?.startsWith('/user/') && (
              <button type="button" className="secondary-button danger-button" onClick={deleteUser} disabled={saveState.saving}>
                Delete user
              </button>
            )}
          </div>
        </div>
      )}

      <DataState state={bootstrapState} emptyMessage="Unable to load the user form." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && bootstrap && (
          <form className="owner-form" onSubmit={submit}>
            <div className={isAdminCreate ? 'form-card ticket-detail-card' : ''}>
              <div className={isAdminCreate ? 'owner-form owner-detail-form' : ''}>
                <div className={`owner-form-grid${isAdminCreate ? ' ticket-detail-grid' : ''}`}>
                  <label>
                    Username
                    <input value={formState.name} onChange={event => setFormState(current => ({ ...current, name: event.target.value }))} required />
                  </label>
                  <label>
                    Full name
                    <input value={formState.fullName} onChange={event => setFormState(current => ({ ...current, fullName: event.target.value }))} />
                  </label>
                  <label>
                    Email
                    <input type="email" value={formState.email} onChange={event => setFormState(current => ({ ...current, email: event.target.value }))} required />
                  </label>
                  <label>
                    Social
                    <input value={formState.social} onChange={event => setFormState(current => ({ ...current, social: event.target.value }))} />
                  </label>
                  <label>
                    Phone number
                    <input value={formState.phoneNumber} onChange={event => setFormState(current => ({ ...current, phoneNumber: event.target.value }))} />
                  </label>
                  <label>
                    Extension
                    <input value={formState.phoneExtension} onChange={event => setFormState(current => ({ ...current, phoneExtension: event.target.value }))} />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={event =>
                        setFormState(current => ({
                          ...current,
                          countryId: event.target.value,
                          timezoneId: ''
                        }))
                      }
                    >
                      <option value="">Select country</option>
                      {(bootstrap.countries || []).map(country => (
                        <option key={country.id} value={country.id ? String(country.id) : ''}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select
                      value={formState.timezoneId}
                      onChange={event => setFormState(current => ({ ...current, timezoneId: event.target.value }))}
                    >
                      <option value="">Select time zone</option>
                      {(bootstrap.timezones || []).map(timezone => (
                        <option key={timezone.id} value={timezone.id ? String(timezone.id) : ''}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Type
                    <select value={formState.type} onChange={event => setFormState(current => ({ ...current, type: event.target.value }))} required>
                      {(bootstrap.types || []).map(type => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Company
                    <select
                      value={formState.companyId}
                      disabled={bootstrap.companyLocked}
                      onChange={event => setFormState(current => ({ ...current, companyId: event.target.value }))}
                    >
                      {(bootstrap.companies || []).map(company => (
                        <option key={company.id} value={company.id ? String(company.id) : ''}>
                          {company.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Password {bootstrap.passwordRequired ? '' : '(leave blank to keep current password)'}
                    <input
                      type="password"
                      value={formState.password}
                      onChange={event => setFormState(current => ({ ...current, password: event.target.value }))}
                      required={bootstrap.passwordRequired}
                    />
                  </label>
                  {isAdminCreate && <div className="detail-card-spacer" aria-hidden="true" />}
                </div>
              </div>
            </div>

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${isAdminCreate ? ' button-row-end' : ''}`}>
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isAdminCreate ? 'Create' : bootstrap.title || (isEdit ? 'Save user' : 'Create user')}
              </button>
              {!isAdminCreate && (
                <SmartLink className="secondary-button" href={bootstrap.cancelPath || navigateFallback}>
                  Cancel
                </SmartLink>
              )}
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function DirectoryUserDetailPage({ sessionState, apiBase, backFallback }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const detailState = useJson(id ? `${apiBase}/${id}` : null);
  const detail = detailState.data;

  const deleteUser = async () => {
    if (!detail?.deletePath || !window.confirm('Delete this user?')) {
      return;
    }
    try {
      await postForm(detail.deletePath, []);
      navigate(resolveClientPath(detail.backPath, backFallback));
    } catch (error) {
      window.alert(error.message || 'Unable to delete user.');
    }
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href={detail?.backPath || backFallback}>
            Back
          </SmartLink>
          <h2>{detail?.displayName || detail?.fullName || detail?.username || 'User details'}</h2>
        </div>
      </div>

      <DataState state={detailState} emptyMessage="User not found." signInHref={sessionState.data?.homePath || '/login'}>
        {detail && (
          <div className="article-detail">
            <div className="admin-detail-layout">
              <section className="detail-section">
                <section className="detail-grid">
                  <div className="detail-card">
                    <h3>Username</h3>
                    <p>{detail.username || '—'}</p>
                  </div>
                  <div className="detail-card">
                    <h3>Type</h3>
                    <p>{detail.typeLabel || detail.type || 'User'}</p>
                  </div>
                  <div className="detail-card">
                    <h3>Email</h3>
                    <p>{detail.email || '—'}</p>
                  </div>
                  <div className="detail-card">
                    <h3>Phone</h3>
                    <p>{formatPhone(detail.phoneNumber, detail.phoneExtension)}</p>
                  </div>
                  <div className="detail-card">
                    <h3>Country</h3>
                    <p>{detail.countryName || '—'}</p>
                  </div>
                  <div className="detail-card">
                    <h3>Time zone</h3>
                    <p>{detail.timezoneName || '—'}</p>
                  </div>
                </section>
              </section>

              <section className="detail-section">
                <div className="detail-card">
                  <h3>Profile</h3>
                  <ul className="plain-list">
                    <li>{detail.fullName || 'No full name'}</li>
                    <li>{detail.social || 'No social profile'}</li>
                  </ul>
                </div>

                <div className="detail-card">
                  <h3>Company</h3>
                  {detail.companyId ? (
                    <SmartLink className="inline-link" href={detail.backPath || backFallback}>
                      {detail.companyName || 'Open company'}
                    </SmartLink>
                  ) : (
                    <p className="muted-text">No company</p>
                  )}
                </div>
              </section>
            </div>

            {(detail.editPath || detail.deletePath) && (
              <div className="button-row button-row-end admin-detail-actions">
                {detail.editPath && (
                  <SmartLink className="primary-button" href={detail.editPath}>
                    Edit
                  </SmartLink>
                )}
                {detail.deletePath && (
                  <button type="button" className="secondary-button danger-button" onClick={deleteUser}>
                    Delete user
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function DirectoryCompanyDetailPage({ sessionState, apiBase, backFallback }) {
  const { id } = useParams();
  const companyState = useJson(id ? `${apiBase}/${id}` : null);
  const company = companyState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href={company?.backPath || backFallback}>
            Back
          </SmartLink>
          <h2>{company?.name || 'Company details'}</h2>
        </div>
      </div>

      <DataState state={companyState} emptyMessage="Company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {company && (
          <div className="article-detail">
            <section className="detail-grid">
              <div className="detail-card">
                <h3>Phone</h3>
                <p>{company.phoneNumber || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Country</h3>
                <p>{company.countryName || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Time zone</h3>
                <p>{company.timezoneName || '—'}</p>
              </div>
            </section>

            <section className="owner-columns">
              <div className="detail-card">
                <h3>Address</h3>
                <ul className="plain-list">
                  {[company.address1, company.address2, company.city, company.state, company.zip].filter(Boolean).length === 0 ? (
                    <li>—</li>
                  ) : (
                    [company.address1, company.address2, company.city, company.state, company.zip]
                      .filter(Boolean)
                      .map(line => <li key={line}>{line}</li>)
                  )}
                </ul>
              </div>
              <div className="detail-card">
                <h3>Users</h3>
                <DirectoryUserReferenceList users={company.users} />
              </div>
              <div className="detail-card">
                <h3>Superusers</h3>
                <DirectoryUserReferenceList users={company.superusers} />
              </div>
              <div className="detail-card">
                <h3>TAMs</h3>
                <DirectoryUserReferenceList users={company.tamUsers} />
              </div>
              <div className="detail-card">
                <h3>Support users</h3>
                <DirectoryUserReferenceList users={company.supportUsers} />
              </div>
            </section>
          </div>
        )}
      </DataState>
    </section>
  );
}

function DirectoryUserReferenceList({ users }) {
  if (!users || users.length === 0) {
    return <p className="muted-text">No users.</p>;
  }

  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id || `${user.username}-${user.type}`}>
          {user.detailPath ? (
            <SmartLink className="inline-link" href={user.detailPath}>
              {user.displayName || user.username || 'User'}
            </SmartLink>
          ) : (
            <span>{user.displayName || user.username || 'User'}</span>
          )}{' '}
          <span className="muted-text">({user.typeLabel || user.type || 'User'})</span>
        </li>
      ))}
    </ul>
  );
}

function TicketWorkbenchPage({ sessionState }) {
  const ticketState = useJson('/api/ticket-workbench');
  const tickets = ticketState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{tickets?.title || 'Tickets'}</h2>
        </div>
        <div className="button-row">
          {tickets?.createPath && (
            <SmartLink className="primary-button" href={tickets.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={ticketState} emptyMessage="No tickets are available." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {(tickets?.items || []).map(ticket => (
            <article key={ticket.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>{ticket.name}</h3>
                    <span className="status-pill">{ticket.status || 'No status'}</span>
                  </div>
                  <p className="tag-copy">{ticket.companyName || 'No company'}</p>
                  <p className="muted-text">
                    {ticket.requesterName || 'No requester'} • {ticket.categoryName || 'No category'}
                  </p>
                  <p className="muted-text">Latest message: {ticket.lastMessageLabel || '-'}</p>
                  {ticket.externalIssueLink && (
                    <p className="muted-text">
                      <a href={ticket.externalIssueLink} target="_blank" rel="noreferrer">
                        External issue
                      </a>
                    </p>
                  )}
                </div>
                <div className="button-row">
                  <SmartLink className="inline-link" href={ticket.detailPath}>
                    Open
                  </SmartLink>
                  <SmartLink className="inline-link" href={ticket.editPath}>
                    Edit
                  </SmartLink>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function TicketWorkbenchFormPage({ sessionState }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get('companyId') || '';
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const bootstrapState = useJson(
    `/api/ticket-workbench/bootstrap${toQueryString({ ticketId: id, companyId: formState?.companyId || requestedCompanyId })}`
  );
  const bootstrap = bootstrapState.data;

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState(current => {
      if (!current || String(current.id || '') !== String(bootstrap.ticket?.id || '')) {
        return {
          id: bootstrap.ticket?.id ? String(bootstrap.ticket.id) : '',
          status: bootstrap.ticket?.status || 'Open',
          companyId: bootstrap.ticket?.companyId ? String(bootstrap.ticket.companyId) : '',
          companyEntitlementId: bootstrap.ticket?.companyEntitlementId ? String(bootstrap.ticket.companyEntitlementId) : '',
          categoryId: bootstrap.ticket?.categoryId ? String(bootstrap.ticket.categoryId) : '',
          externalIssueLink: bootstrap.ticket?.externalIssueLink || '',
          affectsVersionId: bootstrap.ticket?.affectsVersionId ? String(bootstrap.ticket.affectsVersionId) : '',
          resolvedVersionId: bootstrap.ticket?.resolvedVersionId ? String(bootstrap.ticket.resolvedVersionId) : ''
        };
      }
      const entitlements = bootstrap.entitlements || [];
      const versions = bootstrap.versions || [];
      return {
        ...current,
        companyEntitlementId: entitlements.some(option => String(option.id) === current.companyEntitlementId)
          ? current.companyEntitlementId
          : entitlements[0]?.id
            ? String(entitlements[0].id)
            : '',
        affectsVersionId: versions.some(option => String(option.id) === current.affectsVersionId)
          ? current.affectsVersionId
          : versions[0]?.id
            ? String(versions[0].id)
            : '',
        resolvedVersionId: versions.some(option => String(option.id) === current.resolvedVersionId) ? current.resolvedVersionId : ''
      };
    });
  }, [bootstrap]);

  const submit = async event => {
    event.preventDefault();
    if (!formState || !bootstrap) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(bootstrap.submitPath, [
        ['status', formState.status],
        ['companyId', formState.companyId],
        ['companyEntitlementId', formState.companyEntitlementId],
        ['categoryId', formState.categoryId],
        ['externalIssueLink', formState.externalIssueLink],
        ['affectsVersionId', formState.affectsVersionId],
        ['resolvedVersionId', formState.resolvedVersionId]
      ]);
      navigate('/tickets');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteTicket = async () => {
    if (!id || !window.confirm('Delete this ticket?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/tickets/${id}/delete`, []);
      navigate('/tickets');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href="/tickets">
            Back to tickets
          </SmartLink>
          <h2>{bootstrap?.title || 'Ticket form'}</h2>
        </div>
        <div className="button-row">
          {id && (
            <button type="button" className="secondary-button danger-button" onClick={deleteTicket} disabled={saveState.saving}>
              Delete ticket
            </button>
          )}
        </div>
      </div>

      <DataState state={bootstrapState} emptyMessage="Unable to load the ticket form." signInHref={sessionState.data?.homePath || '/login'}>
        {bootstrap && formState && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label>
                Status
                <select value={formState.status} onChange={event => setFormState(current => ({ ...current, status: event.target.value }))}>
                  {['Open', 'Assigned', 'In Progress', 'Pending', 'Closed'].map(status => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Company
                <select
                  value={formState.companyId}
                  onChange={event =>
                    setFormState(current => ({
                      ...current,
                      companyId: event.target.value,
                      companyEntitlementId: '',
                      affectsVersionId: '',
                      resolvedVersionId: ''
                    }))
                  }
                >
                  {(bootstrap.companies || []).map(company => (
                    <option key={company.id} value={company.id ? String(company.id) : ''}>
                      {company.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Entitlement
                <select
                  value={formState.companyEntitlementId}
                  onChange={event => setFormState(current => ({ ...current, companyEntitlementId: event.target.value }))}
                >
                  {(bootstrap.entitlements || []).map(entitlement => (
                    <option key={entitlement.id} value={entitlement.id ? String(entitlement.id) : ''}>
                      {entitlement.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Category
                <select value={formState.categoryId} onChange={event => setFormState(current => ({ ...current, categoryId: event.target.value }))}>
                  {(bootstrap.categories || []).map(category => (
                    <option key={category.id} value={category.id ? String(category.id) : ''}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Affects version
                <select
                  value={formState.affectsVersionId}
                  onChange={event => setFormState(current => ({ ...current, affectsVersionId: event.target.value }))}
                >
                  <option value="">Select version</option>
                  {(bootstrap.versions || []).map(version => (
                    <option key={version.id} value={version.id ? String(version.id) : ''}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Resolved version
                <select
                  value={formState.resolvedVersionId}
                  onChange={event => setFormState(current => ({ ...current, resolvedVersionId: event.target.value }))}
                >
                  <option value="">Select version</option>
                  {(bootstrap.versions || []).map(version => (
                    <option key={version.id} value={version.id ? String(version.id) : ''}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-span-2">
                External issue
                <input
                  value={formState.externalIssueLink}
                  onChange={event => setFormState(current => ({ ...current, externalIssueLink: event.target.value }))}
                  placeholder="https://github.com/..."
                />
              </label>
            </div>

            {bootstrap.edit && (
              <section className="detail-card">
                <h3>Messages</h3>
                <ul className="plain-list">
                  {(bootstrap.messages || []).map(message => (
                    <li key={message.id}>
                      <strong>{message.dateLabel || '-'}</strong> — {message.body || 'No message body'}
                    </li>
                  ))}
                  {(!bootstrap.messages || bootstrap.messages.length === 0) && <li>No messages yet.</li>}
                </ul>
              </section>
            )}

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : bootstrap.edit ? 'Save ticket' : 'Create ticket'}
              </button>
              <SmartLink className="secondary-button" href="/tickets">
                Cancel
              </SmartLink>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function MessagesPage({ sessionState }) {
  const messageState = useJson('/api/messages');
  const messages = messageState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{messages?.title || 'Messages'}</h2>
        </div>
        <div className="button-row">
          {messages?.createPath && (
            <SmartLink className="primary-button" href={messages.createPath}>
              New message
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={messageState} emptyMessage="No messages are available." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {(messages?.items || []).map(message => (
            <article key={message.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>{message.ticketName || 'No ticket'}</h3>
                    <span className="status-pill">{message.date || 'No date'}</span>
                  </div>
                  <p className="tag-copy">{message.preview}</p>
                  <p className="muted-text">
                    {message.authorName || 'Unknown author'} • {message.attachmentCount} attachment(s)
                  </p>
                </div>
                <SmartLink className="inline-link" href={message.editPath}>
                  Edit
                </SmartLink>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}

function MessageFormPage({ sessionState }) {
  const navigate = useNavigate();
  const { id } = useParams();
  const bootstrapState = useJson(`/api/messages/bootstrap${toQueryString({ messageId: id })}`);
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] = useState(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState({
      body: bootstrap.message?.body || '',
      date: bootstrap.message?.date || '',
      ticketId: bootstrap.message?.ticketId ? String(bootstrap.message.ticketId) : '',
      files: []
    });
  }, [bootstrap]);

  const submit = async event => {
    event.preventDefault();
    if (!bootstrap || !formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postMultipart(bootstrap.submitPath, [
        ['body', formState.body],
        ['date', formState.date],
        ['ticketId', formState.ticketId],
        ['attachments', formState.files]
      ]);
      navigate('/messages');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to save message.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteMessage = async () => {
    if (!id || !window.confirm('Delete this message?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/messages/${id}/delete`, []);
      navigate('/messages');
    } catch (error) {
      setSaveState({ saving: false, error: error.message || 'Unable to delete message.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href="/messages">
            Back to messages
          </SmartLink>
          <h2>{bootstrap?.title || 'Message form'}</h2>
        </div>
        <div className="button-row">
          {id && (
            <button type="button" className="secondary-button danger-button" onClick={deleteMessage} disabled={saveState.saving}>
              Delete message
            </button>
          )}
        </div>
      </div>

      <DataState state={bootstrapState} emptyMessage="Unable to load the message form." signInHref={sessionState.data?.homePath || '/login'}>
        {bootstrap && formState && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label className="form-span-2">
                Body
                <textarea rows={8} value={formState.body} onChange={event => setFormState(current => ({ ...current, body: event.target.value }))} required />
              </label>
              <label>
                Date
                <input type="datetime-local" value={formState.date} onChange={event => setFormState(current => ({ ...current, date: event.target.value }))} required />
              </label>
              <label>
                Ticket
                <select value={formState.ticketId} onChange={event => setFormState(current => ({ ...current, ticketId: event.target.value }))} required>
                  <option value="">Select ticket</option>
                  {(bootstrap.tickets || []).map(ticket => (
                    <option key={ticket.id} value={ticket.id ? String(ticket.id) : ''}>
                      {ticket.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <AttachmentPicker files={formState.files} onFilesChange={files => setFormState(current => ({ ...current, files }))} existingAttachments={bootstrap.attachments || []} />

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : bootstrap.edit ? 'Save message' : 'Create message'}
              </button>
              <SmartLink className="secondary-button" href={bootstrap.cancelPath || '/messages'}>
                Cancel
              </SmartLink>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

function AttachmentPage({ sessionState }) {
  const { id } = useParams();
  const attachmentState = useJson(id ? `/api/attachments/${id}` : null);
  const attachment = attachmentState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href={attachment?.backPath || '/'}>
            Back
          </SmartLink>
          <h2>{attachment?.name || 'Attachment'}</h2>
          <p className="section-copy">{attachment?.mimeType || 'Attachment preview'}</p>
        </div>
        <div className="button-row">
          {attachment?.downloadPath && (
            <a className="primary-button" href={attachment.downloadPath} target="_blank" rel="noreferrer">
              Open raw file
            </a>
          )}
        </div>
      </div>

      <DataState state={attachmentState} emptyMessage="Attachment not found." signInHref={sessionState.data?.homePath || '/login'}>
        {attachment && (
          <div className="article-detail">
            <section className="detail-grid">
              <div className="detail-card">
                <h3>Type</h3>
                <p>{attachment.mimeType || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Size</h3>
                <p>{attachment.sizeLabel || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Ticket</h3>
                <p>{attachment.ticketName || '—'}</p>
              </div>
            </section>

            {attachment.image ? (
              <div className="markdown-card">
                <img src={attachment.downloadPath} alt={attachment.name} style={{ maxWidth: '100%' }} />
              </div>
            ) : (
              <section className="detail-card">
                <h3>Contents</h3>
                <pre className="markdown-card">
                  {(attachment.lines || []).map(line => `${line.number}. ${line.content}`).join('\n') || attachment.messageBody || ''}
                </pre>
              </section>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

function AttachmentPicker({ files, onFilesChange, existingAttachments }) {
  return (
    <section className="detail-card">
      <h3>Attachments</h3>
      <input
        type="file"
        multiple
        onChange={event => onFilesChange(Array.from(event.target.files || []))}
      />
      <div className="version-list">
        {files.map(file => (
          <div key={`${file.name}-${file.size}`} className="version-row">
            <strong>{file.name}</strong>
            <span>{file.type || 'application/octet-stream'}</span>
          </div>
        ))}
        {files.length === 0 && <p className="muted-text">No new attachments selected.</p>}
      </div>
      {!!existingAttachments?.length && (
        <>
          <h4>Existing attachments</h4>
          <div className="attachment-table">
            <div className="attachment-row attachment-header-row">
              <span>Name</span>
              <span>Mimetype</span>
              <span>Size</span>
            </div>
            {existingAttachments.map(attachment => (
              <div key={attachment.id} className="attachment-row">
                <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                  {attachment.name}
                </a>
                <span>{attachment.mimeType}</span>
                <span>{attachment.sizeLabel}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

function CategoryDetailPage({ sessionState }) {
  const { id } = useParams();
  const categoryState = useJson(id ? `/api/categories/${id}` : null);
  const category = categoryState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <div className="category-title-row">
            <h2>{category?.name || 'Category details'}</h2>
            {category?.isDefault && <span className="status-pill">Default</span>}
          </div>
        </div>
      </div>

      <DataState state={categoryState} emptyMessage="Category not found." signInHref={sessionState.data?.homePath || '/login'}>
        {category && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={category.name || '—'} readOnly />
                  </label>
                  <label>
                    Default
                    <input value={category.isDefault ? 'Yes' : 'No'} readOnly />
                  </label>
                  <label className="form-span-2">
                    Description
                    <textarea value={category.description || '—'} readOnly rows={10} />
                  </label>
                  <div className="owner-detail-panel form-span-2">
                    <div className="owner-detail-panel-label">Attachments</div>
                    <div className="owner-detail-panel-body">
                      {category.attachments.length === 0 ? (
                        <p className="muted-text">No attachments.</p>
                      ) : (
                        <div className="attachment-table">
                          <div className="attachment-row attachment-header-row">
                            <span>Name</span>
                            <span>Mimetype</span>
                            <span>Size</span>
                          </div>
                          {category.attachments.map(attachment => (
                            <div key={attachment.id} className="attachment-row">
                              <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                                {attachment.name}
                              </a>
                              <span>{attachment.mimeType}</span>
                              <span>{attachment.sizeLabel}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {category.editPath && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={category.editPath}>
                  Edit
                </SmartLink>
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

const REPORT_STATUS_COLORS = {
  Open: '#4285f4',
  Assigned: '#fbbc04',
  Closed: '#34a853',
  Resolved: '#34a853',
  'In Progress': '#ea4335'
};

const REPORT_DEFAULT_COLORS = ['#4285f4', '#ea4335', '#fbbc04', '#34a853', '#9334e6', '#ff6d01', '#46bdc6', '#7baaf7', '#f07b72', '#fdd663'];

function ReportChartCard({ chartKey, title, children, ...chartProps }) {
  return (
    <section className="detail-card report-chart-card">
      <div className="report-title-row">
        <h3>{title}</h3>
        {children}
      </div>
      <ReportChartCanvas chartKey={chartKey} {...chartProps} />
    </section>
  );
}

function ReportChartCanvas({
  chartKey,
  type,
  items,
  scriptReady,
  scriptError,
  onChartReady,
  colorMap,
  integerScale = false,
  fill = false
}) {
  const canvasRef = useRef(null);
  const chartRef = useRef(null);
  const normalizedItems = Array.isArray(items) ? items : [];

  useEffect(() => {
    if (!scriptReady || !canvasRef.current || normalizedItems.length === 0 || !window.Chart) {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
      onChartReady?.(chartKey, null);
      return undefined;
    }

    if (chartRef.current) {
      chartRef.current.destroy();
    }

    const labels = normalizedItems.map(item => item.label);
    const values = normalizedItems.map(item => item.value);
    const options = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: type === 'pie', position: 'bottom' }
      }
    };
    if (type !== 'pie') {
      options.scales = {
        y: {
          beginAtZero: true,
          ticks: integerScale ? { stepSize: 1 } : undefined
        }
      };
    }

    chartRef.current = new window.Chart(canvasRef.current, {
      type,
      data: {
        labels,
        datasets: [
          {
            label: type === 'line' ? 'Tickets created' : type === 'pie' ? undefined : 'Tickets',
            data: values,
            backgroundColor: reportColorsForLabels(labels, colorMap),
            borderColor: type === 'line' ? '#b00020' : undefined,
            borderWidth: type === 'line' ? 2 : 0,
            fill,
            tension: type === 'line' ? 0.3 : undefined,
            pointRadius: type === 'line' ? 4 : undefined
          }
        ]
      },
      options
    });
    onChartReady?.(chartKey, chartRef.current);

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
      onChartReady?.(chartKey, null);
    };
  }, [chartKey, colorMap, fill, integerScale, normalizedItems, onChartReady, scriptReady, type]);

  if (scriptError) {
    return <div className="report-no-data">Unable to load diagrams.</div>;
  }
  if (!scriptReady) {
    return <div className="report-no-data">Loading diagrams...</div>;
  }
  if (normalizedItems.length === 0) {
    return <div className="report-no-data">No data available</div>;
  }
  return (
    <div className="report-chart-container">
      <canvas ref={canvasRef} />
    </div>
  );
}

function reportColorsForLabels(labels, colorMap) {
  return labels.map((label, index) => (colorMap && colorMap[label]) || REPORT_DEFAULT_COLORS[index % REPORT_DEFAULT_COLORS.length]);
}

function DataState({ state, emptyMessage, signInHref, children }) {
  if (state.loading) {
    return <p>Loading...</p>;
  }

  if (state.unauthorized) {
    return (
      <div className="empty-state">
        <p>You need to sign in to view this area.</p>
        <a className="primary-button" href={signInHref}>
          Sign in
        </a>
      </div>
    );
  }

  if (state.forbidden) {
    return <p className="error-text">You do not have access to this area.</p>;
  }

  if (state.error) {
    return <p className="error-text">{state.error}</p>;
  }

  if (state.empty) {
    return <p className="muted-text">{emptyMessage}</p>;
  }

  return children;
}

function SmartLink({ href, className, children, onClick }) {
  const normalizedHref = normalizeClientPath(href);
  if (isClientRoute(normalizedHref)) {
    return (
      <Link className={className} to={normalizedHref} onClick={onClick}>
        {children}
      </Link>
    );
  }

  return (
    <a className={className} href={normalizedHref} onClick={onClick}>
      {children}
    </a>
  );
}

function useJson(url) {
  const [state, setState] = useState({
    loading: Boolean(url),
    error: '',
    unauthorized: false,
    forbidden: false,
    empty: false,
    data: null
  });

  useEffect(() => {
    if (!url) {
      setState({ loading: false, error: '', unauthorized: false, forbidden: false, empty: true, data: null });
      return undefined;
    }

    let active = true;
    setState(current => ({ ...current, loading: true, error: '', unauthorized: false, forbidden: false, empty: false }));

    fetch(url, { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
        if (response.status === 401) {
          return { unauthorized: true };
        }
        if (response.status === 403) {
          return { forbidden: true };
        }
        if (!response.ok) {
          throw new Error(`Unable to load ${url}`);
        }
        const data = await response.json();
        return { data };
      })
      .then(result => {
        if (!active) {
          return;
        }
        if (result.unauthorized) {
          setState({ loading: false, error: '', unauthorized: true, forbidden: false, empty: false, data: null });
          return;
        }
        if (result.forbidden) {
          setState({ loading: false, error: '', unauthorized: false, forbidden: true, empty: false, data: null });
          return;
        }
        const isEmptyList = Array.isArray(result.data?.items) && result.data.items.length === 0;
        setState({
          loading: false,
          error: '',
          unauthorized: false,
          forbidden: false,
          empty: isEmptyList,
          data: result.data
        });
      })
      .catch(error => {
        if (active) {
          setState({
            loading: false,
            error: error.message || 'Unable to load data',
            unauthorized: false,
            forbidden: false,
            empty: false,
            data: null
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  return state;
}

function useText(url) {
  const [state, setState] = useState({
    loading: Boolean(url),
    error: '',
    data: ''
  });

  useEffect(() => {
    if (!url) {
      setState({ loading: false, error: '', data: '' });
      return undefined;
    }

    let active = true;
    setState(current => ({ ...current, loading: true, error: '' }));

    fetch(url, { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
        if (!response.ok) {
          throw new Error(`Unable to load ${url}`);
        }
        return response.text();
      })
      .then(data => {
        if (active) {
          setState({ loading: false, error: '', data });
        }
      })
      .catch(error => {
        if (active) {
          setState({
            loading: false,
            error: error.message || 'Unable to load data',
            data: ''
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  return state;
}

function useExternalScript(src) {
  const [state, setState] = useState({
    loaded: false,
    error: ''
  });

  useEffect(() => {
    if (!src) {
      setState({ loaded: false, error: '' });
      return undefined;
    }
    if (document.querySelector(`script[src="${src}"]`) && window.Chart) {
      setState({ loaded: true, error: '' });
      return undefined;
    }

    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      const onLoad = () => setState({ loaded: true, error: '' });
      const onError = () => setState({ loaded: false, error: `Unable to load ${src}` });
      existing.addEventListener('load', onLoad);
      existing.addEventListener('error', onError);
      return () => {
        existing.removeEventListener('load', onLoad);
        existing.removeEventListener('error', onError);
      };
    }

    const script = document.createElement('script');
    script.src = src;
    script.async = true;
    const onLoad = () => setState({ loaded: true, error: '' });
    const onError = () => setState({ loaded: false, error: `Unable to load ${src}` });
    script.addEventListener('load', onLoad);
    script.addEventListener('error', onError);
    document.body.appendChild(script);

    return () => {
      script.removeEventListener('load', onLoad);
      script.removeEventListener('error', onError);
    };
  }, [src]);

  return state;
}

function headerNavigation(session) {
  const navigation = Array.isArray(session?.navigation) ? session.navigation : [];
  const role = session?.role;

  if (role === 'admin') {
    return [];
  }

  if (role === 'support') {
    return filterNavigation(navigation, ['Tickets', 'Articles', 'Users']);
  }

  if (role === 'superuser' || role === 'tam') {
    return filterNavigation(navigation, ['Tickets', 'Articles', 'Reports']);
  }

  if (role === 'user') {
    return filterNavigation(navigation, ['Tickets', 'Articles']);
  }

  return navigation.filter(link => link.label !== 'Profile');
}

function filterNavigation(navigation, labels) {
  const allowed = new Set(labels);
  const filtered = navigation.filter(link => allowed.has(link.label));
  return labels.flatMap(label => filtered.find(link => link.label === label) || []);
}

function ticketCountsApiPath(role) {
  if (role === 'support') {
    return '/api/support/tickets';
  }
  if (role === 'superuser') {
    return '/api/superuser/tickets';
  }
  if (role === 'tam' || role === 'user') {
    return '/api/user/tickets';
  }
  return '';
}

async function fetchJson(url) {
  const response = await fetch(url, { credentials: 'same-origin', cache: 'no-store' });
  if (response.status === 401) {
    throw new Error('You need to sign in again.');
  }
  if (response.status === 403) {
    throw new Error('You do not have access to this page.');
  }
  if (!response.ok) {
    throw new Error((await response.text()) || `Unable to load ${url}`);
  }
  return response.json();
}

function ticketLabelForRole(role, assignedCount, openCount) {
  if (role === 'support' || role === 'superuser' || role === 'tam' || role === 'user') {
    return `Tickets (${assignedCount}/${openCount})`;
  }
  return 'Tickets';
}

function isRoleTicketRoute(role, pathname) {
  if (role === 'support') {
    return pathname.startsWith('/support/tickets');
  }
  if (role === 'superuser') {
    return pathname.startsWith('/superuser/tickets');
  }
  if (role === 'tam' || role === 'user') {
    return pathname.startsWith('/user/tickets');
  }
  return pathname === '/tickets';
}

function showRoleTicketAlarm(role) {
  return role === 'support' || role === 'superuser' || role === 'tam' || role === 'user';
}

function rssPath(role) {
  if (role === 'support') {
    return '/rss/support';
  }
  if (role === 'superuser') {
    return '/rss/superuser';
  }
  if (role === 'tam') {
    return '/rss/tam';
  }
  return '';
}

async function postForm(url, entries) {
  const body = new URLSearchParams();
  entries.forEach(([key, value]) => appendFormValue(body, key, value));

  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body: body.toString()
  });

  if (response.status === 401) {
    throw new Error('You need to sign in again.');
  }
  if (response.status === 403) {
    throw new Error('You do not have access to this action.');
  }
  if (!response.ok) {
    throw new Error((await response.text()) || 'Unable to save.');
  }

  return response;
}

async function postMultipart(url, entries, options = {}) {
  const body = new FormData();
  entries.forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach(item => body.append(key, item));
      return;
    }
    if (value !== undefined && value !== null) {
      body.append(key, value);
    }
  });

  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    headers: options.headers,
    body
  });

  if (response.status === 401) {
    throw new Error('You need to sign in again.');
  }
  if (response.status === 403) {
    throw new Error('You do not have access to this action.');
  }
  if (!response.ok) {
    throw new Error((await response.text()) || 'Unable to save.');
  }

  return response;
}

function createDirectoryUserFormState(bootstrap) {
  return {
    id: bootstrap?.user?.id ? String(bootstrap.user.id) : '',
    name: bootstrap?.user?.name || '',
    fullName: bootstrap?.user?.fullName || '',
    email: bootstrap?.user?.email || '',
    social: bootstrap?.user?.social || '',
    phoneNumber: bootstrap?.user?.phoneNumber || '',
    phoneExtension: bootstrap?.user?.phoneExtension || '',
    countryId: bootstrap?.user?.countryId ? String(bootstrap.user.countryId) : '',
    timezoneId: bootstrap?.user?.timezoneId ? String(bootstrap.user.timezoneId) : '',
    type: bootstrap?.user?.type || bootstrap?.types?.[0]?.value || '',
    companyId: bootstrap?.user?.companyId ? String(bootstrap.user.companyId) : bootstrap?.selectedCompanyId ? String(bootstrap.selectedCompanyId) : '',
    password: ''
  };
}

function resolveClientPath(path, fallback) {
  if (!path) {
    return fallback;
  }
  return normalizeClientPath(path);
}

function resolveRedirectPath(response, fallback) {
  if (!response?.redirected || !response.url) {
    return fallback;
  }
  try {
    const redirectUrl = new URL(response.url, window.location.origin);
    if (redirectUrl.origin !== window.location.origin) {
      return fallback;
    }
    return resolveClientPath(`${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`, fallback);
  } catch {
    return fallback;
  }
}

async function resolvePostRedirectPath(response, fallback) {
  const contentType = response?.headers?.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const payload = await response.json();
    return resolveClientPath(payload?.redirectTo, fallback);
  }
  return resolveRedirectPath(response, fallback);
}

function formatPhone(phoneNumber, extension) {
  if (!phoneNumber && !extension) {
    return '—';
  }
  if (!extension) {
    return phoneNumber;
  }
  return `${phoneNumber || ''} ext. ${extension}`.trim();
}

function appendFormValue(searchParams, key, value) {
  if (Array.isArray(value)) {
    value.forEach(item => appendFormValue(searchParams, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  searchParams.append(key, String(value));
}

function isNetworkRequestError(error) {
  const message = error?.message || '';
  return error instanceof TypeError || /networkerror|failed to fetch|load failed/i.test(message);
}

function submitBrowserForm(url, entries) {
  const form = document.createElement('form');
  form.method = 'post';
  form.action = url;
  form.style.display = 'none';
  entries.forEach(([key, value]) => appendBrowserFormValue(form, key, value));
  document.body.appendChild(form);
  form.submit();
}

function appendBrowserFormValue(form, key, value) {
  if (Array.isArray(value)) {
    value.forEach(item => appendBrowserFormValue(form, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  const input = document.createElement('input');
  input.type = 'hidden';
  input.name = key;
  input.value = String(value);
  form.appendChild(input);
}

function toQueryString(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value);
    }
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

function normalizeClientPath(path) {
  if (typeof path !== 'string' || path.length === 0) {
    return path;
  }
  if (path.startsWith('/app/#')) {
    const cleanPath = path.slice('/app/#'.length);
    return cleanPath || '/';
  }
  if (path === '/app' || path === '/app/') {
    return '/';
  }
  if (path.startsWith('/app/')) {
    const cleanPath = path.slice('/app'.length);
    return cleanPath || '/';
  }
  return path;
}

function isClientRoute(href) {
  if (typeof href !== 'string' || !href.startsWith('/')) {
    return false;
  }
  if (href.startsWith('/api/') || href === '/logout' || href.startsWith('/tickets/export/')) {
    return false;
  }
  if (href.startsWith('/attachments/')) {
    return !href.includes('/data');
  }
  return true;
}

function orderedNavigation(navigation, labels) {
  if (!Array.isArray(navigation)) {
    return [];
  }
  const linksByLabel = new Map(navigation.map(link => [link.label, link]));
  return labels.map(label => linksByLabel.get(label)).filter(Boolean);
}

function durationLabel(value) {
  return String(value) === '1' ? 'Monthly' : 'Yearly';
}

export default App;
