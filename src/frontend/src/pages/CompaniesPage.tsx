import { Link } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CollectionResponse, CompanyRecord } from '../types/domain';

export default function CompaniesPage({ sessionState }: SessionPageProps) {
  const companiesState = useJson<CollectionResponse<CompanyRecord>>('/api/companies');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Companies</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href={companiesState.data?.createPath || '/companies/new'}>
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={companiesState} emptyMessage="No companies are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {companiesState.data?.items.map((company: CompanyRecord) => (
            <article key={company.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <h3>
                    <Link className="inline-link" to={`/companies/${company.id}`}>
                      {company.name}
                    </Link>
                  </h3>
                  <p className="muted-text">
                    {[company.countryName, company.timezoneName].filter(Boolean).join(' • ') || 'No locale configured'}
                  </p>
                  <p className="muted-text">
                    {company.superuserCount} superuser{company.superuserCount === 1 ? '' : 's'} • {company.userCount} users • {company.tamCount} TAMs
                  </p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}
