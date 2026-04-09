import { INSERT_EMBED_COMMAND } from "@lexical/react/LexicalAutoEmbedPlugin";

import { useToolbarContext } from "@/components/editor/toolbar-context";
import { EmbedConfigs } from "@/components/editor/auto-embed-plugin";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

export function InsertEmbeds() {
  const { activeEditor } = useToolbarContext();
  return EmbedConfigs.map((embedConfig) => (
    <DropdownMenuItem
      key={embedConfig.type}
      onClick={() => {
        activeEditor.dispatchCommand(INSERT_EMBED_COMMAND, embedConfig.type);
      }}
    >
      <div className="flex items-center gap-1">
        {embedConfig.icon}
        <span>{embedConfig.contentName}</span>
      </div>
    </DropdownMenuItem>
  ));
}
