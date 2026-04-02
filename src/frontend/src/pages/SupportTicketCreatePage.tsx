/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AttachmentPicker from '../components/common/AttachmentPicker';
import DataState from '../components/common/DataState';
import MarkdownEditor from '../components/markdown/MarkdownEditor';
import useJson from '../hooks/useJson';
import useSubmissionGuard from '../hooks/useSubmissionGuard';
import { postMultipart } from '../utils/api';
import { toQueryString } from '../utils/formatting';
import { resolvePostRedirectPath } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CompanyEntitlementOption, NamedEntity, SupportTicketCreateBootstrap, VersionInfo } from '../types/domain';
import type { SupportTicketCreateFormState } from '../types/forms';
import type { FormEntries } from '../utils/api';

interface SupportTicketCreatePageProps extends SessionPageProps {
  apiBase?: string;
  submitFallbackPath?: string;
  title?: string;
  description?: string;
  navigateTo?: string;
  compactCreateActions?: boolean;
  hideEntitlementLevel?: boolean;
}

export default function SupportTicketCreatePage({
  sessionState,
  apiBase = '/api/support/tickets/bootstrap',
  submitFallbackPath = '/support/tickets',
  title = '',
  description = '',
  navigateTo = '/support/tickets',
  compactCreateActions = false,
  hideEntitlementLevel = false
}: SupportTicketCreatePageProps) {
  const navigate = useNavigate();
  const [selectedCompanyId, setSelectedCompanyId] = useState('');
  const [selectedCompanyEntitlementId, setSelectedCompanyEntitlementId] = useState('');
  const bootstrapState = useJson<SupportTicketCreateBootstrap>(
    `${apiBase}${toQueryString({
      companyId: selectedCompanyId || undefined,
      companyEntitlementId: selectedCompanyEntitlementId || undefined
    })}`
  );
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] = useState<SupportTicketCreateFormState | null>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [saveState, setSaveState] = useState({ saving: false, error: '' });
  const messageInputRef = useRef<HTMLTextAreaElement | null>(null);
  const submissionGuard = useSubmissionGuard();
  const showFixedCompany = sessionState.data?.role === 'superuser';
  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState(current => {
      const initialCompanyId = bootstrap.selectedCompanyId ? String(bootstrap.selectedCompanyId) : '';
      const initialEntitlementId = bootstrap.selectedCompanyEntitlementId ? String(bootstrap.selectedCompanyEntitlementId) : '';
      const initialAffectsVersionId = bootstrap.defaultAffectsVersion?.id ? String(bootstrap.defaultAffectsVersion.id) : '';
      if (!current) {
        setSelectedCompanyId(initialCompanyId);
        setSelectedCompanyEntitlementId(initialEntitlementId);
        return {
          ticketName: bootstrap.ticketName || '',
          title: bootstrap.title || '',
          companyId: initialCompanyId,
          companyEntitlementId: initialEntitlementId,
          categoryId: bootstrap.defaultCategoryId ? String(bootstrap.defaultCategoryId) : '',
          affectsVersionId: initialAffectsVersionId,
          message: ''
        };
      }
      const validEntitlementIds = (bootstrap.companyEntitlements || []).map(entry => String(entry.id));
      const validVersionIds = (bootstrap.versions || []).map(version => String(version.id));
      const nextEntitlementId = validEntitlementIds.includes(current.companyEntitlementId) ? current.companyEntitlementId : initialEntitlementId;
      setSelectedCompanyEntitlementId(nextEntitlementId);
        return {
          ...current,
          ticketName: bootstrap.ticketName || current.ticketName,
          title: current.title,
          companyId: initialCompanyId || current.companyId,
          companyEntitlementId: nextEntitlementId,
          categoryId: current.categoryId || (bootstrap.defaultCategoryId ? String(bootstrap.defaultCategoryId) : ''),
        affectsVersionId: validVersionIds.includes(current.affectsVersionId) ? current.affectsVersionId : initialAffectsVersionId
      };
    });
  }, [bootstrap]);

  const updateFormState = <K extends keyof SupportTicketCreateFormState>(field: K, value: SupportTicketCreateFormState[K]) => {
    setFormState(current => (current ? { ...current, [field]: value } : current));
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: '' });
      const entries: FormEntries = [
        ['status', 'Open'],
        ['title', formState.title],
        ['message', formState.message],
        ['companyId', formState.companyId],
        ['companyEntitlementId', formState.companyEntitlementId],
        ['categoryId', formState.categoryId || null],
        ['affectsVersionId', formState.affectsVersionId || null],
        ...files.map((file): [string, File] => ['attachments', file])
      ];
      const response = await postMultipart(
        bootstrap?.submitPath || submitFallbackPath,
        entries,
        {
          headers: { 'X-Billetsys-Client': 'react' }
        }
      );
      navigate(await resolvePostRedirectPath(response, navigateTo));
    } catch (error: unknown) {
      setSaveState({ saving: false, error: error instanceof Error ? error.message : 'Unable to create ticket.' });
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: '' });
  };

  return (
    <section className="panel">
      {(bootstrap?.ticketName || title || description) ? (
        <div className="section-header">
          <div>
            <h2>{bootstrap?.ticketName || title}</h2>
            {description ? <p className="section-copy">{description}</p> : null}
          </div>
        </div>
      ) : null}

      <DataState state={bootstrapState} emptyMessage="A company is required before creating a support ticket." signInHref={sessionState.data?.homePath || '/login'}>
        {formState && bootstrap && (
          <form className="owner-form" onSubmit={submit}>
            <div className="owner-form-grid">
              <label className="form-span-2">
                Title
                <input value={formState.title} onChange={event => updateFormState('title', event.target.value)} required />
              </label>
              <label className="form-span-2">
                Company
                {showFixedCompany ? (
                  <input value={(bootstrap.companies || []).find((company: NamedEntity) => String(company.id) === formState.companyId)?.name || ''} readOnly />
                ) : (
                  <select
                    value={formState.companyId}
                    onChange={event => {
                      const nextCompanyId = event.target.value;
                      setSelectedCompanyId(nextCompanyId);
                      setSelectedCompanyEntitlementId('');
                      setFormState(current =>
                        current
                          ? {
                              ...current,
                              companyId: nextCompanyId,
                              companyEntitlementId: '',
                              affectsVersionId: ''
                            }
                          : current
                      );
                    }}
                    required
                  >
                    {(bootstrap.companies || []).map((company: NamedEntity) => (
                      <option key={company.id} value={company.id}>
                        {company.name}
                      </option>
                    ))}
                  </select>
                )}
              </label>
              <label className="form-span-2">
                Entitlement
                <select
                  value={formState.companyEntitlementId}
                  onChange={event => {
                    const nextEntitlementId = event.target.value;
                    setSelectedCompanyEntitlementId(nextEntitlementId);
                    setFormState(current =>
                      current
                        ? { ...current, companyEntitlementId: nextEntitlementId, affectsVersionId: '' }
                        : current
                    );
                  }}
                  required
                >
                  {(bootstrap.companyEntitlements || []).map((entry: CompanyEntitlementOption) => (
                    <option key={entry.id} value={entry.id}>
                      {entry.name}
                      {!hideEntitlementLevel && entry.levelName ? ` • ${entry.levelName}` : ''}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-span-2">
                Category
                <select value={formState.categoryId} onChange={event => updateFormState('categoryId', event.target.value)}>
                  {(bootstrap.categories || []).map((category: NamedEntity) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-span-2">
                Message
                <MarkdownEditor
                  value={formState.message}
                  onChange={value => updateFormState('message', value)}
                  inputRef={messageInputRef}
                  rows={10}
                  required
                />
              </label>
            </div>

            <section className="detail-grid">
              <div className="detail-card">
                <h3>Affects</h3>
                <select value={formState.affectsVersionId} onChange={event => updateFormState('affectsVersionId', event.target.value)}>
                  {(bootstrap.versions || []).length === 0 && <option value="">-</option>}
                  {(bootstrap.versions || []).map((version: VersionInfo) => (
                    <option key={version.id} value={version.id}>
                      {version.name}
                      {version.date ? ` (${version.date})` : ''}
                    </option>
                  ))}
                </select>
              </div>
              <div className="detail-card-spacer" aria-hidden="true" />
            </section>

            <AttachmentPicker files={files} onFilesChange={setFiles} />

            {saveState.error && <p className="error-text">{saveState.error}</p>}

            <div className="button-row button-row-end">
              <button type="submit" className="primary-button" disabled={saveState.saving}>
                {saveState.saving ? 'Creating...' : 'Create'}
              </button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
