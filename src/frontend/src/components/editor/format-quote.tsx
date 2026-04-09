import { $createQuoteNode } from "@lexical/rich-text";
import { $setBlocksType } from "@lexical/selection";
import { $getSelection } from "lexical";

import { useToolbarContext } from "@/components/editor/toolbar-context";
import { blockTypeToBlockName } from "@/components/editor/block-format-data";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

const BLOCK_FORMAT_VALUE = "quote";

export function FormatQuote() {
  const { activeEditor, blockType } = useToolbarContext();

  const formatQuote = () => {
    if (blockType !== "quote") {
      activeEditor.update(() => {
        const selection = $getSelection();
        $setBlocksType(selection, () => $createQuoteNode());
      });
    }
  };

  return (
    <DropdownMenuItem onClick={formatQuote}>
      <div className="flex items-center gap-1 font-normal">
        {blockTypeToBlockName[BLOCK_FORMAT_VALUE].icon}
        {blockTypeToBlockName[BLOCK_FORMAT_VALUE].label}
      </div>
    </DropdownMenuItem>
  );
}
