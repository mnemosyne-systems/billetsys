import { $createHeadingNode, type HeadingTagType } from "@lexical/rich-text";
import { $setBlocksType } from "@lexical/selection";
import { $getSelection } from "lexical";

import { useToolbarContext } from "@/components/editor/toolbar-context";
import { blockTypeToBlockName } from "@/components/editor/block-format-data";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

export function FormatHeading({ levels = [] }: { levels: HeadingTagType[] }) {
  const { activeEditor, blockType } = useToolbarContext();

  const formatHeading = (headingSize: HeadingTagType) => {
    if (blockType !== headingSize) {
      activeEditor.update(() => {
        const selection = $getSelection();
        $setBlocksType(selection, () => $createHeadingNode(headingSize));
      });
    }
  };

  return levels.map((level) => (
    <DropdownMenuItem key={level} onClick={() => formatHeading(level)}>
      <div className="flex items-center gap-1 font-normal">
        {blockTypeToBlockName[level].icon}
        {blockTypeToBlockName[level].label}
      </div>
    </DropdownMenuItem>
  ));
}
