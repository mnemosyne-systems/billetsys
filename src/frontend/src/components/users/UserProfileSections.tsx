import type { ReactNode } from 'react';
import { formatPhone, profileInitial } from '../../utils/formatting';
import { SmartLink } from '../../utils/routing';
import type { UserReference } from '../../types/domain';

interface UserLogoPreviewProps {
  logoBase64?: string;
  fullName?: string;
  username?: string;
  email?: string;
}

interface UserDetailCardUser {
  username?: string;
  fullName?: string;
  email?: string;
  social?: string;
  phoneNumber?: string;
  phoneExtension?: string;
  type?: string;
  typeLabel?: string;
  countryName?: string;
  timezoneName?: string;
  companyName?: string;
  logoBase64?: string;
}

interface UserDetailCardProps {
  user: UserDetailCardUser;
  companyHref?: string;
  companyLabel?: string;
  actions?: ReactNode;
}

export function UserLogoPreview({ logoBase64, fullName, username, email }: UserLogoPreviewProps) {
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

export function UserDetailCard({ user, companyHref, companyLabel = 'Company', actions }: UserDetailCardProps) {
  const hasCompany = user.companyName || companyHref;

  return (
    <div className="article-detail">
      <div className="form-card ticket-detail-card">
        <div className="owner-form owner-detail-form">
          <div className="owner-form-grid ticket-detail-grid">
            <label>
              Username
              <input value={user.username || '—'} readOnly />
            </label>
            <label>
              Full name
              <input value={user.fullName || '—'} readOnly />
            </label>
            <label>
              Type
              <input value={user.typeLabel || user.type || 'User'} readOnly />
            </label>
            <label>
              Email
              <input value={user.email || '—'} readOnly />
            </label>
            <label>
              Social
              <input value={user.social || '—'} readOnly />
            </label>
            <label>
              Phone
              <input value={formatPhone(user.phoneNumber, user.phoneExtension)} readOnly />
            </label>
            <label>
              Country
              <input value={user.countryName || '—'} readOnly />
            </label>
            <label>
              Time zone
              <input value={user.timezoneName || '—'} readOnly />
            </label>
            <label>
              {companyLabel}
              {companyHref ? (
                <div className="readonly-link-field">
                  <input value={user.companyName || '—'} readOnly />
                  <SmartLink className="readonly-link-field-link" href={companyHref}>
                    {user.companyName || 'Open company'}
                  </SmartLink>
                </div>
              ) : (
                <input value={hasCompany ? user.companyName : '—'} readOnly />
              )}
            </label>
            <div className="owner-detail-panel">
              <div className="owner-detail-panel-label">Logo</div>
              <div className="owner-detail-panel-body profile-logo-panel">
                <div className="profile-logo-panel-content">
                  <UserLogoPreview
                    logoBase64={user.logoBase64}
                    fullName={user.fullName}
                    username={user.username}
                    email={user.email}
                  />
                </div>
              </div>
            </div>
            <div className="detail-card-spacer" aria-hidden="true" />
          </div>
        </div>
      </div>

      {actions ? <div className="button-row button-row-end admin-detail-actions">{actions}</div> : null}
    </div>
  );
}
