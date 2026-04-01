/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import useSubmissionGuard from '../hooks/useSubmissionGuard';
import DataState from '../components/common/DataState';
import { SmartLink, resolvePostRedirectPath } from '../utils/routing';
import { postForm } from '../utils/api';
import { isNetworkRequestError, submitBrowserForm } from '../utils/forms';
import { SelectableUserPicker, SelectableUserSummary } from '../components/users/UserComponents';
import type { FormMode, SessionPageProps } from '../types/app';
import type { CompanyFormBootstrap } from '../types/domain';
import type { CompanyEntitlementEntry, CompanyFormState } from '../types/forms';
import type { BrowserFormEntries } from '../utils/forms';

interface CompanyFormPageProps extends SessionPageProps {
  mode: FormMode;
}

const EMPTY_COMPANY_FORM_STATE: CompanyFormState = {
  name: '',
  address1: '',
  address2: '',
  city: '',
  state: '',
  zip: '',
  phoneNumber: '',
  countryId: '',
  timezoneId: '',
  selectedUserIds: [],
  selectedTamIds: [],
  entitlements: [],
  superuserId: '',
  superuserUsername: '',
  superuserFullName: '',
  superuserEmail: '',
  superuserSocial: '',
  superuserPhoneNumber: '',
  superuserPhoneExtension: '',
  superuserCountryId: '',
  superuserTimezoneId: '',
  superuserPassword: ''
};

