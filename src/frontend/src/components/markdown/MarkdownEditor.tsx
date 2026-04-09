/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent, RefObject } from "react";
import { useRef } from "react";
import MarkdownContent from "./MarkdownContent";
import {
  Bold,
  Italic,
  Heading,
  List,
  Quote,
  Code,
  Link as LinkIcon,
  Image as ImageIcon,
} from "lucide-react";
import { Button } from "../ui/button";

type MarkdownAction =
  | "bold"
  | "italic"
  | "heading"
  | "list"
  | "quote"
  | "code"
  | "code-block"
  | "link"
  | "media";

const MARKDOWN_CODE_BLOCK_LANGUAGES: Array<[string, string]> = [
  ["", "Text"],
  ["bash", "Bash"],
  ["c", "C"],
  ["cpp", "C++"],
  ["go", "Go"],
  ["html", "HTML"],
  ["java", "Java"],
  ["javascript", "JS"],
  ["json", "JSON"],
  ["python", "Py"],
  ["rust", "Rust"],
  ["sql", "SQL"],
  ["xml", "XML"],
];

function markdownActionText(
  action: MarkdownAction,
  selectedText: string,
  option = "",
): string | null {
  const text = selectedText || "text";
  if (action === "bold") {
    return `**${text}**`;
  }
  if (action === "italic") {
    return `*${text}*`;
  }
  if (action === "heading") {
    return `## ${selectedText || "Heading"}`;
  }
  if (action === "list") {
    return selectedText
      ? selectedText
          .split("\n")
          .map((line) => `- ${line}`)
          .join("\n")
      : "- Item";
  }
  if (action === "quote") {
    return selectedText
      ? selectedText
          .split("\n")
          .map((line) => `> ${line}`)
          .join("\n")
      : "> Quote";
  }
  if (action === "code") {
    return `\`${text}\``;
  }
  if (action === "code-block") {
    return `\`\`\`${option || ""}\n${selectedText || "code"}\n\`\`\``;
  }
  if (action === "link") {
    const href = window.prompt("Link URL", "https://");
    if (!href) {
      return null;
    }
    return `[${selectedText || "link"}](${href})`;
  }
  if (action === "media") {
    const href = window.prompt("Media URL", "https://");
    if (!href) {
      return null;
    }
    return `![${selectedText || "media"}](${href})`;
  }
  return selectedText;
}

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  inputRef?: RefObject<HTMLTextAreaElement | null>;
  rows?: number;
  name?: string;
  required?: boolean;
}

export default function MarkdownEditor({
  value,
  onChange,
  inputRef,
  rows = 10,
  name,
  required = false,
}: MarkdownEditorProps) {
  const fallbackInputRef = useRef<HTMLTextAreaElement | null>(null);
  const textareaRef = inputRef || fallbackInputRef;

  const applyAction = (action: MarkdownAction, option = "") => {
    if (!textareaRef.current) {
      return;
    }
    const textarea = textareaRef.current;
    const selectionStart = textarea.selectionStart ?? value.length;
    const selectionEnd = textarea.selectionEnd ?? value.length;
    const selectedText = value.slice(selectionStart, selectionEnd);
    const replacement = markdownActionText(action, selectedText, option);
    if (replacement == null) {
      return;
    }
    const nextValue = `${value.slice(0, selectionStart)}${replacement}${value.slice(selectionEnd)}`;
    onChange(nextValue);
    requestAnimationFrame(() => {
      textarea.focus();
      const cursor = selectionStart + replacement.length;
      textarea.setSelectionRange(cursor, cursor);
    });
  };

  return (
    <div className="flex flex-col rounded-md border border-input overflow-hidden shadow-sm">
      <div className="flex items-center flex-wrap gap-1 border-b border-input bg-muted/40 p-1">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("bold")}
          title="Bold"
        >
          <Bold className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("italic")}
          title="Italic"
        >
          <Italic className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("heading")}
          title="Heading"
        >
          <Heading className="h-4 w-4" />
        </Button>
        <div className="h-4 w-px bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("list")}
          title="List"
        >
          <List className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("quote")}
          title="Quote"
        >
          <Quote className="h-4 w-4" />
        </Button>
        <div className="h-4 w-px bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("code")}
          title="Inline Code"
        >
          <Code className="h-4 w-4" />
        </Button>
        <select
          className="h-8 rounded-md border border-input bg-background px-2 py-1 text-xs text-muted-foreground outline-none focus:ring-1 focus:ring-ring focus:text-foreground transition-colors"
          defaultValue="__label"
          onChange={(event: ChangeEvent<HTMLSelectElement>) => {
            const selectedLanguage = event.target.value;
            if (selectedLanguage === "__label") {
              return;
            }
            applyAction("code-block", selectedLanguage);
            event.target.value = "__label";
          }}
          aria-label="Code block language"
        >
          <option value="__label" hidden>
            Code Block
          </option>
          {MARKDOWN_CODE_BLOCK_LANGUAGES.map(([code, label]) => (
            <option key={code || "plaintext"} value={code}>
              {label}
            </option>
          ))}
        </select>
        <div className="h-4 w-px bg-border mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("link")}
          title="Link"
        >
          <LinkIcon className="h-4 w-4" />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-muted-foreground hover:text-foreground"
          onClick={() => applyAction("media")}
          title="Image/Media"
        >
          <ImageIcon className="h-4 w-4" />
        </Button>
      </div>

      <div className="grid md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-border bg-background">
        <div className="flex flex-col min-h-[300px]">
          <div className="bg-muted/40 px-3 py-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b border-border">
            Write
          </div>
          <textarea
            ref={textareaRef}
            name={name}
            rows={rows}
            value={value}
            onChange={(event: ChangeEvent<HTMLTextAreaElement>) =>
              onChange(event.target.value)
            }
            required={required}
            className="flex-1 w-full resize-y p-3 outline-none bg-transparent text-sm font-mono focus:ring-0 leading-relaxed"
            placeholder="Write using markdown syntax..."
          />
        </div>
        <div className="flex flex-col min-h-[300px] bg-muted/10">
          <div className="bg-muted/40 px-3 py-1.5 text-xs font-semibold text-muted-foreground uppercase tracking-wider border-b border-border">
            Preview
          </div>
          <div className="flex-1 p-4 prose prose-sm dark:prose-invert max-w-none overflow-y-auto max-h-[600px]">
            {value ? (
              <MarkdownContent>{value}</MarkdownContent>
            ) : (
              <span className="text-muted-foreground opacity-50 italic">
                Nothing to preview.
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
