import { CodeHighlightNode, CodeNode } from "@lexical/code";
import {
  AutoFocusExtension,
  ClearEditorExtension,
  DecoratorTextExtension,
  HorizontalRuleExtension,
  SelectionAlwaysOnDisplayExtension,
} from "@lexical/extension";
import { HistoryExtension } from "@lexical/history";
import {
  AutoLinkExtension,
  ClickableLinkExtension,
  LinkExtension,
} from "@lexical/link";
import { CheckListExtension, ListExtension } from "@lexical/list";
import {
  CHECK_LIST,
  ELEMENT_TRANSFORMERS,
  MULTILINE_ELEMENT_TRANSFORMERS,
  TEXT_FORMAT_TRANSFORMERS,
  TEXT_MATCH_TRANSFORMERS,
  $convertToMarkdownString,
  $convertFromMarkdownString,
  type TextMatchTransformer,
} from "@lexical/markdown";
import { OverflowNode } from "@lexical/overflow";
import { CharacterLimitPlugin } from "@lexical/react/LexicalCharacterLimitPlugin";
import { LexicalExtensionComposer } from "@lexical/react/LexicalExtensionComposer";
import { TabIndentationPlugin } from "@lexical/react/LexicalTabIndentationPlugin";
import { TablePlugin } from "@lexical/react/LexicalTablePlugin";
import { RichTextExtension } from "@lexical/rich-text";
import { TableCellNode, TableNode, TableRowNode } from "@lexical/table";
import { useLexicalComposerContext } from "@lexical/react/LexicalComposerContext";
import {
  $isTextNode,
  $createTextNode,
  configExtension,
  defineExtension,
} from "lexical";
import React, {
  useMemo,
  useState,
  useEffect,
  useRef,
  type RefObject,
} from "react";

import { ContentEditable } from "./content-editable";
import { DateTimeExtension } from "./date-time-extension";
import { EmojisExtension } from "./emojis-extension";
import { ImagesExtension } from "./images-extension";
import { MarkdownShortcutsExtension } from "./markdown-shortcuts-extension";
import { MaxLengthExtension } from "./max-length-extension";
import { AutocompleteNode } from "./autocomplete-node";
import { TweetNode } from "./tweet-node";
import { YouTubeNode } from "./youtube-node";
import { EmojiNode } from "./emoji-node";
import { LayoutContainerNode } from "./layout-container-node";
import { LayoutItemNode } from "./layout-item-node";
import { MentionNode } from "./mention-node";
import {
  TicketMentionNode,
  $isTicketMentionNode,
  $createTicketMentionNode,
} from "./ticket-mention-node";
import { TicketMentionsPlugin } from "./ticket-mentions-plugin";
import { SpecialTextNode } from "./special-text-node";
import { ActionsPlugin } from "./actions-plugin";
import { ClearEditorActionPlugin } from "./clear-editor-plugin";
import { CounterCharacterPlugin } from "./counter-character-plugin";
import { EditModeTogglePlugin } from "./edit-mode-toggle-plugin";
import { ImportExportPlugin } from "./import-export-plugin";
import { MarkdownTogglePlugin } from "./markdown-toggle-plugin";
import { ShareContentPlugin } from "./share-content-plugin";
import { SpeechToTextPlugin } from "./speech-to-text-plugin";
import { TreeViewPlugin } from "./tree-view-plugin";
import { AutoCompletePlugin } from "./auto-complete-plugin";
import { CodeActionMenuPlugin } from "./code-action-menu-plugin";
import { CodeHighlightPlugin } from "./code-highlight-plugin";
import { ComponentPickerMenuPlugin } from "./component-picker-menu-plugin";
import { ContextMenuPlugin } from "./context-menu-plugin";
import { DraggableBlockPlugin } from "./draggable-block-plugin";
import { AutoEmbedPlugin } from "./auto-embed-plugin";
import { TwitterPlugin } from "./twitter-plugin";
import { YouTubePlugin } from "./youtube-plugin";
import { EmojiPickerPlugin } from "./emoji-picker-plugin";
import { FloatingLinkEditorPlugin } from "./floating-link-editor-plugin";
import { FloatingTextFormatToolbarPlugin } from "./floating-text-format-plugin";
import { LayoutPlugin } from "./layout-plugin";
import { MentionsPlugin } from "./mentions-plugin";
import SpecialTextPlugin from "./special-text-plugin";
import { TabFocusPlugin } from "./tab-focus-plugin";
import { BlockFormatDropDown } from "./block-format-toolbar-plugin";
import { FormatBulletedList } from "./format-bulleted-list";
import { FormatCheckList } from "./format-check-list";
import { FormatCodeBlock } from "./format-code-block";
import { FormatHeading } from "./format-heading";
import { FormatNumberedList } from "./format-numbered-list";
import { FormatParagraph } from "./format-paragraph";
import { FormatQuote } from "./format-quote";
import { BlockInsertPlugin } from "./block-insert-plugin";
import { InsertColumnsLayout } from "./insert-columns-layout";
import { InsertEmbeds } from "./insert-embeds";
import { InsertHorizontalRule } from "./insert-horizontal-rule";
import { InsertImage } from "./insert-image";
import { InsertTable } from "./insert-table";
import { ClearFormattingToolbarPlugin } from "./clear-formatting-toolbar-plugin";
import { CodeLanguageToolbarPlugin } from "./code-language-toolbar-plugin";
import { ElementFormatToolbarPlugin } from "./element-format-toolbar-plugin";
import { FontBackgroundToolbarPlugin } from "./font-background-toolbar-plugin";
import { FontColorToolbarPlugin } from "./font-color-toolbar-plugin";
import { FontFamilyToolbarPlugin } from "./font-family-toolbar-plugin";
import { FontFormatToolbarPlugin } from "./font-format-toolbar-plugin";
import { FontSizeToolbarPlugin } from "./font-size-toolbar-plugin";
import { HistoryToolbarPlugin } from "./history-toolbar-plugin";
import { LinkToolbarPlugin } from "./link-toolbar-plugin";
import { SubSuperToolbarPlugin } from "./subsuper-toolbar-plugin";
import { ToolbarPlugin } from "./toolbar-plugin";
import { editorTheme } from "./editor-theme";
import { EMOJI } from "./markdown-emoji-transformer";
import { HR } from "./markdown-hr-transformer";
import { IMAGE } from "./markdown-image-transformer";
import { TABLE } from "./markdown-table-transformer";
import { TWEET } from "./markdown-tweet-transformer";
import { validateUrl } from "./url";
import { Separator } from "@/components/ui/separator";
import { TooltipProvider } from "@/components/ui/tooltip";

