/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from 'react-router-dom';
import DataState from '../components/common/DataState';
import { LevelColorBadge } from '../components/common/LevelColorBadge';
import useJson from '../hooks/useJson';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CollectionResponse, LevelRecord } from '../types/domain';

export default function LevelsPage({ sessionState }: SessionPageProps) {
  const levelsState = useJson<CollectionResponse<LevelRecord>>('/api/levels');

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Support levels</h2>
        </div>
        <div className="button-row">
          <SmartLink className="primary-button" href="/levels/new">
            Create
          </SmartLink>
        </div>
      </div>

      <DataState state={levelsState} emptyMessage="No support levels are available yet." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="category-list">
          {levelsState.data?.items.map((level: LevelRecord) => (
            <article key={level.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>
                      <Link className="inline-link" to={`/levels/${level.id}`}>
                        {level.name}
                      </Link>
                    </h3>
                    <LevelColorBadge color={level.color} display={level.colorDisplay} />
                  </div>
                  <p className="tag-copy">{level.descriptionPreview || 'No description'}</p>
                  <p className="muted-text">
                    Level {level.level} • {level.fromLabel} - {level.toLabel}
                  </p>
                  <p className="muted-text">
                    {level.countryName || 'No country'} • {level.timezoneName || 'No time zone'}
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

