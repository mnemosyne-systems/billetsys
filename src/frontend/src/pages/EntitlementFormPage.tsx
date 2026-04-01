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
import MarkdownEditor from '../components/markdown/MarkdownEditor';
import useJson from '../hooks/useJson';
import useSubmissionGuard from '../hooks/useSubmissionGuard';
import DataState from '../components/common/DataState';
import { postForm } from '../utils/api';
import { resolvePostRedirectPath } from '../utils/routing';
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
  const submissionGuard = useSubmissionGuard();
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
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
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
      const response = await postForm(isEdit ? `/entitlements/${id}` : '/entitlements', entries);
      navigate(await resolvePostRedirectPath(response, '/entitlements'));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save entitlement.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteEntitlement = async () => {
    if (!id || !window.confirm('Delete this entitlement?') || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postForm(`/entitlements/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, '/entitlements'));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete entitlement.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <DataState state={entitlementState} emptyMessage="Entitlement not found." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && entitlement && (
          <div className="form-card ticket-detail-card">
            <form className="owner-form" onSubmit={submit}>
              <div className="owner-form-grid ticket-detail-grid">
                <label className="form-span-2">
                  Name
                  <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
                </label>
                <label className="form-span-2">
                  Description
                  <MarkdownEditor value={formState.description} onChange={value => updateFormState('description', value)} rows={10} required />
                </label>
              </div>

              <section className="detail-card">
                <h3>Support levels</h3>
                <div className="entitlement-support-level-list">
                  {(entitlement.supportLevels || []).map((level: LevelRecord) => (
                    <label key={level.id} className="entitlement-support-level-row">
                      <span className="entitlement-support-level-name">{level.name}</span>
                      <span className="entitlement-support-level-window">
                        {level.fromLabel} - {level.toLabel}
                      </span>
                      <input
                        type="checkbox"
                        checked={formState.selectedLevelIds.includes(level.id)}
                        onChange={() => toggleLevel(level.id)}
                      />
                    </label>
                  ))}
                </div>
              </section>

              <section className="detail-card">
                <div className="section-header compact-header">
                  <div>
                    <h3>Versions</h3>
                  </div>
                  <button type="button" className="primary-button" onClick={addVersion}>
                    Add
                  </button>
                </div>
                <div className="version-editor-list">
                  {formState.versions.map((version, index) => (
                    <div key={`${version.id || 'new'}-${index}`} className="version-editor-card entitlement-version-card">
                      <div className="entitlement-version-grid">
                        <label>
                          Version
                          <input value={version.name} onChange={event => updateVersion(index, 'name', event.target.value)} required />
                        </label>
                        <label>
                          Date
                          <input type="date" value={version.date} onChange={event => updateVersion(index, 'date', event.target.value)} required />
                        </label>
                        <div className="button-row button-row-end entitlement-version-actions">
                          <button
                            type="button"
                            className="secondary-button danger-button"
                            onClick={() => removeVersion(index)}
                            disabled={formState.versions.length === 1}
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </section>

              {saveState.error && <p className="error-text">{saveState.error}</p>}

              <div className="button-row button-row-end">
                {isEdit && (
                  <button type="button" className="secondary-button danger-button" onClick={deleteEntitlement} disabled={saveState.saving}>
                    Delete
                  </button>
                )}
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
