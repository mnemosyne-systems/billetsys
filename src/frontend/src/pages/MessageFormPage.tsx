/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import AttachmentPicker from '../components/common/AttachmentPicker';
import DataState from '../components/common/DataState';
import useJson from '../hooks/useJson';
import { postForm, postMultipart } from '../utils/api';
import { toQueryString } from '../utils/formatting';
import { PATHS } from '../routes/paths';
import { resolvePostRedirectPath, SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { MessageFormBootstrap } from '../types/domain';

interface MessageFormState {
  body: string;
  date: string;
  ticketId: string;
  files: File[];
}

export default function MessageFormPage({ sessionState }: SessionPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const bootstrapState = useJson<MessageFormBootstrap>(`/api/messages/bootstrap${toQueryString({ messageId: id })}`);
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] = useState<MessageFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });

  const updateFormState = <K extends keyof MessageFormState>(field: K, value: MessageFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState({
      body: bootstrap.message?.body || '',
      date: bootstrap.message?.date || '',
      ticketId: bootstrap.message?.ticketId ? String(bootstrap.message.ticketId) : '',
      files: []
    });
  }, [bootstrap]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!bootstrap || !formState) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      const response = await postMultipart(bootstrap.submitPath, [
        ['body', formState.body],
        ['date', formState.date],
        ['ticketId', formState.ticketId],
        ['attachments', formState.files]
      ]);
      navigate(await resolvePostRedirectPath(response, PATHS.messages));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save message.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteMessage = async () => {
    if (!id || !window.confirm('Delete this message?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      const response = await postForm(`/messages/${id}/delete`, []);
      navigate(await resolvePostRedirectPath(response, PATHS.messages));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete message.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div className="button-row">
          {id && (
            <button type="button" className="secondary-button danger-button" onClick={deleteMessage} disabled={saveState.saving}>
              Delete message
            </button>
          )}
        </div>
      </div>

      <DataState state={bootstrapState} emptyMessage="Unable to load the message form." signInHref={sessionState.data?.homePath || '/login'}>
        {bootstrap && formState && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label className="form-span-2">
                Body
                <textarea rows={8} value={formState.body} onChange={event => updateFormState('body', event.target.value)} required />
              </label>
              <label>
                Date
                <input type="datetime-local" value={formState.date} onChange={event => updateFormState('date', event.target.value)} required />
              </label>
              <label>
                Ticket
                <select value={formState.ticketId} onChange={event => updateFormState('ticketId', event.target.value)} required>
                  <option value="">Select ticket</option>
                  {(bootstrap.tickets || []).map(ticket => (
                    <option key={ticket.id} value={ticket.id ? String(ticket.id) : ''}>
                      {ticket.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <AttachmentPicker files={formState.files} onFilesChange={files => updateFormState('files', files)} existingAttachments={bootstrap.attachments || []} />

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : bootstrap.edit ? 'Save' : 'Create'}
              </button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