import { ParagraphPickerPlugin } from "./paragraph-picker-plugin";
import { HeadingPickerPlugin } from "./heading-picker-plugin";
import { NumberedListPickerPlugin } from "./numbered-list-picker-plugin";
import { BulletedListPickerPlugin } from "./bulleted-list-picker-plugin";
import { CheckListPickerPlugin } from "./check-list-picker-plugin";
import { QuotePickerPlugin } from "./quote-picker-plugin";
import { CodePickerPlugin } from "./code-picker-plugin";
import { DividerPickerPlugin } from "./divider-picker-plugin";
import { ImagePickerPlugin } from "./image-picker-plugin";
import { TablePickerPlugin } from "./table-picker-plugin";
import { ColumnsLayoutPickerPlugin } from "./columns-layout-picker-plugin";
import { AlignmentPickerPlugin } from "./alignment-picker-plugin";

const baseOptions = [
  ParagraphPickerPlugin(),
  HeadingPickerPlugin({ n: 1 }),
  HeadingPickerPlugin({ n: 2 }),
  HeadingPickerPlugin({ n: 3 }),
  NumberedListPickerPlugin(),
  BulletedListPickerPlugin(),
  CheckListPickerPlugin(),
  QuotePickerPlugin(),
  CodePickerPlugin(),
  DividerPickerPlugin(),
  ImagePickerPlugin(),
  TablePickerPlugin(),
  ColumnsLayoutPickerPlugin(),
  AlignmentPickerPlugin({ alignment: "left" }),
  AlignmentPickerPlugin({ alignment: "center" }),
  AlignmentPickerPlugin({ alignment: "right" }),
  AlignmentPickerPlugin({ alignment: "justify" }),
];

const placeholder = "Press / for commands...";
const maxLength = 3000;

interface LexicalEditorProps {
  value: string;
  onChange: (value: string) => void;
  inputRef?: RefObject<HTMLTextAreaElement | null>;
  rows?: number;
  name?: string;
  required?: boolean;
  ticketSuggestApiBase?: string;
  excludeTicketId?: number | string;
}

const SUPERSCRIPT: TextMatchTransformer = {
  dependencies: [],
  export: (node) => {
    if ($isTextNode(node) && node.hasFormat("superscript")) {
      return `<sup>${node.getTextContent()}</sup>`;
    }
    return null;
  },
  importRegExp: /<sup>(.*?)<\/sup>/,
  regExp: /<sup>(.*?)<\/sup>$/,
  replace: (textNode, match) => {
    const newNode = $createTextNode(match[1]);
    newNode.toggleFormat("superscript");
    textNode.replace(newNode);
  },
  trigger: ">",
  type: "text-match",
};

