/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useState, useMemo } from "react";
import { toast } from "sonner";
import { postForm } from "../../utils/api";
import useJson from "../../hooks/useJson";
import type { UserReference, DirectoryUsersResponse } from "../../types/domain";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Plus, X } from "lucide-react";

interface ExternalUserPickerProps {
  ticketId: string | number;
  role: string;
  users: UserReference[];
  isClosed?: boolean;
  onUpdate: () => void;
  endpointSuffix?: string;
}

export function ExternalUserPicker({
  ticketId,
  role,
  users,
  isClosed,
  onUpdate,
  endpointSuffix = "externals",
}: ExternalUserPickerProps) {
  const availableUsersState = useJson<DirectoryUsersResponse>(
    `/api/${role}/externals`,
  );
  const addedUserIds = useMemo(() => new Set(users.map((u) => u.id)), [users]);
  const availableUsers = useMemo(
    () =>
      (availableUsersState.data?.items || []).filter(
        (u) => !addedUserIds.has(u.id),
      ),
    [availableUsersState.data?.items, addedUserIds],
  );

  const [selectedEmail, setSelectedEmail] = useState<string>("");
  const [adding, setAdding] = useState(false);
  const [removingId, setRemovingId] = useState<string | number | null>(null);

  const handleAdd = async () => {
    if (!selectedEmail) return;

    setAdding(true);
    try {
      await postForm(`/api/${role}/tickets/${ticketId}/${endpointSuffix}/add`, [
        ["email", selectedEmail],
      ]);
      setSelectedEmail("");
      onUpdate();
      toast.success("User added.");
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Failed to add external contributor",
      );
    } finally {
      setAdding(false);
    }
  };

  const handleRemove = async (userId: string | number) => {
    setRemovingId(userId);
    try {
      await postForm(
        `/api/${role}/tickets/${ticketId}/${endpointSuffix}/${userId}/remove`,
        [],
      );
      onUpdate();
      toast.success("User removed.");
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Failed to remove external contributor",
      );
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <div className="space-y-2">
      {!isClosed && (
        <div className="flex items-center gap-2">
          <Select
            value={selectedEmail}
            onValueChange={setSelectedEmail}
            disabled={adding}
          >
            <SelectTrigger className="h-8 flex-1">
              <SelectValue placeholder="Select an external contributor" />
            </SelectTrigger>
            <SelectContent>
              {availableUsers.map((user) => (
                <SelectItem key={user.id} value={user.email || ""}>
                  {user.displayName || user.fullName || user.email}
                </SelectItem>
              ))}
              {availableUsers.length === 0 && (
                <div className="px-2 py-2 text-sm text-muted-foreground">
                  No external contributors available. Create one in the
                  Directory.
                </div>
              )}
            </SelectContent>
          </Select>
          <button
            type="button"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-emerald-600 text-white hover:bg-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 focus-visible:ring-offset-2 disabled:opacity-50"
            onClick={handleAdd}
            disabled={adding || !selectedEmail}
            title="Add"
          >
            <Plus className="h-4 w-4" />
            <span className="sr-only">Add</span>
          </button>
        </div>
      )}
      <div className="flex flex-col gap-2">
        {users.map((user) => (
          <div
            key={user.id}
            className="flex items-center justify-between gap-2 rounded-md border bg-muted/50 px-3 py-1.5 text-sm"
          >
            <span className="font-medium truncate">
              {user.email || user.name || user.fullName}
            </span>
            {!isClosed && (
              <button
                type="button"
                className="text-destructive hover:text-destructive/80 disabled:opacity-50 shrink-0"
                onClick={() => handleRemove(user.id)}
                disabled={removingId === user.id}
                title="Remove"
              >
                <X className="h-4 w-4" />
                <span className="sr-only">Remove</span>
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