export default function CompanyFormPage({ sessionState, mode }: CompanyFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const companyState = useJson<CompanyFormBootstrap>(mode === 'edit' && id ? `/api/companies/${id}` : '/api/companies/bootstrap');
  const company = companyState.data;
  const [formState, setFormState] = useState<CompanyFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (!company) {
      return;
    }
    if (isEdit) {
      setFormState({
        ...EMPTY_COMPANY_FORM_STATE,
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
        superuserId: company.superuserId ? String(company.superuserId) : ''
      });
      return;
    }
    setFormState({
      ...EMPTY_COMPANY_FORM_STATE,
      countryId: company.defaultCountryId ? String(company.defaultCountryId) : '',
      timezoneId: company.defaultTimezoneId ? String(company.defaultTimezoneId) : '',
      superuserCountryId: company.defaultCountryId ? String(company.defaultCountryId) : '',
      superuserTimezoneId: company.defaultTimezoneId ? String(company.defaultTimezoneId) : ''
    });
  }, [company, isEdit]);

  const availableTimezones =
    company?.timezones?.filter(timezone => !formState?.countryId || String(timezone.countryId) === formState.countryId) || [];
  const availableSuperuserTimezones =
    company?.timezones?.filter(
      timezone => !formState?.superuserCountryId || String(timezone.countryId) === formState.superuserCountryId
    ) || [];

  const updateFormState = <K extends keyof CompanyFormState>(field: K, value: CompanyFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const toggleSelection = (field: 'selectedUserIds' | 'selectedTamIds', idToToggle: string | number) => {
    setFormState(current =>
      current
        ? {
            ...current,
            [field]: current[field].includes(idToToggle)
              ? current[field].filter(existing => existing !== idToToggle)
              : [...current[field], idToToggle]
          }
        : current
    );
  };

  const updateEntitlement = (index: number, field: keyof CompanyEntitlementEntry, value: string) => {
    setFormState(current =>
      current
        ? {
            ...current,
            entitlements: current.entitlements.map((entry, entryIndex) =>
              entryIndex === index ? { ...entry, [field]: value } : entry
            )
          }
        : current
    );
  };

  const addEntitlement = () => {
    setFormState(current =>
      current
        ? {
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
          }
        : current
    );
  };

  const removeEntitlement = (index: number) => {
    setFormState(current =>
      current
        ? {
            ...current,
            entitlements: current.entitlements.filter((_, entryIndex) => entryIndex !== index)
          }
        : current
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    const entries: BrowserFormEntries = [
      ['name', formState.name],
      ['address1', formState.address1],
      ['address2', formState.address2],
      ['city', formState.city],
      ['state', formState.state],
      ['zip', formState.zip],
      ['phoneNumber', formState.phoneNumber],
      ['countryId', formState.countryId],
      ['timezoneId', formState.timezoneId],
      ...formState.selectedUserIds.map((userId): [string, string] => ['userIds', String(userId)]),
      ...formState.selectedTamIds.map((userId): [string, string] => ['tamIds', String(userId)]),
      ...formState.entitlements.flatMap((entry): BrowserFormEntries => [
        ['entitlementIds', entry.entitlementId],
        ['levelIds', entry.levelId],
        ['entitlementDates', entry.date],
        ['entitlementDurations', entry.duration]
      ])
    ];
    try {
      setSaveState({ saving: true, error: '' });
      if (isEdit) {
        entries.push(['superuserId', formState.superuserId]);
      } else {
        entries.push(
          ['superuserUsername', formState.superuserUsername],
          ['superuserFullName', formState.superuserFullName],
          ['superuserEmail', formState.superuserEmail],
          ['superuserSocial', formState.superuserSocial],
          ['superuserPhoneNumber', formState.superuserPhoneNumber],
          ['superuserPhoneExtension', formState.superuserPhoneExtension],
          ['superuserCountryId', formState.superuserCountryId],
          ['superuserTimezoneId', formState.superuserTimezoneId],
          ['superuserPassword', formState.superuserPassword]
        );
      }

      const response = await postForm(isEdit ? `/companies/${id}` : '/companies', entries);
      navigate(await resolvePostRedirectPath(response, '/companies'));
    } catch (error: unknown) {
      if (isNetworkRequestError(error)) {
        setSaveState({ saving: false, error: '' });
        submissionGuard.exit();
        submitBrowserForm(isEdit ? `/companies/${id}` : '/companies', entries);
        return;
      }
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save company.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteCompany = async () => {
    if (!id || !window.confirm('Delete this company?') || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postForm(`/companies/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, '/companies'));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete company.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <DataState state={companyState} emptyMessage="Company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && company && (
          <form className={`owner-form${!isEdit ? ' owner-detail-form' : ''}`} onSubmit={submit}>
            {isEdit ? (
              <div className="form-card ticket-detail-card">
                <div className="owner-form owner-detail-form">
                  <div className="owner-form-grid ticket-detail-grid">
                    <label>
                      Name
                      <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
                    </label>
                    <label>
                      Phone
                      <input value={formState.phoneNumber} onChange={event => updateFormState('phoneNumber', event.target.value)} />
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
                          setFormState(current =>
                            current
                              ? {
                                  ...current,
                                  countryId: nextCountryId,
                                  timezoneId: timezoneStillValid ? current.timezoneId : ''
                                }
                              : current
                          );
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
                      <select value={formState.timezoneId} onChange={event => updateFormState('timezoneId', event.target.value)}>
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
                      <input value={formState.address1} onChange={event => updateFormState('address1', event.target.value)} />
                    </label>
                    <label>
                      Address2
                      <input value={formState.address2} onChange={event => updateFormState('address2', event.target.value)} />
                    </label>
                    <label>
                      City
                      <input value={formState.city} onChange={event => updateFormState('city', event.target.value)} />
                    </label>
                    <label>
                      State
                      <input value={formState.state} onChange={event => updateFormState('state', event.target.value)} />
                    </label>
                    <label>
                      Zip
                      <input value={formState.zip} onChange={event => updateFormState('zip', event.target.value)} />
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
              <div className="form-card ticket-detail-card">
                <div className="owner-form owner-detail-form">
                  <p className="required-fields-note">
                    Required fields are marked <span className="required-indicator">*</span>.
                  </p>
                  <div className="owner-form-grid ticket-detail-grid">
                    <label>
                      Name <span className="required-indicator">*</span>
                      <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
                    </label>
                    <label>
                      Phone number
                      <input value={formState.phoneNumber} onChange={event => updateFormState('phoneNumber', event.target.value)} />
                    </label>
                    <label>
                      Address 1
                      <input value={formState.address1} onChange={event => updateFormState('address1', event.target.value)} />
                    </label>
                    <label>
                      Address 2
                      <input value={formState.address2} onChange={event => updateFormState('address2', event.target.value)} />
                    </label>
                    <label>
                      City
                      <input value={formState.city} onChange={event => updateFormState('city', event.target.value)} />
                    </label>
                    <label>
                      State
                      <input value={formState.state} onChange={event => updateFormState('state', event.target.value)} />
                    </label>
                    <label>
                      Zip
                      <input value={formState.zip} onChange={event => updateFormState('zip', event.target.value)} />
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
                          setFormState(current =>
                            current
                              ? {
                                  ...current,
                                  countryId: nextCountryId,
                                  timezoneId: timezoneStillValid ? current.timezoneId : ''
                                }
                              : current
                          );
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
                      <select value={formState.timezoneId} onChange={event => updateFormState('timezoneId', event.target.value)}>
                        <option value="">Select a time zone</option>
                        {availableTimezones.map(timezone => (
                          <option key={timezone.id} value={timezone.id}>
                            {timezone.name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <div className="detail-card-spacer" aria-hidden="true" />
                    <SelectableUserPicker
                      title="Users"
                      users={company.userOptions || []}
                      selectedIds={formState.selectedUserIds}
                      onToggle={userId => toggleSelection('selectedUserIds', userId)}
                    />
                    <section className="detail-card">
                      <h3>Superuser</h3>
                      <div className="owner-form-grid">
                        <label>
                          Username <span className="required-indicator">*</span>
                          <input value={formState.superuserUsername} onChange={event => updateFormState('superuserUsername', event.target.value)} required />
                        </label>
                        <label>
                          Full name
                          <input value={formState.superuserFullName} onChange={event => updateFormState('superuserFullName', event.target.value)} />
                        </label>
                        <label>
                          Email <span className="required-indicator">*</span>
                          <input type="email" value={formState.superuserEmail} onChange={event => updateFormState('superuserEmail', event.target.value)} required />
                        </label>
                        <label>
                          Social
                          <input value={formState.superuserSocial} onChange={event => updateFormState('superuserSocial', event.target.value)} />
                        </label>
                        <label>
                          Phone number
                          <input value={formState.superuserPhoneNumber} onChange={event => updateFormState('superuserPhoneNumber', event.target.value)} />
                        </label>
                        <label>
                          Phone extension
                          <input value={formState.superuserPhoneExtension} onChange={event => updateFormState('superuserPhoneExtension', event.target.value)} />
                        </label>
                        <label>
                          Country
                          <select
                            value={formState.superuserCountryId}
                            onChange={event => {
                              const nextCountryId = event.target.value;
                              const timezoneStillValid = (company.timezones || []).some(
                                timezone =>
                                  String(timezone.id) === formState.superuserTimezoneId &&
                                  String(timezone.countryId) === nextCountryId
                              );
                              setFormState(current =>
                                current
                                  ? {
                                      ...current,
                                      superuserCountryId: nextCountryId,
                                      superuserTimezoneId: timezoneStillValid ? current.superuserTimezoneId : ''
                                    }
                                  : current
                              );
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
                            value={formState.superuserTimezoneId}
                            onChange={event => updateFormState('superuserTimezoneId', event.target.value)}
                          >
                            <option value="">Select a time zone</option>
                            {availableSuperuserTimezones.map(timezone => (
                              <option key={timezone.id} value={timezone.id}>
                                {timezone.name}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label className="form-span-2">
                          Password <span className="required-indicator">*</span>
                          <input
                            type="password"
                            value={formState.superuserPassword}
                            onChange={event => updateFormState('superuserPassword', event.target.value)}
                            required
                          />
                        </label>
                      </div>
                    </section>
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
            )}

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className={`button-row${isEdit ? ' button-row-split' : ' button-row-end'}`}>
              {isEdit ? (
                <button type="button" className="secondary-button danger-button" onClick={deleteCompany} disabled={saveState.saving}>
                  Delete
                </button>
              ) : null}
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : isEdit ? 'Save' : 'Create'}
              </button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
