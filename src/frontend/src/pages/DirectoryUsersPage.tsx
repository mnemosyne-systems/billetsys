/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import { toQueryString } from "../utils/formatting";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  DirectoryUserRecord,
  DirectoryUsersResponse,
  NamedEntity,
} from "../types/domain";

interface DirectoryUsersPageProps extends SessionPageProps {
  apiBase: string;
  basePath: string;
  titleFallback: string;
  description?: string;
}

export default function DirectoryUsersPage({
  sessionState,
  apiBase,
  titleFallback,
  description = "",
}: DirectoryUsersPageProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const companyId = new URLSearchParams(location.search).get("companyId") || "";
  const dataState = useJson<DirectoryUsersResponse>(
    `${apiBase}${toQueryString({ companyId })}`,
  );
  const directory = dataState.data;

  const selectCompany = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextCompanyId = event.target.value;
    navigate(
      `${location.pathname}${toQueryString({ companyId: nextCompanyId })}`,
    );
  };

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{directory?.title || titleFallback}</h2>
          {directory?.description || description ? (
            <p className="section-copy">
              {directory?.description || description}
            </p>
          ) : null}
        </div>
        <div className="button-row">
          {directory?.createPath && (
            <SmartLink className="primary-button" href={directory.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState
        state={dataState}
        emptyMessage="No users are available."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        <>
          {directory?.showCompanySelector && (
            <section className="detail-card">
              <h3>Company</h3>
              <label>
                Select company
                <select
                  value={
                    directory.selectedCompanyId
                      ? String(directory.selectedCompanyId)
                      : ""
                  }
                  onChange={selectCompany}
                >
                  {(directory.companies || []).map((company: NamedEntity) => (
                    <option
                      key={company.id}
                      value={company.id ? String(company.id) : ""}
                    >
                      {company.name}
                    </option>
                  ))}
                </select>
              </label>
            </section>
          )}

          <div className="category-list">
            {(directory?.items || []).map((user: DirectoryUserRecord) => (
              <article key={user.id} className="category-card">
                <div className="category-card-head">
                  <div>
                    <div className="category-title-row">
                      <h3>
                        {user.detailPath ? (
                          <SmartLink
                            className="inline-link"
                            href={user.detailPath}
                          >
                            {user.displayName ||
                              user.fullName ||
                              user.username ||
                              "User"}
                          </SmartLink>
                        ) : (
                          user.displayName ||
                          user.fullName ||
                          user.username ||
                          "User"
                        )}
                      </h3>
                      <span className="status-pill">
                        {user.typeLabel || user.type || "User"}
                      </span>
                    </div>
                    <p className="tag-copy">{user.email || "No email"}</p>
                    <p className="muted-text">@{user.username || "unknown"}</p>
                  </div>
                </div>
              </article>
            ))}
          </div>

          {(!directory?.items || directory.items.length === 0) && (
            <p className="muted-text">
              No users are available for the selected company.
            </p>
          )}
        </>
      </DataState>
    </section>
  );
}
