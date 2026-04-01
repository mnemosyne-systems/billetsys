/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CollectionResponse, EntitlementRecord } from '../types/domain';

type SupportLevelListItem = string | { id?: string | number; name?: string };

function supportLevelLabel(level: SupportLevelListItem): string {
  return typeof level === 'string' ? level : level.name || 'Unnamed level';
}

function sortedSupportLevels(supportLevels?: Array<SupportLevelListItem>): SupportLevelListItem[] {
  return [...(supportLevels || [])].sort((left, right) =>
    supportLevelLabel(left).localeCompare(supportLevelLabel(right), undefined, { sensitivity: 'base' })
  );
}

export default function EntitlementsListPage({ sessionState }: SessionPageProps) {
  const entitlementsState = useJson<CollectionResponse<EntitlementRecord>>('/api/entitlements');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Entitlements</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href="/entitlements/new">
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={entitlementsState} emptyMessage="No entitlements are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {entitlementsState.data?.items.map((entitlement: EntitlementRecord) => (
            <article key={entitlement.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <h3>
                    <Link className="inline-link" to={`/entitlements/${entitlement.id}`}>
                      {entitlement.name}
                    </Link>
                  </h3>
                  <p className="tag-copy">{entitlement.descriptionPreview || 'No description'}</p>
                  <div className="pill-row">
                    {sortedSupportLevels((entitlement.supportLevels || []) as Array<SupportLevelListItem>).map(level => (
                      <span key={typeof level === 'string' ? level : String(level.id)} className="status-pill">
                        {supportLevelLabel(level)}
                      </span>
                    ))}
                    {(!entitlement.supportLevels || entitlement.supportLevels.length === 0) && (
                      <span className="muted-text">No support levels</span>
                    )}
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}
