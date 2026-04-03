/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useNavigate, useParams } from "react-router-dom";
import DataState from "../components/common/DataState";
import {
  buildToastNavigationState,
  useToast,
} from "../components/common/ToastProvider";
import { UserDetailCard } from "../components/users/UserProfileSections";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm } from "../utils/api";
import {
  resolveClientPath,
  resolvePostRedirectPath,
  SmartLink,
} from "../utils/routing";
import type { MouseEvent } from "react";
import type { SessionPageProps } from "../types/app";
import type { DirectoryUserDetail } from "../types/domain";

interface DirectoryUserDetailPageProps extends SessionPageProps {
  apiBase: string;
  backFallback: string;
}

export default function DirectoryUserDetailPage({
  sessionState,
  apiBase,
  backFallback,
}: DirectoryUserDetailPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const { showToast } = useToast();
  const detailState = useJson<DirectoryUserDetail>(
    id ? `${apiBase}/${id}` : null,
  );
  const detail = detailState.data;
  const resolvedBackHref = detail?.backPath || backFallback;
  const submissionGuard = useSubmissionGuard();

  const handleBackClick = (event: MouseEvent<HTMLAnchorElement>) => {
    if (window.history.length > 1) {
      event.preventDefault();
      navigate(-1);
    }
  };

  const deleteUser = async () => {
    if (
      !detail?.deletePath ||
      !window.confirm("Delete this user?") ||
      !submissionGuard.tryEnter()
    ) {
      return;
    }
    try {
      const response = await postForm(detail.deletePath, []);
      navigate(
        await resolvePostRedirectPath(
          response,
          resolveClientPath(detail.backPath, backFallback),
        ),
        {
          state: buildToastNavigationState({
            variant: "danger",
            message: "User deleted.",
          }),
        },
      );
    } catch (error: unknown) {
      showToast({
        variant: "error",
        message:
          error instanceof Error ? error.message : "Unable to delete user.",
      });
    } finally {
      submissionGuard.exit();
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
          <h2>
            {detail?.displayName ||
              detail?.fullName ||
              detail?.username ||
              "User details"}
          </h2>
        </div>
      </div>

      <DataState
        state={detailState}
        emptyMessage="User not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {detail && (
          <UserDetailCard
            user={{
              username: detail.username,
              fullName: detail.fullName,
              email: detail.email,
              social: detail.social,
              phoneNumber: detail.phoneNumber,
              phoneExtension: detail.phoneExtension,
              type: detail.type,
              typeLabel: detail.typeLabel,
              countryName: detail.countryName,
              timezoneName: detail.timezoneName,
              companyName: detail.companyName,
              logoBase64: detail.logoBase64,
            }}
            companyHref={detail.companyPath}
            actions={
              <>
                {detail.editPath && (
                  <SmartLink className="primary-button" href={detail.editPath}>
                    Edit
                  </SmartLink>
                )}
                {detail.deletePath && (
                  <button
                    type="button"
                    className="secondary-button danger-button"
                    onClick={deleteUser}
                  >
                    Delete user
                  </button>
                )}
              </>
            }
          />
        )}
      </DataState>
    </section>
  );
}
