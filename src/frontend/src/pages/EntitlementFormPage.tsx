import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import { postForm } from '../utils/api';
import type { FormMode, SessionPageProps } from '../types/app';
import type { EntitlementRecord, LevelRecord } from '../types/domain';
import type { FormEntries } from '../utils/api';

interface EntitlementVersionForm {
  id: string;
  name: string;
  date: string;
}

interface EntitlementFormState {
  name: string;
  description: string;
  selectedLevelIds: Array<string | number>;
  versions: EntitlementVersionForm[];
}

interface EntitlementFormPageProps extends SessionPageProps {
  mode: FormMode;
}

interface EntitlementFormBootstrap extends EntitlementRecord {
  selectedLevelIds?: Array<string | number>;
  todayDate?: string;
}

export default function EntitlementFormPage({ sessionState, mode }: EntitlementFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const entitlementState = useJson<EntitlementFormBootstrap>(mode === 'edit' && id ? `/api/entitlements/${id}` : '/api/entitlements/bootstrap');
  const entitlement = entitlementState.data;
  const [formState, setFormState] = useState<EntitlementFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const isEdit = mode === 'edit';

  const updateFormState = <K extends keyof EntitlementFormState>(field: K, value: EntitlementFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

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

  const toggleLevel = (levelId: string | number) => {
    setFormState(current =>
      current
        ? {
            ...current,
            selectedLevelIds: current.selectedLevelIds.includes(levelId)
              ? current.selectedLevelIds.filter(existing => existing !== levelId)
              : [...current.selectedLevelIds, levelId]
          }
        : current
    );
  };

  const updateVersion = (index: number, field: keyof EntitlementVersionForm, value: string) => {
    setFormState(current =>
      current
        ? {
            ...current,
            versions: current.versions.map((version, versionIndex) =>
              versionIndex === index ? { ...version, [field]: value } : version
            )
          }
        : current
    );
  };

  const addVersion = () => {
    setFormState(current =>
      current
        ? {
            ...current,
            versions: [...current.versions, { id: '', name: '', date: entitlement?.todayDate || '' }]
          }
        : current
    );
  };

  const removeVersion = (index: number) => {
    setFormState(current =>
      current
        ? {
            ...current,
            versions: current.versions.filter((_, versionIndex) => versionIndex !== index)
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
      const entries: FormEntries = [
        ['name', formState.name],
        ['description', formState.description],
        ...formState.selectedLevelIds.map((levelId): [string, string] => ['levelIds', String(levelId)]),
        ...formState.versions.flatMap((version): [string, string][] => [
          ['versionIds', version.id || ''],
          ['versionNames', version.name],
          ['versionDates', version.date]
        ])
      ];
      await postForm(isEdit ? `/entitlements/${id}` : '/entitlements', entries);
      navigate('/entitlements');
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save entitlement.' });
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
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete entitlement.' });
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
                <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
              </label>
              <label className="form-span-2">
                Description
                <textarea
                  rows={6}
                  value={formState.description}
                  onChange={event => updateFormState('description', event.target.value)}
                  required
                />
              </label>
            </div>

            <section className="detail-card">
              <h3>Support levels</h3>
              <div className="checkbox-list">
                {(entitlement.supportLevels || []).map((level: LevelRecord) => (
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
