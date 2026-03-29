import type { FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import MarkdownEditor from '../components/markdown/MarkdownEditor';
import AttachmentPicker from '../components/common/AttachmentPicker';
import { postMultipart } from '../utils/api';
import { DeleteArticleButton } from './ArticleDetailPage';
import type { FormMode, SessionPageProps } from '../types/app';
import type { ArticleRecord } from '../types/domain';

interface ArticleFormState {
  title: string;
  tags: string;
  body: string;
}

interface ArticleFormPageProps extends SessionPageProps {
  mode: FormMode;
}

export default function ArticleFormPage({ sessionState, mode }: ArticleFormPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const articleState = useJson<ArticleRecord>(mode === 'edit' && id ? `/api/articles/${id}` : '/api/articles/bootstrap');
  const article = articleState.data;
  const [formState, setFormState] = useState<ArticleFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const [files, setFiles] = useState<File[]>([]);
  const bodyInputRef = useRef<HTMLTextAreaElement | null>(null);
  const isEdit = mode === 'edit';

  const updateFormState = (field: keyof ArticleFormState, value: string) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

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

  const submit = async (event: FormEvent<HTMLFormElement>) => {
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
        ...files.map((file): ['attachments', File] => ['attachments', file])
      ]);
      navigate(isEdit && id ? `/articles/${id}` : '/articles');
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save article.' });
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
                  <input value={formState.title} onChange={event => updateFormState('title', event.target.value)} required />
                </label>
                <label>
                  Tags
                  <input value={formState.tags} onChange={event => updateFormState('tags', event.target.value)} />
                </label>
                <label className="form-span-2">
                  Body
                  <MarkdownEditor
                    value={formState.body}
                    onChange={value => updateFormState('body', value)}
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
