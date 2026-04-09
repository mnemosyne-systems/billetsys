/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useNavigate, useParams } from "react-router-dom";
import DataState from "../components/common/DataState";
import { toast } from "sonner";
import { UserDetailCard } from "../components/users/UserProfileSections";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm } from "../utils/api";
import {
  resolveClientPath,
  resolvePostRedirectPath,
  SmartLink,
} from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { DirectoryUserDetail } from "../types/domain";
import { Button } from "../components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../components/ui/alert-dialog";

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

  const detailState = useJson<DirectoryUserDetail>(
    id ? `${apiBase}/${id}` : null,
  );
  const detail = detailState.data;
  const submissionGuard = useSubmissionGuard();

  const deleteUser = async () => {
    if (!detail?.deletePath || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      const response = await postForm(detail.deletePath, []);
      toast.success("User deleted.");
      navigate(
        await resolvePostRedirectPath(
          response,
          resolveClientPath(detail.backPath, backFallback),
        ),
      );
    } catch (error: unknown) {
      toast.error(
        error instanceof Error ? error.message : "Unable to delete user.",
      );
    } finally {
      submissionGuard.exit();
    }
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex items-center gap-4 pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {detail?.displayName ||
            detail?.fullName ||
            detail?.username ||
            "User details"}
        </h2>
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
                  <Button asChild>
                    <SmartLink href={detail.editPath}>Edit</SmartLink>
                  </Button>
                )}
                {detail.deletePath && (
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button type="button" variant="destructive">
                        Delete user
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                          This action cannot be undone. This will permanently
                          delete this user.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={deleteUser}
                          className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                          Delete
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </>
            }
          />
        )}
      </DataState>
    </section>
  );
}
