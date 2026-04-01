/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import { OwnerUserList, OwnerSelector } from '../components/users/UserComponents';
import type { SessionPageProps } from '../types/app';
import type { OwnerCompany } from '../types/domain';

interface OwnerFormState {
  name: string;
  address1: string;
  address2: string;
  city: string;
  state: string;
  zip: string;
  phoneNumber: string;
  countryId: string;
  timezoneId: string;
  supportIds: Array<string | number>;
  tamIds: Array<string | number>;
}

export function OwnerPage({ sessionState }: SessionPageProps) {
  const ownerState = useJson<OwnerCompany>('/api/owner');
  const owner = ownerState.data;

  return (
    <section className="panel">
      <DataState state={ownerState} emptyMessage="Owner company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {owner && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Owner
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

export function OwnerEditPage({ sessionState }: SessionPageProps) {
  const navigate = useNavigate();
  const ownerState = useJson<OwnerCompany>('/api/owner');
  const owner = ownerState.data;
  const [formState, setFormState] = useState<OwnerFormState | null>(null);
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

  const updateField = (field: keyof OwnerFormState, value: string) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const toggleSelectedUser = (field: 'supportIds' | 'tamIds', userId: string | number) => {
    setFormState(current =>
      current
        ? {
            ...current,
            [field]: current[field].includes(userId)
              ? current[field].filter(existing => existing !== userId)
              : [...current[field], userId]
          }
        : current
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
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
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save owner details.' });
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

export default OwnerPage;
