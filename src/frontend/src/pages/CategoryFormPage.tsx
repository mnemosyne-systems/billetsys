/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import useSubmissionGuard from '../hooks/useSubmissionGuard';
import DataState from '../components/common/DataState';
import MarkdownEditor from '../components/markdown/MarkdownEditor';
import { postForm, postMultipart } from '../utils/api';
import { resolvePostRedirectPath } from '../utils/routing';
import type { FormMode, SessionPageProps } from '../types/app';
import type { CategoryRecord } from '../types/domain';

interface CategoryFormState {
  name: string;
  description: string;
  isDefault: boolean;
}

interface CategoryFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function CategoryFormPage({ sessionState, mode }: CategoryFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const categoryState = useJson<CategoryRecord>(mode === 'edit' && id ? `/api/categories/${id}` : '/api/categories/bootstrap');
  const category = categoryState.data;
  const [formState, setFormState] = useState<CategoryFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const descriptionInputRef = useRef<HTMLTextAreaElement | null>(null);
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === 'edit';

  const updateFormState = <K extends keyof CategoryFormState>(field: K, value: CategoryFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

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

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postMultipart(isEdit ? `/categories/${id}` : '/categories', [
        ['name', formState.name],
        ['description', formState.description],
        ['isDefault', String(formState.isDefault)]
      ]);
      navigate(await resolvePostRedirectPath(response, isEdit && id ? `/categories/${id}` : '/categories'));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save category.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteCategory = async () => {
    if (!id || !window.confirm('Delete this category?') || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const response = await postForm(`/categories/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, '/categories'));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete category.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <DataState state={categoryState} emptyMessage="Category unavailable." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && (
          <div className="form-card ticket-detail-card">
            <form className="owner-form" onSubmit={submit}>
              <div className="owner-form-grid ticket-detail-grid">
                <label>
                  Name
                  <input value={formState.name} onChange={event => updateFormState('name', event.target.value)} required />
                </label>
                <label>
                  Default
                  <select
                    value={String(formState.isDefault)}
                    onChange={event => updateFormState('isDefault', event.target.value === 'true')}
                  >
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                  </select>
                </label>
                <label className="form-span-2">
                  Description
                  <MarkdownEditor
                    value={formState.description}
                    onChange={value => updateFormState('description', value)}
                    inputRef={descriptionInputRef}
                    rows={10}
                  />
                </label>
              </div>
              {saveState.error && <p className="error-text">{saveState.error}</p>}

              <div className={`button-row${isEdit ? ' button-row-split' : ' button-row-end'}`}>
                {isEdit && (
                  <button type="button" className="secondary-button danger-button" onClick={deleteCategory} disabled={saveState.saving}>
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
