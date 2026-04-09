import { INSERT_HORIZONTAL_RULE_COMMAND } from "@lexical/react/LexicalHorizontalRuleNode";

import { ScissorsIcon } from "lucide-react";

import { useToolbarContext } from "@/components/editor/toolbar-context";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

export function InsertHorizontalRule() {
  const { activeEditor } = useToolbarContext();

  return (
    <DropdownMenuItem
      onClick={() =>
        activeEditor.dispatchCommand(INSERT_HORIZONTAL_RULE_COMMAND, undefined)
      }
    >
      <div className="flex items-center gap-1">
        <ScissorsIcon className="size-4" />
        <span>Horizontal Rule</span>
      </div>
    </DropdownMenuItem>
  );
}
