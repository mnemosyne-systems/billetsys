import { useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CategoryRecord } from '../types/domain';

export default function CategoryDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const categoryState = useJson<CategoryRecord>(id ? `/api/categories/${id}` : null);
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
