import { useParams } from 'react-router-dom';
import DataState from '../components/common/DataState';
import { LevelColorFieldValue } from '../components/common/LevelColorBadge';
import useJson from '../hooks/useJson';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { LevelRecord } from '../types/domain';

export default function LevelDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const levelState = useJson<LevelRecord>(id ? `/api/levels/${id}` : null);
  const level = levelState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{level?.name || 'Level details'}</h2>
        </div>
      </div>

      <DataState state={levelState} emptyMessage="Level not found." signInHref={sessionState.data?.homePath || '/login'}>
        {level && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <div className="owner-form owner-detail-form">
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Name
                    <input value={level.name || '—'} readOnly />
                  </label>
                  <label>
                    Business level
                    <input value={level.level ?? '—'} readOnly />
                  </label>
                  <label>
                    Color
                    <LevelColorFieldValue color={level.color} display={level.colorDisplay} />
                  </label>
                  <label>
                    Country
                    <input value={level.countryName || '—'} readOnly />
                  </label>
                  <label>
                    From
                    <input value={level.fromLabel || '—'} readOnly />
                  </label>
                  <label>
                    To
                    <input value={level.toLabel || '—'} readOnly />
                  </label>
                  <label>
                    Time zone
                    <input value={level.timezoneName || '—'} readOnly />
                  </label>
                  <div className="detail-card-spacer" aria-hidden="true" />
                  <label className="form-span-2">
                    Description
                    <textarea value={level.description || '—'} readOnly rows={6} />
                  </label>
                </div>
              </div>
            </div>

            {level.editPath && (
              <div className="button-row button-row-end admin-detail-actions">
                <SmartLink className="primary-button" href={level.editPath}>
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
