import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import DataState from '../components/common/DataState';
import useJson from '../hooks/useJson';
import { postForm } from '../utils/api';
import { toQueryString } from '../utils/formatting';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CompanyEntitlementOption, NamedEntity, TicketWorkbenchBootstrap, VersionInfo } from '../types/domain';
import type { TicketWorkbenchFormState } from '../types/forms';
import { SUPPORT_TICKET_STATUSES } from '../types/tickets';

export default function TicketWorkbenchFormPage({ sessionState }: SessionPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get('companyId') || '';
  const [formState, setFormState] = useState<TicketWorkbenchFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const bootstrapState = useJson<TicketWorkbenchBootstrap>(
    `/api/ticket-workbench/bootstrap${toQueryString({ ticketId: id, companyId: formState?.companyId || requestedCompanyId })}`
  );
  const bootstrap = bootstrapState.data;

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState(current => {
      if (!current || String(current.id || '') !== String(bootstrap.ticket?.id || '')) {
        return {
          id: bootstrap.ticket?.id ? String(bootstrap.ticket.id) : '',
          status: bootstrap.ticket?.status || 'Open',
          companyId: bootstrap.ticket?.companyId ? String(bootstrap.ticket.companyId) : '',
          companyEntitlementId: bootstrap.ticket?.companyEntitlementId ? String(bootstrap.ticket.companyEntitlementId) : '',
          categoryId: bootstrap.ticket?.categoryId ? String(bootstrap.ticket.categoryId) : '',
          externalIssueLink: bootstrap.ticket?.externalIssueLink || '',
          affectsVersionId: bootstrap.ticket?.affectsVersionId ? String(bootstrap.ticket.affectsVersionId) : '',
          resolvedVersionId: bootstrap.ticket?.resolvedVersionId ? String(bootstrap.ticket.resolvedVersionId) : ''
        };
      }
      const entitlements = bootstrap.entitlements || [];
      const versions = bootstrap.versions || [];
      return {
        ...current,
        companyEntitlementId: entitlements.some(option => String(option.id) === current.companyEntitlementId)
          ? current.companyEntitlementId
          : entitlements[0]?.id
            ? String(entitlements[0].id)
            : '',
        affectsVersionId: versions.some(option => String(option.id) === current.affectsVersionId)
          ? current.affectsVersionId
          : versions[0]?.id
            ? String(versions[0].id)
            : '',
        resolvedVersionId: versions.some(option => String(option.id) === current.resolvedVersionId) ? current.resolvedVersionId : ''
      };
    });
  }, [bootstrap]);

  const updateFormState = <K extends keyof TicketWorkbenchFormState>(field: K, value: TicketWorkbenchFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !bootstrap) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(bootstrap.submitPath, [
        ['status', formState.status],
        ['companyId', formState.companyId],
        ['companyEntitlementId', formState.companyEntitlementId],
        ['categoryId', formState.categoryId],
        ['externalIssueLink', formState.externalIssueLink],
        ['affectsVersionId', formState.affectsVersionId],
        ['resolvedVersionId', formState.resolvedVersionId]
      ]);
      navigate('/tickets');
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to save ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  const deleteTicket = async () => {
    if (!id || !window.confirm('Delete this ticket?')) {
      return;
    }
    setSaveState({ saving: true, error: '' });
    try {
      await postForm(`/tickets/${id}/delete`, []);
      navigate('/tickets');
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to delete ticket.' });
      return;
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink className="inline-link back-link" href="/tickets">
            Back to tickets
          </SmartLink>
          <h2>{bootstrap?.title || 'Ticket form'}</h2>
        </div>
        <div className="button-row">
          {id && (
            <button type="button" className="secondary-button danger-button" onClick={deleteTicket} disabled={saveState.saving}>
              Delete ticket
            </button>
          )}
        </div>
      </div>

      <DataState state={bootstrapState} emptyMessage="Unable to load the ticket form." signInHref={sessionState.data?.homePath || '/login'}>
        {bootstrap && formState && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label>
                Status
                <select value={formState.status} onChange={event => updateFormState('status', event.target.value)}>
                  {SUPPORT_TICKET_STATUSES.filter(status => status !== 'Resolved').map(status => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Company
                <select
                  value={formState.companyId}
                  onChange={event =>
                    setFormState(current =>
                      current
                        ? {
                            ...current,
                            companyId: event.target.value,
                            companyEntitlementId: '',
                            affectsVersionId: '',
                            resolvedVersionId: ''
                          }
                        : current
                    )
                  }
                >
                  {(bootstrap.companies || []).map((company: NamedEntity) => (
                    <option key={company.id} value={company.id ? String(company.id) : ''}>
                      {company.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Entitlement
                <select value={formState.companyEntitlementId} onChange={event => updateFormState('companyEntitlementId', event.target.value)}>
                  {(bootstrap.entitlements || []).map((entitlement: CompanyEntitlementOption) => (
                    <option key={entitlement.id} value={entitlement.id ? String(entitlement.id) : ''}>
                      {entitlement.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Category
                <select value={formState.categoryId} onChange={event => updateFormState('categoryId', event.target.value)}>
                  {(bootstrap.categories || []).map((category: NamedEntity) => (
                    <option key={category.id} value={category.id ? String(category.id) : ''}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Affects version
                <select value={formState.affectsVersionId} onChange={event => updateFormState('affectsVersionId', event.target.value)}>
                  <option value="">Select version</option>
                  {(bootstrap.versions || []).map((version: VersionInfo) => (
                    <option key={version.id} value={version.id ? String(version.id) : ''}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Resolved version
                <select value={formState.resolvedVersionId} onChange={event => updateFormState('resolvedVersionId', event.target.value)}>
                  <option value="">Select version</option>
                  {(bootstrap.versions || []).map((version: VersionInfo) => (
                    <option key={version.id} value={version.id ? String(version.id) : ''}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-span-2">
                External issue
                <input value={formState.externalIssueLink} onChange={event => updateFormState('externalIssueLink', event.target.value)} placeholder="https://github.com/..." />
              </label>
            </div>

            {bootstrap.edit && (
              <section className="detail-card">
                <h3>Messages</h3>
                <ul className="plain-list">
                  {(bootstrap.messages || []).map(message => (
                    <li key={message.id}>
                      <strong>{message.dateLabel || '-'}</strong> - {message.body || 'No message body'}
                    </li>
                  ))}
                  {(!bootstrap.messages || bootstrap.messages.length === 0) && <li>No messages yet.</li>}
                </ul>
              </section>
            )}

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Saving...' : bootstrap.edit ? 'Save ticket' : 'Create ticket'}
              </button>
              <SmartLink className="secondary-button" href="/tickets">
                Cancel
              </SmartLink>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