const SUBSCRIPT: TextMatchTransformer = {
  dependencies: [],
  export: (node) => {
    if ($isTextNode(node) && node.hasFormat("subscript")) {
      return `<sub>${node.getTextContent()}</sub>`;
    }
    return null;
  },
  importRegExp: /<sub>(.*?)<\/sub>/,
  regExp: /<sub>(.*?)<\/sub>$/,
  replace: (textNode, match) => {
    const newNode = $createTextNode(match[1]);
    newNode.toggleFormat("subscript");
    textNode.replace(newNode);
  },
  trigger: ">",
  type: "text-match",
};

const TICKET_MENTION: TextMatchTransformer = {
  dependencies: [TicketMentionNode],
  export: (node) => {
    if (!$isTicketMentionNode(node)) return null;
    return `#[${node.getTicketId()}]`;
  },
  importRegExp: /#\[(\d+)\]/,
  regExp: /#\[(\d+)\]$/,
  replace: (textNode, match) => {
    const ticketId = Number(match[1]);
    const node = $createTicketMentionNode(ticketId, String(ticketId), "");
    textNode.replace(node);
  },
  trigger: "]",
  type: "text-match",
};

const ALL_TRANSFORMERS = [
  TABLE,
  HR,
  IMAGE,
  EMOJI,
  TWEET,
  TICKET_MENTION,
  CHECK_LIST,
  ...ELEMENT_TRANSFORMERS,
  ...MULTILINE_ELEMENT_TRANSFORMERS,
  ...TEXT_FORMAT_TRANSFORMERS,
  SUPERSCRIPT,
  SUBSCRIPT,
  ...TEXT_MATCH_TRANSFORMERS,
];

function MarkdownExportPlugin({
  onChange,
  initialValue,
}: {
  onChange: (value: string) => void;
  initialValue: string;
}) {
  const [editor] = useLexicalComposerContext();
  const isFirstRenderRef = useRef(true);
  const onChangeRef = useRef(onChange);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  // Initialize editor with markdown content on first render
  useEffect(() => {
    if (isFirstRenderRef.current) {
      if (initialValue) {
        editor.update(() => {
          $convertFromMarkdownString(
            initialValue,
            ALL_TRANSFORMERS,
            undefined,
            true,
          );
        });
      }
      isFirstRenderRef.current = false;
    }
  }, [editor, initialValue]);

  // Listen for editor updates and convert to markdown
  useEffect(() => {
    return editor.registerUpdateListener(({ dirtyElements, dirtyLeaves }) => {
      // Only export when content actually changed
      if (dirtyElements.size === 0 && dirtyLeaves.size === 0) {
        return;
      }
      editor.read(() => {
        const markdown = $convertToMarkdownString(
          ALL_TRANSFORMERS,
          undefined,
          true,
        );
        onChangeRef.current(markdown);
      });
    });
  }, [editor]);

  return null;
}

