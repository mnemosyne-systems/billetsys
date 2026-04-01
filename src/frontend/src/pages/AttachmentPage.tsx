/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from 'react-router-dom';
import DataState from '../components/common/DataState';
import useJson from '../hooks/useJson';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { AttachmentDetail, AttachmentLine } from '../types/domain';

export default function AttachmentPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const attachmentState = useJson<AttachmentDetail>(id ? `/api/attachments/${id}` : null);
  const attachment = attachmentState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href={attachment?.backPath || '/'}>
            Back
          </SmartLink>
          <h2>{attachment?.name || 'Attachment'}</h2>
          <p className="section-copy">{attachment?.mimeType || 'Attachment preview'}</p>
        </div>
        <div className="button-row">
          {attachment?.downloadPath && (
            <a className="primary-button" href={attachment.downloadPath} target="_blank" rel="noreferrer">
              Open raw file
            </a>
          )}
        </div>
      </div>

      <DataState state={attachmentState} emptyMessage="Attachment not found." signInHref={sessionState.data?.homePath || '/login'}>
        {attachment && (
          <div className="article-detail">
            <section className="detail-grid">
              <div className="detail-card">
                <h3>Type</h3>
                <p>{attachment.mimeType || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Size</h3>
                <p>{attachment.sizeLabel || '—'}</p>
              </div>
              <div className="detail-card">
                <h3>Ticket</h3>
                <p>{attachment.ticketName || '—'}</p>
              </div>
            </section>

            {attachment.image ? (
              <div className="markdown-card">
                <img src={attachment.downloadPath} alt={attachment.name} style={{ maxWidth: '100%' }} />
              </div>
            ) : (
              <section className="detail-card">
                <h3>Contents</h3>
                <pre className="markdown-card">
                  {(attachment.lines || []).map((line: AttachmentLine) => `${line.number}. ${line.content}`).join('\n') || attachment.messageBody || ''}
                </pre>
              </section>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}

