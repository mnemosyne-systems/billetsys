/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import DataState from '../components/common/DataState';
import useJson from '../hooks/useJson';
import useSubmissionGuard from '../hooks/useSubmissionGuard';
import { postForm } from '../utils/api';
import { createDirectoryUserFormState } from '../utils/forms';
import { toQueryString } from '../utils/formatting';
import { resolveClientPath, resolvePostRedirectPath, SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CountryOption, DirectoryUserBootstrap, NamedEntity, TimezoneOption } from '../types/domain';
import type { DirectoryUserFormState } from '../utils/forms';

interface DirectoryUserFormPageProps extends SessionPageProps {
  bootstrapBase: string;
  navigateFallback: string;
}

interface UserTypeOption {
  value?: string;
  label?: string;
}

export default function DirectoryUserFormPage({ sessionState, bootstrapBase, navigateFallback }: DirectoryUserFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get('companyId') || '';
  const isEdit = Boolean(id);
  const isAdminCreate = !isEdit && bootstrapBase === '/api/admin/users/bootstrap';
  const [formState, setFormState] = useState<DirectoryUserFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const submissionGuard = useSubmissionGuard();
  const selectedCountryId = formState?.countryId || '';
  const bootstrapState = useJson<DirectoryUserBootstrap>(
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

  const updateFormState = <K extends keyof DirectoryUserFormState>(field: K, value: DirectoryUserFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !bootstrap || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postForm(bootstrap.submitPath, [
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
      navigate(await resolvePostRedirectPath(response, resolveClientPath(bootstrap.cancelPath, navigateFallback)));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save user.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteUser = async () => {
    if (!id || !bootstrap?.submitPath?.startsWith('/user/') || !window.confirm('Delete this user?') || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postForm(`/user/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, resolveClientPath(bootstrap.cancelPath, navigateFallback)));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete user.' });
      return;
    } finally {
      submissionGuard.exit();
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
                    <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
                  </label>
                  <label>
                    Full name
                    <input value={formState.fullName} onChange={event => updateFormState('fullName', event.target.value)} />
                  </label>
                  <label>
                    Email
                    <input type="email" value={formState.email} onChange={event => updateFormState('email', event.target.value)} required />
                  </label>
                  <label>
                    Social
                    <input value={formState.social} onChange={event => updateFormState('social', event.target.value)} />
                  </label>
                  <label>
                    Phone number
                    <input value={formState.phoneNumber} onChange={event => updateFormState('phoneNumber', event.target.value)} />
                  </label>
                  <label>
                    Extension
                    <input value={formState.phoneExtension} onChange={event => updateFormState('phoneExtension', event.target.value)} />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={event =>
                        setFormState(current =>
                          current
                            ? {
                                ...current,
                                countryId: event.target.value,
                                timezoneId: ''
                              }
                            : current
                        )
                      }
                    >
                      <option value="">Select country</option>
                      {(bootstrap.countries || []).map((country: CountryOption) => (
                        <option key={country.id} value={country.id ? String(country.id) : ''}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select value={formState.timezoneId} onChange={event => updateFormState('timezoneId', event.target.value)}>
                      <option value="">Select time zone</option>
                      {(bootstrap.timezones || []).map((timezone: TimezoneOption) => (
                        <option key={timezone.id} value={timezone.id ? String(timezone.id) : ''}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Type
                    <select value={formState.type} onChange={event => updateFormState('type', event.target.value)} required>
                      {(bootstrap.types || []).map((type: UserTypeOption) => (
                        <option key={type.value} value={type.value}>
                          {type.label || type.value}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Company
                    <select
                      value={formState.companyId}
                      disabled={bootstrap.companyLocked}
                      onChange={event => updateFormState('companyId', event.target.value)}
                    >
                      {(bootstrap.companies || []).map((company: NamedEntity) => (
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
                      onChange={event => updateFormState('password', event.target.value)}
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

