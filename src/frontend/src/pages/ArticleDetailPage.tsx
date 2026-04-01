/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import MarkdownContent from '../components/markdown/MarkdownContent';
import { resolvePostRedirectPath, SmartLink } from '../utils/routing';
import { postForm } from '../utils/api';
import type { SessionPageProps } from '../types/app';
import type { ArticleRecord } from '../types/domain';

interface DeleteArticleButtonProps {
  articleId: string | number;
  label?: string;
}

function DeleteArticleButton({ articleId, label = 'Delete article' }: DeleteArticleButtonProps) {
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
      const response = await postForm(`/articles/${articleId}/delete`, []);
      navigate(await resolvePostRedirectPath(response, '/articles'));
    } catch (submitError: unknown) {
      setDeleting(false);
      setError(submitError instanceof Error ? submitError.message : 'Unable to delete article.');
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

export { DeleteArticleButton };

export default function ArticleDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const articleState = useJson<ArticleRecord>(id ? `/api/articles/${id}` : null);
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

