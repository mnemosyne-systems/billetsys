import type { ChangeEvent, FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import DataState from '../components/common/DataState';
import MarkdownContent from '../components/markdown/MarkdownContent';
import MarkdownEditor from '../components/markdown/MarkdownEditor';
import { UserHoverLink, UserReferenceInlineList } from '../components/users/UserComponents';
import useJson from '../hooks/useJson';
import { postForm } from '../utils/api';
import { formatFileSize, toQueryString, versionLabel } from '../utils/formatting';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { AttachmentReference, MessageReference, NamedEntity, SupportTicketDetailRecord, VersionInfo } from '../types/domain';
import type { SupportTicketDetailState } from '../types/forms';

interface SupportTicketDetailPageProps extends SessionPageProps {
  apiBase?: string;
  backPath?: string;
  titleFallback?: string;
  secondaryUsersLabel?: string;
}

export default function SupportTicketDetailPage({
  sessionState,
  apiBase = '/api/support/tickets',
  backPath = '/support/tickets',
  titleFallback = 'Support ticket',
  secondaryUsersLabel = 'TAM'
}: SupportTicketDetailPageProps) {
  const { id } = useParams();
  const location = useLocation();
  const [refreshNonce, setRefreshNonce] = useState(0);
  const ticketState = useJson<SupportTicketDetailRecord>(id ? `${apiBase}/${id}${toQueryString({ refresh: refreshNonce })}` : null);
  const ticket = ticketState.data;
  const [formState, setFormState] = useState<SupportTicketDetailState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const [replyState] = useState({ saving: false, error: '' });
  const [replyBody, setReplyBody] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const replyInputRef = useRef<HTMLTextAreaElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messagesHeadingRef = useRef<HTMLHeadingElement | null>(null);
  const [scrollToMessages, setScrollToMessages] = useState(false);
  const isClosed = ticket?.displayStatus === 'Closed';
  const canEditStatus = ticket?.editableStatus ?? true;
  const canEditCategory = ticket?.editableCategory ?? true;
  const canEditExternalIssue = ticket?.editableExternalIssue ?? true;
  const canEditAffectsVersion = ticket?.editableAffectsVersion ?? true;
  const canEditResolvedVersion = ticket?.editableResolvedVersion ?? true;
  const showLevelField = apiBase !== '/api/user/tickets' || sessionState.data?.role === 'tam';

  useEffect(() => {
    if (!ticket) {
      return;
    }
    setFormState({
      status: ticket.displayStatus || 'Open',
      categoryId: ticket.categoryId ? String(ticket.categoryId) : '',
      externalIssueLink: ticket.externalIssueLink || '',
      affectsVersionId: ticket.affectsVersionId ? String(ticket.affectsVersionId) : '',
      resolvedVersionId: ticket.resolvedVersionId ? String(ticket.resolvedVersionId) : ''
    });
  }, [ticket]);

  useEffect(() => {
    if (!ticket) {
      return;
    }
    const shouldScrollFromQuery = new URLSearchParams(location.search).has('replyAdded');
    if (!scrollToMessages && !shouldScrollFromQuery) {
      return;
    }
    messagesHeadingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setScrollToMessages(false);
    if (shouldScrollFromQuery) {
      window.history.replaceState({}, '', ticket.actionPath || location.pathname);
    }
  }, [ticket, scrollToMessages, location.search, location.pathname]);

  const updateFormState = <K extends keyof SupportTicketDetailState>(field: K, value: SupportTicketDetailState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const saveTicket = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!ticket || !formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(ticket.actionPath || `${apiBase}/${id || ''}`, [
        ['status', formState.status],
        ['companyId', ticket.companyId],
        ['companyEntitlementId', ticket.companyEntitlementId],
        ['categoryId', formState.categoryId || null],
        ['externalIssueLink', formState.externalIssueLink],
        ['affectsVersionId', formState.affectsVersionId],
        ['resolvedVersionId', formState.resolvedVersionId || null]
      ]);
      setRefreshNonce(current => current + 1);
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const addReplyFiles = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFiles = Array.from(event.target.files || []);
    if (nextFiles.length === 0) {
      return;
    }
    setFiles(current => [...current, ...nextFiles]);
    event.target.value = '';
  };

  const removeReplyFile = (index: number) => {
    setFiles(current => current.filter((_, fileIndex) => fileIndex !== index));
  };

  return (
    <section className="panel support-ticket-detail-page">
      <SmartLink className="inline-link back-link" href={backPath}>
        Back to tickets
      </SmartLink>

      <DataState state={ticketState} emptyMessage="Ticket not found." signInHref={sessionState.data?.homePath || '/login'}>
        {ticket && formState && (
          <>
            <div className="form-card ticket-detail-card">
              <h1>{ticket.name || titleFallback}</h1>
              <form className="owner-form ticket-detail-form" onSubmit={saveTicket}>
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Ticket
                    <input value={ticket.name || ''} readOnly />
                  </label>
                  <label>
                    Company
                    <div className="readonly-link-field">
                      <input value={ticket.companyName || ''} readOnly />
                      {ticket.companyId ? (
                        <a className="readonly-link-field-link" href={`/support/companies/${ticket.companyId}`}>
                          {ticket.companyName || '-'}
                        </a>
                      ) : null}
                    </div>
                  </label>
                  <label>
                    Category
                    {isClosed || !canEditCategory ? (
                      <input value={ticket.categoryName || '-'} readOnly />
                    ) : (
                      <select value={formState.categoryId} onChange={event => updateFormState('categoryId', event.target.value)}>
                        {(ticket.categories || []).map((category: NamedEntity) => (
                          <option key={category.id} value={category.id}>
                            {category.name}
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Entitlement
                    <input value={ticket.entitlementName || '-'} readOnly className={ticket.ticketEntitlementExpired ? 'expired-input' : ''} />
                  </label>
                  <label>
                    Status
                    {isClosed || !canEditStatus ? (
                      <input value={formState.status || '-'} readOnly />
                    ) : (
                      <select value={formState.status} onChange={event => updateFormState('status', event.target.value)}>
                        {(ticket.statusOptions || []).map(option => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  {showLevelField ? (
                    <label>
                      Level
                      <input value={ticket.levelName || '-'} readOnly className={ticket.ticketEntitlementExpired ? 'expired-input' : ''} />
                    </label>
                  ) : (
                    <label className="ticket-detail-spacer" aria-hidden="true">
                      <input value="-" readOnly />
                    </label>
                  )}
                  <label>
                    External issue
                    {isClosed || !canEditExternalIssue ? (
                      formState.externalIssueLink ? (
                        <a href={formState.externalIssueLink} target="_blank" rel="noreferrer">
                          {formState.externalIssueLink}
                        </a>
                      ) : (
                        <input value="-" readOnly />
                      )
                    ) : (
                      <div className="inline-link-field">
                        <input value={formState.externalIssueLink} onChange={event => updateFormState('externalIssueLink', event.target.value)} />
                        {formState.externalIssueLink ? (
                          <a className="inline-link-field-link" href={formState.externalIssueLink} target="_blank" rel="noreferrer">
                            Open
                          </a>
                        ) : null}
                      </div>
                    )}
                  </label>
                  <label className="ticket-detail-spacer" aria-hidden="true">
                    <input value="-" readOnly />
                  </label>
                  <label>
                    Affects
                    {isClosed || !canEditAffectsVersion ? (
                      <input value={versionLabel(ticket.versions, formState.affectsVersionId) || '-'} readOnly />
                    ) : (
                      <select value={formState.affectsVersionId} onChange={event => updateFormState('affectsVersionId', event.target.value)}>
                        {(ticket.versions || []).length === 0 && <option value="">-</option>}
                        {(ticket.versions || []).map((version: VersionInfo) => (
                          <option key={version.id} value={version.id}>
                            {version.name} ({version.date})
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Resolved
                    {isClosed || !canEditResolvedVersion ? (
                      <input value={versionLabel(ticket.versions, formState.resolvedVersionId) || '-'} readOnly />
                    ) : (
                      <select value={formState.resolvedVersionId} onChange={event => updateFormState('resolvedVersionId', event.target.value)}>
                        <option value="">-</option>
                        {(ticket.versions || []).map((version: VersionInfo) => (
                          <option key={version.id} value={version.id}>
                            {version.name} ({version.date})
                          </option>
                        ))}
                      </select>
                    )}
                  </label>
                  <label>
                    Support
                    <div className="ticket-user-field">
                      <UserReferenceInlineList users={ticket.supportUsers} />
                    </div>
                  </label>
                  <label>
                    {ticket.secondaryUsersLabel || secondaryUsersLabel}
                    <div className="ticket-user-field">
                      <UserReferenceInlineList users={ticket.secondaryUsers || ticket.tamUsers} />
                    </div>
                  </label>
                </div>

                {saveState.error && <p className="error-text">{saveState.error}</p>}

                {!isClosed && (canEditStatus || canEditCategory || canEditExternalIssue || canEditAffectsVersion || canEditResolvedVersion) && (
                  <div className="form-actions">
                    <button type="submit" className="action-button" disabled={saveState.saving}>
                      {saveState.saving ? 'Saving...' : 'Save ticket'}
                    </button>
                  </div>
                )}
              </form>
            </div>

            <h2 ref={messagesHeadingRef}>Messages</h2>
            {!ticket.messages || ticket.messages.length === 0 ? (
              <p className="muted-text">No messages yet.</p>
            ) : (
              <table className="message-table">
                {(ticket.messages || []).map((message: MessageReference) => (
                  <tbody key={message.id}>
                    <tr className="message-header">
                      <td>{message.dateLabel || '-'}</td>
                      <td className="message-email">
                        {message.author?.detailPath ? (
                          <UserHoverLink user={message.author} className="inline-link">
                            {message.author.displayName || message.author.username}
                          </UserHoverLink>
                        ) : (
                          message.author?.displayName || message.author?.username || '-'
                        )}
                      </td>
                    </tr>
                    <tr>
                      <td colSpan={2}>
                        <div className="markdown-output">
                          <MarkdownContent>{message.body || ''}</MarkdownContent>
                        </div>
                      </td>
                    </tr>
                    {(message.attachments || []).length > 0 && (
                      <tr className="message-attachments">
                        <td colSpan={2}>
                          {(message.attachments || []).map((attachment: AttachmentReference) => (
                            <div key={attachment.id} className="attachment-footer">
                              <span className="attachment-name">
                                <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
                                  {attachment.name}
                                </a>
                              </span>
                              <span className="attachment-meta">
                                {attachment.mimeType} - {attachment.sizeLabel}
                              </span>
                            </div>
                          ))}
                        </td>
                      </tr>
                    )}
                  </tbody>
                ))}
              </table>
            )}

            {!isClosed ? (
              <>
                <h2>Reply</h2>
                <form className="ticket-reply-form" action={ticket.messageActionPath} method="post" encType="multipart/form-data">
                  <MarkdownEditor value={replyBody} onChange={setReplyBody} inputRef={replyInputRef} name="body" rows={6} required />

                  <div className="reply-attachment-container">
                    <span className="attachment-label">Attachments</span>
                    <div className="reply-attachment-list">
                      <table>
                        <thead>
                          <tr>
                            <th>Name</th>
                            <th>Mimetype</th>
                            <th>Size</th>
                            <th></th>
                          </tr>
                        </thead>
                        <tbody>
                          {files.map((file, index) => (
                            <tr key={`${file.name}-${file.size}-${index}`}>
                              <td>{file.name}</td>
                              <td>{file.type || 'application/octet-stream'}</td>
                              <td>{formatFileSize(file.size)}</td>
                              <td>
                                <button type="button" className="secondary-button attachment-remove-button" onClick={() => removeReplyFile(index)}>
                                  Remove
                                </button>
                              </td>
                            </tr>
                          ))}
                          {files.length === 0 && (
                            <tr>
                              <td colSpan={4} className="muted-text">
                                No attachments selected.
                              </td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                    <input ref={fileInputRef} type="file" name="attachments" multiple className="attachment-input" onChange={addReplyFiles} />
                  </div>

                  {replyState.error && <p className="error-text">{replyState.error}</p>}

                  <div className="form-actions attachment-actions">
                    {ticket.exportPath && (
                      <a className="action-button export-btn" href={ticket.exportPath}>
                        Export
                      </a>
                    )}
                    <button type="button" className="action-button" onClick={() => fileInputRef.current?.click()}>
                      Browse
                    </button>
                    <button type="submit" className="action-button" disabled={replyState.saving}>
                      {replyState.saving ? 'Adding...' : 'Add'}
                    </button>
                  </div>
                </form>
              </>
            ) : (
              ticket.exportPath && (
                <div className="form-actions attachment-actions">
                  <a className="action-button export-btn" href={ticket.exportPath}>
                    Export
                  </a>
                </div>
              )
            )}
          </>
        )}
      </DataState>
    </section>
  );
}
