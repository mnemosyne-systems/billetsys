/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MouseEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  DirectoryCompanyDetail as DirectoryCompanyDetailType,
  DirectoryUserRecord,
} from "../types/domain";

interface DirectoryUserReferenceListProps {
  users?: DirectoryUserRecord[];
}

function DirectoryUserReferenceList({
  users,
}: DirectoryUserReferenceListProps) {
  if (!users || users.length === 0) {
    return <p className="muted-text">No users.</p>;
  }

  return (
    <ul className="plain-list">
      {users.map((user: DirectoryUserRecord) => (
        <li key={user.id || `${user.username}-${user.type}`}>
          {user.detailPath ? (
            <SmartLink className="inline-link" href={user.detailPath}>
              {user.displayName || user.username || "User"}
            </SmartLink>
          ) : (
            <span>{user.displayName || user.username || "User"}</span>
          )}{" "}
          <span className="muted-text">
            ({user.typeLabel || user.type || "User"})
          </span>
        </li>
      ))}
    </ul>
  );
}

interface DirectoryCompanyDetailPageProps extends SessionPageProps {
  apiBase: string;
  backFallback: string;
}

export default function DirectoryCompanyDetailPage({
  sessionState,
  apiBase,
  backFallback,
}: DirectoryCompanyDetailPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const companyState = useJson<DirectoryCompanyDetailType>(
    id ? `${apiBase}/${id}` : null,
  );
  const company = companyState.data;
  const resolvedBackHref = company?.backPath || backFallback;

  const handleBackClick = (event: MouseEvent<HTMLAnchorElement>) => {
    if (window.history.length > 1) {
      event.preventDefault();
      navigate(-1);
    }
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <SmartLink
            className="inline-link back-link"
            href={resolvedBackHref}
            onClick={handleBackClick}
          >
            Back
          </SmartLink>
          <h2>{company?.name || "Company details"}</h2>
        </div>
      </div>

      <DataState
        state={companyState}
        emptyMessage="Company not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {company && (
          <div className="article-detail">
            <section className="detail-grid">
              <div className="detail-card">
                <h3>Phone</h3>
                <p>{company.phoneNumber || "—"}</p>
              </div>
              <div className="detail-card">
                <h3>Country</h3>
                <p>{company.countryName || "—"}</p>
              </div>
              <div className="detail-card">
                <h3>Time zone</h3>
                <p>{company.timezoneName || "—"}</p>
              </div>
            </section>

            <section className="owner-columns">
              <div className="detail-card">
                <h3>Address</h3>
                <ul className="plain-list">
                  {[
                    company.address1,
                    company.address2,
                    company.city,
                    company.state,
                    company.zip,
                  ].filter(Boolean).length === 0 ? (
                    <li>—</li>
                  ) : (
                    [
                      company.address1,
                      company.address2,
                      company.city,
                      company.state,
                      company.zip,
                    ]
                      .filter(Boolean)
                      .map((line) => <li key={line}>{line}</li>)
                  )}
                </ul>
              </div>
              <div className="detail-card">
                <h3>Users</h3>
                <DirectoryUserReferenceList users={company.users} />
              </div>
              <div className="detail-card">
                <h3>Superusers</h3>
                <DirectoryUserReferenceList users={company.superusers} />
              </div>
              <div className="detail-card">
                <h3>TAMs</h3>
                <DirectoryUserReferenceList users={company.tamUsers} />
              </div>
              <div className="detail-card">
                <h3>Support users</h3>
                <DirectoryUserReferenceList users={company.supportUsers} />
              </div>
            </section>
          </div>
        )}
      </DataState>
    </section>
  );
}
