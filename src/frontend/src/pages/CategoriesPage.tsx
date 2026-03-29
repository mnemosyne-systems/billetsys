import { Link } from 'react-router-dom';
import useJson from '../hooks/useJson';
import DataState from '../components/common/DataState';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CategoryRecord, CollectionResponse } from '../types/domain';

export default function CategoriesPage({ sessionState }: SessionPageProps) {
  const categoriesState = useJson<CollectionResponse<CategoryRecord>>('/api/categories');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Categories</h2>
        </div>
        <div className="button-row">
          {categoriesState.data?.canCreate && (
            <SmartLink className="primary-button" href={categoriesState.data.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={categoriesState} emptyMessage="No categories are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {categoriesState.data?.items.map((category: CategoryRecord) => (
            <article key={category.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>
                      <Link className="inline-link" to={`/categories/${category.id}`}>
                        {category.name}
                      </Link>
                    </h3>
                    {category.isDefault && <span className="status-pill">Default</span>}
                  </div>
                  <p className="tag-copy">{category.descriptionPreview || 'No description'}</p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}