export default function LexicalEditor({
  value,
  onChange,
  inputRef,
  name,
  required,
  ticketSuggestApiBase,
  excludeTicketId,
}: LexicalEditorProps) {
  const [floatingAnchorElem, setFloatingAnchorElem] =
    useState<HTMLDivElement | null>(null);
  const [isLinkEditMode, setIsLinkEditMode] = useState<boolean>(false);

  const onRef = (_floatingAnchorElem: HTMLDivElement) => {
    if (_floatingAnchorElem !== null) {
      setFloatingAnchorElem(_floatingAnchorElem);
    }
  };

  const AppExtension = useMemo(
    () =>
      defineExtension({
        dependencies: [
          RichTextExtension,
          AutoFocusExtension,
          SelectionAlwaysOnDisplayExtension,
          HistoryExtension,
          configExtension(LinkExtension, {
            validateUrl,
            attributes: { rel: "noopener noreferrer", target: "_blank" },
          }),
          AutoLinkExtension,
          ClickableLinkExtension,
          configExtension(MaxLengthExtension, { disabled: false, maxLength }),
          configExtension(MarkdownShortcutsExtension, {
            transformers: ALL_TRANSFORMERS,
          }),
          ClearEditorExtension,
          EmojisExtension,
          DecoratorTextExtension,
          configExtension(ListExtension, { shouldPreserveNumbering: false }),
          CheckListExtension,
          HorizontalRuleExtension,
          ImagesExtension,
          DateTimeExtension,
        ],
        name: "@shadcn-editor",
        namespace: "Playground",
        nodes: [
          OverflowNode,
          EmojiNode,
          MentionNode,
          TicketMentionNode,
          AutocompleteNode,
          SpecialTextNode,
          CodeNode,
          CodeHighlightNode,
          TableNode,
          TableCellNode,
          TableRowNode,
          LayoutContainerNode,
          LayoutItemNode,
          TweetNode,
          YouTubeNode,
        ],
        theme: editorTheme,
      }),
    [],
  );

  return (
    <div className="editor-shell bg-background overflow-hidden rounded-lg border shadow w-full">
      <LexicalExtensionComposer extension={AppExtension} contentEditable={null}>
        <TooltipProvider>
          <div className="relative">
            <ToolbarPlugin>
              {({ blockType }) => (
                <div className="vertical-align-middle sticky top-0 z-10 flex items-center gap-2 overflow-auto border-b p-1">
                  <HistoryToolbarPlugin />
                  <Separator orientation="vertical" className="!h-7" />
                  <BlockFormatDropDown>
                    <FormatParagraph />
                    <FormatHeading levels={["h1", "h2", "h3"]} />
                    <FormatNumberedList />
                    <FormatBulletedList />
                    <FormatCheckList />
                    <FormatCodeBlock />
                    <FormatQuote />
                  </BlockFormatDropDown>
                  {blockType === "code" ? (
                    <CodeLanguageToolbarPlugin />
                  ) : (
                    <>
                      <FontFamilyToolbarPlugin />
                      <Separator orientation="vertical" className="!h-7" />
                      <FontSizeToolbarPlugin />
                      <FontFormatToolbarPlugin />
                      <SubSuperToolbarPlugin />
                      <LinkToolbarPlugin
                        setIsLinkEditMode={setIsLinkEditMode}
                      />
                      <ClearFormattingToolbarPlugin />
                      <FontColorToolbarPlugin />
                      <FontBackgroundToolbarPlugin />
                      <ElementFormatToolbarPlugin />
                      <BlockInsertPlugin>
                        <InsertHorizontalRule />
                        <InsertImage />
                        <InsertTable />
                        <InsertColumnsLayout />
                        <InsertEmbeds />
                      </BlockInsertPlugin>
                    </>
                  )}
                </div>
              )}
            </ToolbarPlugin>
            <div className="relative">
              <div className="">
                <div className="" ref={onRef}>
                  <ContentEditable
                    placeholder={placeholder}
                    className="min-h-[250px] !pl-[52px] outline-none focus:ring-0 w-full"
                    placeholderClassName="!pl-[52px] text-muted-foreground opacity-50 leading-7"
                  />
                </div>
              </div>
              <ComponentPickerMenuPlugin baseOptions={baseOptions} />
              <EmojiPickerPlugin />
              <AutoEmbedPlugin />
              <MentionsPlugin />
              {ticketSuggestApiBase && (
                <TicketMentionsPlugin
                  apiBase={ticketSuggestApiBase}
                  excludeTicketId={excludeTicketId}
                />
              )}
              <AutoCompletePlugin />
              <ContextMenuPlugin />
              <SpecialTextPlugin />
              <TabFocusPlugin />
              <TabIndentationPlugin />
              <CodeHighlightPlugin />
              <TablePlugin />
              <LayoutPlugin />
              <TwitterPlugin />
              <YouTubePlugin />
              <DraggableBlockPlugin
                anchorElem={floatingAnchorElem}
                baseOptions={[]}
              />
              <FloatingTextFormatToolbarPlugin
                anchorElem={floatingAnchorElem}
                setIsLinkEditMode={setIsLinkEditMode}
              />
              <FloatingLinkEditorPlugin
                anchorElem={floatingAnchorElem}
                isLinkEditMode={isLinkEditMode}
                setIsLinkEditMode={setIsLinkEditMode}
              />
              <CodeActionMenuPlugin anchorElem={floatingAnchorElem} />
            </div>
            <ActionsPlugin>
              <div className="clear-both flex items-center justify-between gap-2 overflow-auto border-t p-1">
                <div className="flex flex-1 justify-start text-xs text-muted-foreground">
                  <CharacterLimitPlugin
                    maxLength={maxLength}
                    charset="UTF-16"
                  />
                </div>
                <div>
                  <CounterCharacterPlugin charset="UTF-16" />
                </div>
                <div className="flex flex-1 justify-end">
                  <SpeechToTextPlugin />
                  <ShareContentPlugin />
                  <ImportExportPlugin />
                  <MarkdownTogglePlugin
                    shouldPreserveNewLinesInMarkdown={true}
                    transformers={ALL_TRANSFORMERS}
                  />
                  <EditModeTogglePlugin />
                  <ClearEditorActionPlugin />
                  <TreeViewPlugin />
                </div>
              </div>
            </ActionsPlugin>
          </div>

          <MarkdownExportPlugin onChange={onChange} initialValue={value} />
        </TooltipProvider>
      </LexicalExtensionComposer>
      <textarea
        style={{ display: "none" }}
        name={name}
        value={value}
        readOnly
        required={required}
        ref={inputRef}
      />
    </div>
  );
}
