import { useParams } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import { SelectableUserSummary } from '../components/users/UserComponents';
import { sortUsersByName, sortEntitlementAssignments } from '../utils/formatting';
import type { SessionPageProps } from '../types/app';
import type { CompanyAssignment, CompanyRecord } from '../types/domain';

export default function CompanyDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const companyState = useJson<CompanyRecord>(id ? `/api/companies/${id}` : null);
  const company = companyState.data;
  const sortedEntitlements = sortEntitlementAssignments(company?.entitlementAssignments || []);
  const sortedSuperusers = sortUsersByName(company?.selectedSuperusers || []);
  const sortedUsers = sortUsersByName(company?.selectedUsers || []);
  const sortedTams = sortUsersByName(company?.selectedTams || []);

  return (
    <section className="panel">
      <DataState state={companyState} emptyMessage="Company not found." signInHref={sessionState.data?.homePath || '/login'}>
        {company && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={company.name || '—'} readOnly />
                  </label>
                  <label>
                    Phone
                    <input value={company.phoneNumber || '—'} readOnly />
                  </label>
                  <label>
                    Country
                    <input value={company.countryName || '—'} readOnly />
                  </label>
                  <label>
                    Time zone
                    <input value={company.timezoneName || '—'} readOnly />
                  </label>
                  <label>
                    Address1
                    <input value={company.address1 || '—'} readOnly />
                  </label>
                  <label>
                    Address2
                    <input value={company.address2 || '—'} readOnly />
                  </label>
                  <label>
                    City
                    <input value={company.city || '—'} readOnly />
                  </label>
                  <label>
                    State
                    <input value={company.state || '—'} readOnly />
                  </label>
                  <label>
                    Zip
                    <input value={company.zip || '—'} readOnly />
                  </label>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Entitlements</div>
                    <div className="owner-detail-panel-body">
                      {sortedEntitlements.length === 0 ? (
                        <p className="muted-text">—</p>
                      ) : (
                        <ul className="plain-list">
                          {sortedEntitlements.map((entry: CompanyAssignment, index) => (
                            <li key={`${entry.entitlementId}-${entry.levelId}-${index}`}>
                              {entry.entitlementName} • {entry.levelName}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Superuser</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedSuperusers} />
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">User</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedUsers} />
                    </div>
                  </div>
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">TAMs</div>
                    <div className="owner-detail-panel-body">
                      <SelectableUserSummary users={sortedTams} />
                    </div>
                  </div>
                  <div className="detail-card-spacer" aria-hidden="true" />
                </div>
              </div>
            </div>

            {company.id && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={`/companies/${company.id}/edit`}>
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
