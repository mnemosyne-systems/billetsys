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
import type { ArticleRecord, CollectionResponse } from '../types/domain';

export default function ArticlesPage({ sessionState }: SessionPageProps) {
  const articlesState = useJson<CollectionResponse<ArticleRecord>>('/api/articles');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Articles</h2>
        </div>
        <div className="button-row">
          {articlesState.data?.canCreate && (
            <SmartLink className="primary-button" href={articlesState.data.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState state={articlesState} emptyMessage="No articles are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="ticket-table-wrap">
          <table className="support-ticket-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Tags</th>
              </tr>
            </thead>
            <tbody>
              {(articlesState.data?.items || []).map(article => (
                <tr key={article.id}>
                  <td>
                    <Link className="inline-link" to={`/articles/${article.id}`}>
                      {article.title}
                    </Link>
                  </td>
                  <td>{article.tags || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </DataState>
    </section>
  );
}

