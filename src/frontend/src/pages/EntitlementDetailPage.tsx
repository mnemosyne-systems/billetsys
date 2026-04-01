/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

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
      <DataState state={entitlementState} emptyMessage="Entitlement not found." signInHref={sessionState.data?.homePath || '/login'}>
        {entitlement && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label className="form-span-2">
                    Name
                    <input value={entitlement.name || '—'} readOnly />
                  </label>
                  <div className="detail-card form-span-2">
                    <h3>Description</h3>
                    <div className="markdown-card">
                      {entitlement.description ? <MarkdownContent>{entitlement.description}</MarkdownContent> : <p className="muted-text">No description.</p>}
                    </div>
                  </div>
                </div>

                <section className="detail-card">
                  <h3>Support levels</h3>
                  <div className="entitlement-support-level-list">
                    {(entitlement.supportLevels || []).map((level: LevelRecord) => (
                      <div key={level.id} className="entitlement-support-level-row">
                        <span className="entitlement-support-level-name">{level.name}</span>
                        <span className="entitlement-support-level-window">
                          {level.fromLabel} - {level.toLabel}
                        </span>
                      </div>
                    ))}
                    {(!entitlement.supportLevels || entitlement.supportLevels.length === 0) && (
                      <p className="muted-text">No support levels.</p>
                    )}
                  </div>
                </section>

                <section className="detail-card">
                  <h3>Versions</h3>
                  <div className="version-editor-list">
                    {(entitlement.versions || []).map((version: VersionInfo) => (
                      <div key={version.id || `${version.name}-${version.date}`} className="version-editor-card entitlement-version-card">
                        <div className="entitlement-version-grid">
                          <label>
                            Version
                            <input value={version.name || '—'} readOnly />
                          </label>
                          <label>
                            Date
                            <input value={version.date || '—'} readOnly />
                          </label>
                          <div className="button-row button-row-end entitlement-version-actions" />
                        </div>
                      </div>
                    ))}
                    {(!entitlement.versions || entitlement.versions.length === 0) && <p className="muted-text">No versions.</p>}
                  </div>
                </section>
              </div>
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
