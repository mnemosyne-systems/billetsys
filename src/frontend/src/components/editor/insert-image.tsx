import { ImageIcon } from "lucide-react";

import { useToolbarContext } from "@/components/editor/toolbar-context";
import { InsertImageDialog } from "@/components/editor/images-extension";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

export function InsertImage() {
  const { activeEditor, showModal } = useToolbarContext();

  return (
    <DropdownMenuItem
      onClick={() => {
        showModal("Insert Image", (onClose) => (
          <InsertImageDialog activeEditor={activeEditor} onClose={onClose} />
        ));
      }}
    >
      <div className="flex items-center gap-1">
        <ImageIcon className="size-4" />
        <span>Image</span>
      </div>
    </DropdownMenuItem>
  );
}
