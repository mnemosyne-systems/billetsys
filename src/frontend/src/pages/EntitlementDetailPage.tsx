import { useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import MarkdownContent from '../components/markdown/MarkdownContent';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { EntitlementRecord, LevelRecord, VersionInfo } from '../types/domain';

export default function EntitlementDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const entitlementState = useJson<EntitlementRecord>(id ? `/api/entitlements/${id}` : null);
  const entitlement = entitlementState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{entitlement?.name || 'Entitlement details'}</h2>
        </div>
      </div>

      <DataState state={entitlementState} emptyMessage="Entitlement not found." signInHref={sessionState.data?.homePath || '/login'}>
        {entitlement && (
          <div className="article-detail">
            <div className="admin-detail-layout">
              <section className="detail-section">
                <div className="markdown-card">
                  {entitlement.description ? <MarkdownContent>{entitlement.description}</MarkdownContent> : <p className="muted-text">No description.</p>}
                </div>

                <div className="detail-card">
                  <h3>Versions</h3>
                  <div className="version-list">
                    {(entitlement.versions || []).map((version: VersionInfo) => (
                      <div key={version.id || `${version.name}-${version.date}`} className="version-row">
                        <strong>{version.name}</strong>
                        <span>{version.date || 'No date'}</span>
                      </div>
                    ))}
                    {(!entitlement.versions || entitlement.versions.length === 0) && <p className="muted-text">No versions.</p>}
                  </div>
                </div>
              </section>

              <section className="detail-section">
                <div className="detail-card">
                  <h3>Support levels</h3>
                  <div className="checkbox-list">
                    {(entitlement.supportLevels || []).map((level: LevelRecord) => (
                      <div key={level.id} className="checkbox-card">
                        <span>
                          <strong>{level.name}</strong>
                          <small>
                            {level.fromLabel} - {level.toLabel}
                          </small>
                        </span>
                      </div>
                    ))}
                    {(!entitlement.supportLevels || entitlement.supportLevels.length === 0) && (
                      <p className="muted-text">No support levels.</p>
                    )}
                  </div>
                </div>
              </section>
            </div>

            {entitlement.editPath && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={entitlement.editPath}>
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
